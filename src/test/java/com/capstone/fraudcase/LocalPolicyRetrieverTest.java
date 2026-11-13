package com.capstone.fraudcase;

import com.capstone.fraudcase.domain.dto.PolicyPassage;
import com.capstone.fraudcase.knowledge.HashingEmbedder;
import com.capstone.fraudcase.knowledge.LocalPolicyRetriever;
import com.capstone.fraudcase.knowledge.PolicyChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPolicyRetrieverTest {

    private LocalPolicyRetriever retriever() {
        LocalPolicyRetriever r = new LocalPolicyRetriever(new HashingEmbedder());
        r.index(List.of(
                new PolicyChunk("POL-CNP-017", "CNP", "3.2", "2026-10-01", "ops",
                        "3.2 Initial fraud routing", "pol-cnp-017-c1",
                        "route card not present unrecognised merchant disputes to fraud investigation"),
                new PolicyChunk("POL-DUP-009", "DUP", "1.4", "2026-09-15", "ops",
                        "1.2 Routing", "pol-dup-009-c1",
                        "duplicate fingerprint evidence routes to payment operations")));
        return r;
    }

    @Test
    void relevantQueryReturnsCitationMetadata() {
        List<PolicyPassage> hits = retriever().retrieve(
                "unrecognised merchant card not present fraud routing", 4, 0.2);
        assertThat(hits).isNotEmpty();
        PolicyPassage top = hits.get(0);
        assertThat(top.documentId()).isEqualTo("POL-CNP-017");
        assertThat(top.chunkId()).isNotBlank();
        assertThat(top.score()).isGreaterThan(0.2);
    }

    @Test
    void irrelevantQueryAbstains() {
        List<PolicyPassage> hits = retriever().retrieve(
                "zebra quantum telescope", 4, 0.6);
        assertThat(hits).isEmpty();
    }
}
