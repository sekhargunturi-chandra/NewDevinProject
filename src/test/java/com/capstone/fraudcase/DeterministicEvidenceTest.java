package com.capstone.fraudcase;

import com.capstone.fraudcase.tools.DeterministicEvidence;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicEvidenceTest {

    private final DeterministicEvidence fixtures = new DeterministicEvidence("fraud-training-v1");

    @Test
    void identicalInputProducesIdenticalResults() {
        var intake = TestFixtures.unrecognisedMerchantCase();
        assertThat(fixtures.getDeviceSignals(intake))
                .isEqualTo(fixtures.getDeviceSignals(intake));
        assertThat(fixtures.getLocationComparison(intake))
                .isEqualTo(fixtures.getLocationComparison(intake));
        assertThat(fixtures.getMerchantRiskProfile(intake))
                .isEqualTo(fixtures.getMerchantRiskProfile(intake));
        assertThat(fixtures.getPriorDisputes(intake))
                .isEqualTo(fixtures.getPriorDisputes(intake));
    }

    @Test
    void duplicateFingerprintForDupTransactionRef() {
        var result = fixtures.getTransactionDetails(TestFixtures.duplicateCase());
        assertThat(result.get("duplicateFingerprint")).isEqualTo(true);
    }

    @Test
    void newDeviceYieldsUnavailableAuthOutcome() {
        var result = fixtures.getTransactionDetails(TestFixtures.unrecognisedMerchantCase());
        assertThat(result.get("authOutcome")).isEqualTo("UNAVAILABLE");
    }
}
