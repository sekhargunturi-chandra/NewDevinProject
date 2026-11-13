package com.capstone.fraudcase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudCaseRepository extends JpaRepository<FraudCaseEntity, String> {
}
