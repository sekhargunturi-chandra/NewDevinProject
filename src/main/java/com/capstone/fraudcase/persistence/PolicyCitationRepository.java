package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyCitationRepository extends JpaRepository<PolicyCitationEntity, String> {

    List<PolicyCitationEntity> findByCaseIdOrderByCreatedAtAsc(String caseId);
}
