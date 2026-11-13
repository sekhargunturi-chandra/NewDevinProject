package com.capstone.fraudcase.persistence;

import com.capstone.fraudcase.domain.ActorType;
import com.capstone.fraudcase.util.Hashes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only audit writer. Events record a payload hash rather than the raw
 * payload so no sensitive case data lands in the audit table.
 */
@Service
public class AuditWriter {

    private final AuditEventRepository repository;

    public AuditWriter(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEventEntity record(String caseId, String eventType, ActorType actorType,
                                   String actorId, String payloadSummary) {
        AuditEventEntity event = new AuditEventEntity();
        event.setEventId(Hashes.nextId("EVT"));
        event.setCaseId(caseId);
        event.setEventType(eventType);
        event.setActorType(actorType);
        event.setActorId(actorId);
        event.setPayloadHash(Hashes.sha256(payloadSummary == null ? "" : payloadSummary));
        return repository.save(event);
    }
}
