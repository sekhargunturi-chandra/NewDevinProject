package com.capstone.fraudcase.api;

import com.capstone.fraudcase.application.FraudCaseService;
import com.capstone.fraudcase.application.InvestigationOrchestrator;
import com.capstone.fraudcase.domain.dto.CaseIntakeRequest;
import com.capstone.fraudcase.domain.dto.FollowUpRequest;
import com.capstone.fraudcase.domain.dto.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cases")
public class CaseRestController {

    private final FraudCaseService caseService;
    private final InvestigationOrchestrator orchestrator;

    public CaseRestController(FraudCaseService caseService, InvestigationOrchestrator orchestrator) {
        this.caseService = caseService;
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CaseIntakeRequest request, Authentication auth) {
        var entity = caseService.createCase(request, auth == null ? "anonymous" : auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "caseId", entity.getCaseId(),
                "status", entity.getStatus().name()));
    }

    @GetMapping("/{caseId}")
    public Object get(@PathVariable String caseId) {
        return caseService.getCase(caseId);
    }

    @PostMapping("/{caseId}/investigations")
    public Object investigate(@PathVariable String caseId) {
        return orchestrator.investigate(caseId);
    }

    @GetMapping("/{caseId}/evidence")
    public Object evidence(@PathVariable String caseId) {
        return caseService.evidence(caseId);
    }

    @GetMapping("/{caseId}/tool-traces")
    public Object toolTraces(@PathVariable String caseId) {
        return caseService.toolTraces(caseId);
    }

    @GetMapping("/{caseId}/recommendation")
    public Object recommendation(@PathVariable String caseId) {
        return caseService.latestRecommendation(caseId);
    }

    @GetMapping("/{caseId}/citations")
    public Object citations(@PathVariable String caseId) {
        return caseService.citations(caseId);
    }

    @GetMapping("/{caseId}/audit")
    public Object audit(@PathVariable String caseId) {
        return caseService.auditEvents(caseId);
    }

    @PostMapping("/{caseId}/messages")
    public Object message(@PathVariable String caseId, @Valid @RequestBody FollowUpRequest request,
                          Authentication auth) {
        String answer = orchestrator.followUp(caseId, request.question(),
                auth == null ? "anonymous" : auth.getName());
        return Map.of("caseId", caseId, "answer", answer);
    }

    @PostMapping("/{caseId}/reviews")
    public ResponseEntity<?> review(@PathVariable String caseId, @Valid @RequestBody ReviewRequest request,
                                    Authentication auth) {
        var review = caseService.recordReview(caseId, request,
                auth == null ? "anonymous" : auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "reviewId", review.getReviewId(),
                "disposition", review.getDisposition().name()));
    }
}
