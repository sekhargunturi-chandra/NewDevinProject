package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseMessageRepository extends JpaRepository<CaseMessageEntity, Long> {

    List<CaseMessageEntity> findByCaseIdOrderByCreatedAtAsc(String caseId);
}
