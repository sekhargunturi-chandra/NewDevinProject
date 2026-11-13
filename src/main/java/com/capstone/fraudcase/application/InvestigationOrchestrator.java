package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.FraudCaseProcessor;
import com.capstone.fraudcase.config.CapstoneProperties;
import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.CaseStatus;
import com.capstone.fraudcase.domain.Classification;
import com.capstone.fraudcase.domain.EvidenceType;
import com.capstone.fraudcase.domain.Severity;
import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;
import com.capstone.fraudcase.domain.dto.FraudCaseContext;
import com.capstone.fraudcase.domain.dto.FraudCaseRecommendation;
import com.capstone.fraudcase.domain.dto.PolicyPassage;
import com.capstone.fraudcase.domain.dto.ToolEvidence;
import com.capstone.fraudcase.knowledge.PolicyRetriever;
import com.capstone.fraudcase.persistence.AuditWriter;
import com.capstone.fraudcase.persistence.EvidenceItemEntity;
import com.capstone.fraudcase.persistence.EvidenceItemRepository;
import com.capstone.fraudcase.persistence.RecommendationRepository;
import com.capstone.fraudcase.persistence.PolicyCitationRepository;
import com.capstone.fraudcase.persistence.FraudCaseEntity;
import com.capstone.fraudcase.persistence.PolicyCitationEntity;
import com.capstone.fraudcase.persistence.RecommendationEntity;
import com.capstone.fraudcase.tools.DeterministicEvidence;
import com.capstone.fraudcase.tools.ToolExecutor;
import com.capstone.fraudcase.util.Hashes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Bounded plan/act/retrieve/assess/recommend pipeline. Evidence collection is
 * deterministic and separated from model-assisted interpretation; state
 * transitions live here, never in model output.
 */
