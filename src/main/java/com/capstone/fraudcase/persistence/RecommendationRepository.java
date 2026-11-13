package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<RecommendationEntity, String> {

    List<RecommendationEntity> findByCaseIdOrderByCreatedAtDesc(String caseId);

    default Optional<RecommendationEntity> findLatest(String caseId) {
        return findByCaseIdOrderByCreatedAtDesc(caseId).stream().findFirst();
    }
}
