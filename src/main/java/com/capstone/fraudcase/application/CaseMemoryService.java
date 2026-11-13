package com.capstone.fraudcase.application;

import com.capstone.fraudcase.persistence.CaseMessageEntity;
import com.capstone.fraudcase.persistence.CaseMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Case-scoped conversation memory persisted to MySQL. Messages are keyed by
 * caseId only; the retained window is bounded so context can never leak
 * between cases.
 */
@Service
public class CaseMemoryService {

    private final CaseMessageRepository repository;
    private final int maxMessages;

    public CaseMemoryService(CaseMessageRepository repository,
                             @Value("${capstone.memory.max-messages:12}") int maxMessages) {
        this.repository = repository;
        this.maxMessages = maxMessages;
    }

    public void append(String caseId, String role, String content) {
        CaseMessageEntity entity = new CaseMessageEntity();
        entity.setCaseId(caseId);
        entity.setRole(role);
        entity.setContent(content == null ? "" : content.substring(0, Math.min(content.length(), 4000)));
        repository.save(entity);
    }

    public List<CaseMessageEntity> history(String caseId) {
        List<CaseMessageEntity> all = repository.findByCaseIdOrderByCreatedAtAsc(caseId);
        return all.size() <= maxMessages ? all : all.subList(all.size() - maxMessages, all.size());
    }

    public String renderContext(String caseId) {
        return history(caseId).stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }
}