@Service
public class InvestigationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(InvestigationOrchestrator.class);
    private static final String INVESTIGATION_GOAL =
            "Classify the dispute and recommend a review route using only the supplied evidence and policy passages.";

    private final FraudCaseService caseService;
    private final ProcessorSelector selector;
    private final DeterministicEvidence fixtures;
    private final ToolExecutor toolExecutor;
    private final CaseMemoryService memoryService;
    private final EvidenceItemRepository evidenceRepository;
    private final RecommendationRepository recommendationRepository;
    private final PolicyCitationRepository citationRepository;
    private final AuditWriter auditWriter;
    private final ObjectMapper objectMapper;
    private final CapstoneProperties properties;

    public InvestigationOrchestrator(FraudCaseService caseService,
                                     ProcessorSelector selector,
                                     DeterministicEvidence fixtures,
                                     ToolExecutor toolExecutor,
                                     CaseMemoryService memoryService,
                                     EvidenceItemRepository evidenceRepository,
                                     RecommendationRepository recommendationRepository,
                                     PolicyCitationRepository citationRepository,
                                     AuditWriter auditWriter,
                                     ObjectMapper objectMapper,
                                     CapstoneProperties properties) {
        this.caseService = caseService;
        this.selector = selector;
        this.fixtures = fixtures;
        this.toolExecutor = toolExecutor;
        this.memoryService = memoryService;
        this.evidenceRepository = evidenceRepository;
        this.recommendationRepository = recommendationRepository;
        this.citationRepository = citationRepository;
        this.auditWriter = auditWriter;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Transactional
    public FraudCaseRecommendation investigate(String caseId) {
        FraudCaseEntity entity = caseService.getCase(caseId);
        if (entity.getStatus() != CaseStatus.NEW && entity.getStatus() != CaseStatus.INCOMPLETE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "case " + entity.getStatus() + " cannot be (re)investigated");
        }
        caseService.transition(entity, CaseStatus.ENRICHING);
        auditWriter.record(caseId, "INVESTIGATION_STARTED", ActorType.SYSTEM, "orchestrator",
                "mode=" + selector.activeMode());

        CaseIntakeRequest intake = toIntake(entity);
        List<ToolEvidence> evidence = runToolPlan(caseId, intake);
        persistEvidence(caseId, intake, evidence);

        FraudCaseContext context = buildContext(caseId, intake, evidence);
        FraudCaseRecommendation recommendation = analyzeWithFallback(context);

        ToolEvidence sla = toolExecutor.execute(caseId, "calculateCaseSla",
                Map.of("route", recommendation.getRecommendedRoute()),
                () -> fixtures.calculateCaseSla(recommendation.getRecommendedRoute(),
                        intake.occurredAt().toInstant()));
        evidence.add(sla);
        persistEvidenceItem(caseId, sla);

        toolExecutor.execute(caseId, "saveAuditEvent",
                Map.of("eventType", "INVESTIGATION_COMPLETED"),
                () -> {
                    var e = auditWriter.record(caseId, "INVESTIGATION_COMPLETED", ActorType.SYSTEM,
                            "orchestrator", "mode=" + selector.activeMode());
                    return Map.of("eventId", e.getEventId(), "eventType", "INVESTIGATION_COMPLETED",
                            "recordedAt", e.getCreatedAt().toString());
                });

        persistRecommendation(caseId, context, recommendation);

        CaseStatus next = Classification.INSUFFICIENT_EVIDENCE.name().equals(recommendation.getClassification())
                ? CaseStatus.INCOMPLETE : CaseStatus.READY_FOR_REVIEW;
        caseService.transition(entity, next);
        return recommendation;
    }

    public String followUp(String caseId, String question, String username) {
        FraudCaseEntity entity = caseService.getCase(caseId);
        memoryService.append(caseId, "USER", question);
        auditWriter.record(caseId, "FOLLOW_UP_QUESTION", ActorType.USER, username, "question-hash " + Hashes.shortHash(question));

        FraudCaseContext ctx = buildContext(caseId, toIntake(entity), currentEvidence(caseId));
        String answer;
        try {
            answer = selector.processor().ask(ctx, question);
        } catch (Exception e) {
            answer = selector.rulesProcessor().ask(ctx, question);
        }
        memoryService.append(caseId, "ASSISTANT", answer);
        return answer;
    }

    private List<ToolEvidence> runToolPlan(String caseId, CaseIntakeRequest intake) {
        List<ToolEvidence> evidence = new ArrayList<>();
        run(evidence, caseId, "getTransactionDetails",
                Map.of("transactionRef", intake.transactionRef()),
                () -> fixtures.getTransactionDetails(intake));
        run(evidence, caseId, "getDeviceSignals",
                Map.of("maskedCustomerRef", intake.maskedCustomerRef()),
                () -> fixtures.getDeviceSignals(intake));
        run(evidence, caseId, "compareRecentLocations",
                Map.of("maskedCustomerRef", intake.maskedCustomerRef()),
                () -> fixtures.getLocationComparison(intake));
        run(evidence, caseId, "getMerchantRiskProfile",
                Map.of("maskedMerchant", intake.maskedMerchant()),
                () -> fixtures.getMerchantRiskProfile(intake));
        run(evidence, caseId, "getPriorDisputes",
                Map.of("maskedCustomerRef", intake.maskedCustomerRef()),
                () -> fixtures.getPriorDisputes(intake));
        run(evidence, caseId, "retrievePolicyEvidence",
                Map.of("query", policyQuery(intake), "topK", properties.retrieval().topK()),
                () -> policyResults(intake));
        return evidence;
    }

    private void run(List<ToolEvidence> sink, String caseId, String tool,
                     Map<String, Object> args, Supplier<Map<String, Object>> call) {
        sink.add(toolExecutor.execute(caseId, tool, args, call));
    }

    private Map<String, Object> policyResults(CaseIntakeRequest intake) {
        PolicyRetriever retriever = selector.retriever();
        List<PolicyPassage> passages = retriever.retrieve(policyQuery(intake),
                properties.retrieval().topK(), properties.retrieval().similarityThreshold());
        return Map.of(
                "found", !passages.isEmpty(),
                "retrieverMode", retriever.mode(),
                "passageCount", passages.size(),
                "passages", passages);
    }

    private String policyQuery(CaseIntakeRequest intake) {
        return intake.issueType() + " " + intake.customerStatement() + " "
                + intake.maskedMerchant() + " " + intake.channel();
    }

    private FraudCaseContext buildContext(String caseId, CaseIntakeRequest intake,
                                          List<ToolEvidence> evidence) {
        return new FraudCaseContext(caseId, intake, null, null, INVESTIGATION_GOAL,
                evidence, currentPassages(evidence), memoryService.renderContext(caseId));
    }

    private List<PolicyPassage> currentPassages(List<ToolEvidence> evidence) {
        return evidence.stream()
                .filter(e -> "retrievePolicyEvidence".equals(e.toolName()) && e.ok())
                .findFirst()
                .map(e -> toPassages(e.result().get("passages")))
                .orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    private List<PolicyPassage> toPassages(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<PolicyPassage> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof PolicyPassage p) {
                out.add(p);
            } else if (o instanceof Map<?, ?> m) {
                out.add(new PolicyPassage(
                        str(m, "documentId"), str(m, "title"), str(m, "version"),
                        str(m, "effectiveDate"), str(m, "section"), str(m, "chunkId"),
                        m.get("score") instanceof Number n ? n.doubleValue() : 0.0,
                        str(m, "passage")));
            }
        }
        return out;
    }

    private String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    @SuppressWarnings("unchecked")
    private List<ToolEvidence> currentEvidence(String caseId) {
        // Rebuild tool evidence from the persisted (redacted) evidence items so
        // follow-up questions see the same facts the investigation produced.
        List<ToolEvidence> out = new ArrayList<>();
        for (EvidenceItemEntity item : evidenceRepository.findByCaseIdOrderByObservedAtAsc(caseId)) {
            if (item.getProvenance() == null || !item.getProvenance().startsWith("TOOL:")) {
                continue;
            }
            Map<String, Object> result;
            try {
                result = objectMapper.readValue(item.getValueSummary(), Map.class);
            } catch (Exception e) {
                result = Map.of("summary", item.getValueSummary());
            }
            out.add(new ToolEvidence(item.getEvidenceId(), item.getToolTraceId(),
                    item.getProvenance().substring(5), "OK", 0, result, item.getObservedAt()));
        }
        return out;
    }

    private FraudCaseRecommendation analyzeWithFallback(FraudCaseContext ctx) {
        FraudCaseProcessor processor = selector.processor();
        try {
            return processor.analyze(ctx);
        } catch (Exception e) {
            log.warn("processor {} failed for {}: {} - falling back to rules",
                    processor.mode(), ctx.caseId(), e.getMessage());
            auditWriter.record(ctx.caseId(), "PROCESSOR_FAILED_SAFE", ActorType.MODEL,
                    processor.mode(), "fallback to rules: " + e.getClass().getSimpleName());
            try {
                return selector.rulesProcessor().analyze(ctx);
            } catch (Exception ex) {
                auditWriter.record(ctx.caseId(), "ANALYSIS_FAILED", ActorType.SYSTEM,
                        "orchestrator", "all processors failed");
                FraudCaseRecommendation safe = new FraudCaseRecommendation();
                safe.setCaseId(ctx.caseId());
                safe.setClassification(Classification.INSUFFICIENT_EVIDENCE.name());
                safe.setRiskLevel("MEDIUM");
                safe.setRecommendedRoute("MANUAL_REVIEW");
                safe.setConfidence(0.0);
                safe.setHumanReviewRequired(true);
                safe.setRationale("Analysis failed safely; manual review required.");
                safe.setMissingInformation(List.of("analysis unavailable"));
                return safe;
            }
        }
    }

    private void persistEvidence(String caseId, CaseIntakeRequest intake, List<ToolEvidence> evidence) {
        EvidenceItemEntity claim = new EvidenceItemEntity();
        claim.setEvidenceId(Hashes.nextId("EV"));
        claim.setCaseId(caseId);
        claim.setType(EvidenceType.CUSTOMER_CLAIM);
        claim.setSeverity(Severity.INFO);
        claim.setValueSummary("issueType=" + intake.issueType()
                + " merchantRecognised=" + intake.merchantRecognised()
                + " cardPossession=" + intake.cardPossession());
        claim.setProvenance("INTAKE");
        evidenceRepository.save(claim);
        evidence.forEach(ev -> persistEvidenceItem(caseId, ev));
    }

    private void persistEvidenceItem(String caseId, ToolEvidence ev) {
        EvidenceItemEntity item = new EvidenceItemEntity();
        item.setEvidenceId(ev.evidenceId());
        item.setCaseId(caseId);
        item.setType(evidenceType(ev.toolName()));
        item.setSeverity(severity(ev));
        item.setValueSummary(summarise(ev.result()));
        item.setProvenance("TOOL:" + ev.toolName());
        item.setToolTraceId(ev.traceId());
        evidenceRepository.save(item);
    }

    private EvidenceType evidenceType(String tool) {
        return switch (tool) {
            case "getTransactionDetails" -> EvidenceType.TRANSACTION;
            case "getDeviceSignals" -> EvidenceType.DEVICE;
            case "compareRecentLocations" -> EvidenceType.LOCATION;
            case "getMerchantRiskProfile" -> EvidenceType.MERCHANT;
            case "getPriorDisputes" -> EvidenceType.HISTORY;
            case "calculateCaseSla" -> EvidenceType.SLA;
            default -> EvidenceType.SYSTEM;
        };
    }

    private Severity severity(ToolEvidence ev) {
        if (!ev.ok()) {
            return Severity.MEDIUM;
        }
        Object v = ev.result().get("anomalyLevel");
        if ("HIGH".equals(v)) {
            return Severity.HIGH;
        }
        if (Boolean.TRUE.equals(ev.result().get("duplicateFingerprint"))
                || Boolean.TRUE.equals(ev.result().get("blocklistMatch"))
                || Boolean.FALSE.equals(ev.result().get("deviceKnown"))
                || "HIGH".equals(ev.result().get("riskBand"))) {
            return Severity.HIGH;
        }
        if ("MEDIUM".equals(v) || "MEDIUM".equals(ev.result().get("riskBand"))
                || "UNAVAILABLE".equals(ev.result().get("authOutcome"))) {
            return Severity.MEDIUM;
        }
        Number count = (Number) ev.result().get("count");
        if (count != null && count.longValue() > 0) {
            return Severity.MEDIUM;
        }
        return Severity.INFO;
    }

    private String summarise(Map<String, Object> result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            return json.length() > 1500 ? json.substring(0, 1500) : json;
        } catch (Exception e) {
            return String.valueOf(result);
        }
    }

    private void persistRecommendation(String caseId, FraudCaseContext ctx,
                                       FraudCaseRecommendation rec) {
        try {
            RecommendationEntity entity = new RecommendationEntity();
            entity.setRecommendationId(Hashes.nextId("REC"));
            entity.setCaseId(caseId);
            entity.setMode(selector.activeMode());
            entity.setClassification(rec.getClassification());
            entity.setRecommendedRoute(rec.getRecommendedRoute());
            entity.setRiskLevel(rec.getRiskLevel());
            entity.setConfidence(rec.getConfidence());
            entity.setHumanReviewRequired(rec.isHumanReviewRequired());
            entity.setRationale(rec.getRationale());
            entity.setEvidenceIdsJson(objectMapper.writeValueAsString(rec.getEvidenceIds()));
            entity.setMissingInformationJson(objectMapper.writeValueAsString(rec.getMissingInformation()));
            entity.setRuleIdsJson(objectMapper.writeValueAsString(rec.getRuleIds()));
            entity.setToolTraceIdsJson(objectMapper.writeValueAsString(rec.getToolTraceIds()));
            recommendationRepository.save(entity);

            for (var citation : rec.getPolicyCitations()) {
                PolicyCitationEntity c = new PolicyCitationEntity();
                c.setCitationId(Hashes.nextId("CIT"));
                c.setCaseId(caseId);
                c.setRecommendationId(entity.getRecommendationId());
                c.setDocumentId(citation.documentId());
                c.setVersion(citation.version());
                c.setSection(citation.section());
                c.setChunkId(citation.chunkId());
                c.setScore(citation.score());
                ctx.policyContext().stream()
                        .filter(p -> p.chunkId().equals(citation.chunkId()))
                        .findFirst()
                        .ifPresent(p -> c.setPassage(p.passage()));
                citationRepository.save(c);
            }
            auditWriter.record(caseId, "RECOMMENDATION_PERSISTED", ActorType.MODEL,
                    selector.activeMode(), entity.getRecommendationId() + " " + rec.getClassification());
        } catch (Exception e) {
            throw new IllegalStateException("failed to persist recommendation", e);
        }
    }

    private CaseIntakeRequest toIntake(FraudCaseEntity e) {
        return new CaseIntakeRequest(
                e.getMaskedCustomerRef(), e.getHomeRegion(), e.getTransactionRef(),
                e.getAmount(), e.getCurrency(), e.getOccurredAt(),
                e.getTransactionStatus(), e.getTransactionChannel(), e.getMaskedMerchant(),
                e.getIssueType(), e.getCustomerStatement(),
                e.getCardPossession(), e.getMerchantRecognised(), true, true);
    }
}
