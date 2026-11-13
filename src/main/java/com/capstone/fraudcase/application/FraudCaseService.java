package com.capstone.fraudcase.application;

import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.domain.CaseStatus;
import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;
import com.capstone.fraudcase.domain.dto.ReviewRequest;
import com.capstone.fraudcase.persistence.AuditWriter;
import com.capstone.fraudcase.persistence.FraudCaseRepository;
import com.capstone.fraudcase.persistence.EvidenceItemRepository;
import com.capstone.fraudcase.persistence.ToolTraceRepository;
import com.capstone.fraudcase.persistence.RecommendationRepository;
import com.capstone.fraudcase.persistence.PolicyCitationRepository;
import com.capstone.fraudcase.persistence.ReviewDispositionRepository;
import com.capstone.fraudcase.persistence.AuditEventRepository;
import com.capstone.fraudcase.persistence.FraudCaseEntity;
import com.capstone.fraudcase.persistence.ReviewDispositionEntity;
import com.capstone.fraudcase.util.Hashes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class FraudCaseService {

    private static final Set<Disposition> ROUTE_DISPOSITIONS = EnumSet.of(Disposition.ACCEPT, Disposition.REROUTE);
    private static final Set<Disposition> CLOSE_DISPOSITIONS = EnumSet.of(Disposition.CLOSE, Disposition.OVERRIDE);

    private final FraudCaseRepository caseRepository;
    private final EvidenceItemRepository evidenceRepository;
    private final ToolTraceRepository traceRepository;
    private final RecommendationRepository recommendationRepository;
    private final PolicyCitationRepository citationRepository;
    private final ReviewDispositionRepository reviewRepository;
    private final AuditEventRepository auditRepository;
    private final AuditWriter auditWriter;
    private final CaseMemoryService memoryService;

    public FraudCaseService(FraudCaseRepository caseRepository,
                            EvidenceItemRepository evidenceRepository,
                            ToolTraceRepository traceRepository,
                            RecommendationRepository recommendationRepository,
                            PolicyCitationRepository citationRepository,
                            ReviewDispositionRepository reviewRepository,
                            AuditEventRepository auditRepository,
                            AuditWriter auditWriter,
                            CaseMemoryService memoryService) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
        this.traceRepository = traceRepository;
        this.recommendationRepository = recommendationRepository;
        this.citationRepository = citationRepository;
        this.reviewRepository = reviewRepository;
        this.auditRepository = auditRepository;
        this.auditWriter = auditWriter;
        this.memoryService = memoryService;
    }

    @Transactional
    public FraudCaseEntity createCase(CaseIntakeRequest intake, String username) {
        FraudCaseEntity entity = new FraudCaseEntity();
        entity.setCaseId(Hashes.nextId("CASE"));
        entity.setMaskedCustomerRef(intake.maskedCustomerRef());
        entity.setHomeRegion(intake.homeRegion());
        entity.setReportedAt(Instant.now());
        entity.setAnalystId(username);
        entity.setCreatedBy(username);
        entity.setTransactionRef(intake.transactionRef());
        entity.setAmount(intake.amount());
        entity.setCurrency(intake.currency());
        entity.setOccurredAt(intake.occurredAt());
        entity.setTransactionStatus(intake.status());
        entity.setTransactionChannel(intake.channel());
        entity.setMaskedMerchant(intake.maskedMerchant());
        entity.setIssueType(intake.issueType());
        entity.setCustomerStatement(intake.customerStatement());
        entity.setCardPossession(intake.cardPossession());
        entity.setMerchantRecognised(intake.merchantRecognised());
        entity.setStatus(CaseStatus.NEW);
        FraudCaseEntity saved = caseRepository.save(entity);
        auditWriter.record(saved.getCaseId(), "CASE_CREATED", ActorType.USER, username,
                "case intake " + saved.getCaseId());
        return saved;
    }

    public FraudCaseEntity getCase(String caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "case not found"));
    }

    public List<FraudCaseEntity> listCases() {
        return caseRepository.findAll();
    }

    public List<?> evidence(String caseId) {
        return evidenceRepository.findByCaseIdOrderByObservedAtAsc(caseId);
    }

    public List<?> toolTraces(String caseId) {
        return traceRepository.findByCaseIdOrderByCreatedAtAsc(caseId);
    }

    public Object latestRecommendation(String caseId) {
        return recommendationRepository.findLatest(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no recommendation yet"));
    }

    public List<?> citations(String caseId) {
        return citationRepository.findByCaseIdOrderByCreatedAtAsc(caseId);
    }

    public List<?> auditEvents(String caseId) {
        return auditRepository.findByCaseIdOrderBySeqAsc(caseId);
    }

    public List<?> messages(String caseId) {
        return memoryService.history(caseId);
    }

    @Transactional
    public ReviewDispositionEntity recordReview(String caseId, ReviewRequest request, String reviewerId) {
        FraudCaseEntity entity = getCase(caseId);
        if (entity.getStatus() != CaseStatus.READY_FOR_REVIEW && entity.getStatus() != CaseStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "case " + entity.getStatus() + " is not ready for review");
        }
        if (request.disposition() == Disposition.REROUTE && request.rerouteTo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rerouteTo is required for REROUTE");
        }

        transition(entity, CaseStatus.UNDER_REVIEW);

        ReviewDispositionEntity review = new ReviewDispositionEntity();
        review.setReviewId(Hashes.nextId("REV"));
        review.setCaseId(caseId);
        review.setReviewerId(reviewerId);
        review.setDisposition(request.disposition());
        review.setRerouteTo(request.rerouteTo());
        review.setReason(request.reason());
        ReviewDispositionEntity saved = reviewRepository.save(review);

        if (ROUTE_DISPOSITIONS.contains(request.disposition())) {
            transition(entity, CaseStatus.ROUTED);
        } else if (CLOSE_DISPOSITIONS.contains(request.disposition())) {
            transition(entity, CaseStatus.CLOSED);
        }
        // REQUEST_MORE_INFO stays UNDER_REVIEW until a final disposition is recorded.

        auditWriter.record(caseId, "REVIEW_" + request.disposition(), ActorType.REVIEWER, reviewerId,
                "disposition " + request.disposition() + " reason-hash " + Hashes.shortHash(request.reason()));
        return saved;
    }

    @Transactional
    public void transition(FraudCaseEntity entity, CaseStatus next) {
        if (entity.getStatus() == next) {
            return;
        }
        if (!entity.getStatus().canTransitionTo(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "illegal transition " + entity.getStatus() + " -> " + next);
        }
        entity.setStatus(next);
        caseRepository.save(entity);
        auditWriter.record(entity.getCaseId(), "STATUS_" + next, ActorType.SYSTEM, "system",
                "status -> " + next);
    }

    public EvidenceItemRepository evidenceRepository() {
        return evidenceRepository;
    }

    public RecommendationRepository recommendationRepository() {
        return recommendationRepository;
    }

    public PolicyCitationRepository citationRepository() {
        return citationRepository;
    }
}
