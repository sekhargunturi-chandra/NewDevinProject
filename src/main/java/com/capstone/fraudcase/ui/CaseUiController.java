package com.capstone.fraudcase.ui;

import com.capstone.fraudcase.application.FraudCaseService;
import com.capstone.fraudcase.application.InvestigationOrchestrator;
import com.capstone.fraudcase.application.ProcessorSelector;
import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.IssueType;
import com.capstone.fraudcase.domain.RecommendedRoute;
import com.capstone.fraudcase.domain.TransactionStatus;
import com.capstone.fraudcase.domain.TxnChannel;
import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;
import com.capstone.fraudcase.domain.dto.ReviewRequest;
import com.capstone.fraudcase.persistence.RecommendationEntity;
import com.capstone.fraudcase.persistence.RecommendationRepository;
import com.capstone.fraudcase.persistence.ReviewDispositionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ui/cases")
public class CaseUiController {

    private final FraudCaseService caseService;
    private final InvestigationOrchestrator orchestrator;
    private final ProcessorSelector selector;
    private final RecommendationRepository recommendationRepository;
    private final ReviewDispositionRepository reviewRepository;
    private final ObjectMapper objectMapper;

    public CaseUiController(FraudCaseService caseService,
                            InvestigationOrchestrator orchestrator,
                            ProcessorSelector selector,
                            RecommendationRepository recommendationRepository,
                            ReviewDispositionRepository reviewRepository,
                            ObjectMapper objectMapper) {
        this.caseService = caseService;
        this.orchestrator = orchestrator;
        this.selector = selector;
        this.recommendationRepository = recommendationRepository;
        this.reviewRepository = reviewRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("cases", caseService.listCases());
        model.addAttribute("mode", selector.activeMode());
        return "cases";
    }

    @GetMapping("/new")
    public String intakeForm(Model model) {
        model.addAttribute("caseIntakeRequest", new CaseIntakeRequest(
                null, null, null, null, null, null,
                TransactionStatus.POSTED, TxnChannel.CARD_NOT_PRESENT,
                null, null, null, true, false, true, true));
        model.addAttribute("issueTypes", IssueType.values());
        model.addAttribute("txnStatuses", TransactionStatus.values());
        model.addAttribute("channels", TxnChannel.values());
        return "intake";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CaseIntakeRequest request,
                         BindingResult binding, Authentication auth, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("issueTypes", IssueType.values());
            model.addAttribute("txnStatuses", TransactionStatus.values());
            model.addAttribute("channels", TxnChannel.values());
            return "intake";
        }
        var entity = caseService.createCase(request, auth.getName());
        return "redirect:/ui/cases/" + entity.getCaseId();
    }

    @GetMapping("/{caseId}")
    public String workspace(@PathVariable String caseId, Model model) {
        model.addAttribute("fraudCase", caseService.getCase(caseId));
        model.addAttribute("evidence", caseService.evidence(caseId));
        model.addAttribute("traces", caseService.toolTraces(caseId));
        model.addAttribute("citations", caseService.citations(caseId));
        model.addAttribute("audits", caseService.auditEvents(caseId));
        model.addAttribute("messages", caseService.messages(caseId));
        model.addAttribute("reviews", reviewRepository.findByCaseIdOrderByRecordedAtAsc(caseId));
        model.addAttribute("dispositions", Disposition.values());
        model.addAttribute("routes", RecommendedRoute.values());
        model.addAttribute("mode", selector.activeMode());
        RecommendationEntity rec = recommendationRepository.findLatest(caseId).orElse(null);
        model.addAttribute("recommendation", rec);
        model.addAttribute("recommendationDetail", rec == null ? null : detail(rec));
        return "case";
    }

    @PostMapping("/{caseId}/investigate")
    public String investigate(@PathVariable String caseId, RedirectAttributes ra) {
        try {
            orchestrator.investigate(caseId);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Investigation failed safely: " + e.getMessage());
        }
        return "redirect:/ui/cases/" + caseId;
    }

    @PostMapping("/{caseId}/messages")
    public String message(@PathVariable String caseId, @RequestParam String question,
                          Authentication auth, RedirectAttributes ra) {
        if (question == null || question.isBlank()) {
            ra.addFlashAttribute("error", "Question is required.");
            return "redirect:/ui/cases/" + caseId;
        }
        orchestrator.followUp(caseId, question, auth.getName());
        return "redirect:/ui/cases/" + caseId;
    }

    @PostMapping("/{caseId}/review")
    public String review(@PathVariable String caseId,
                         @RequestParam Disposition disposition,
                         @RequestParam(required = false) String reason,
                         @RequestParam(required = false) RecommendedRoute rerouteTo,
                         Authentication auth, RedirectAttributes ra) {
        if (reason == null || reason.isBlank()) {
            ra.addFlashAttribute("error", "A reviewer rationale is mandatory.");
            return "redirect:/ui/cases/" + caseId;
        }
        try {
            caseService.recordReview(caseId, new ReviewRequest(disposition, reason, rerouteTo), auth.getName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ui/cases/" + caseId;
    }

    private Map<String, Object> detail(RecommendationEntity rec) {
        return Map.of(
                "evidenceIds", parseList(rec.getEvidenceIdsJson()),
                "missingInformation", parseList(rec.getMissingInformationJson()),
                "ruleIds", parseList(rec.getRuleIdsJson()),
                "toolTraceIds", parseList(rec.getToolTraceIdsJson()));
    }

    private List<String> parseList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
