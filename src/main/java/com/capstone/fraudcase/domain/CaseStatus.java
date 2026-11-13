package com.capstone.fraudcase.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum CaseStatus {
    NEW, ENRICHING, INCOMPLETE, READY_FOR_REVIEW, UNDER_REVIEW, ROUTED, CLOSED;

    private static final Map<CaseStatus, Set<CaseStatus>> TRANSITIONS = Map.of(
            NEW, EnumSet.of(ENRICHING),
            ENRICHING, EnumSet.of(READY_FOR_REVIEW, INCOMPLETE),
            INCOMPLETE, EnumSet.of(ENRICHING, CLOSED),
            READY_FOR_REVIEW, EnumSet.of(UNDER_REVIEW),
            UNDER_REVIEW, EnumSet.of(ROUTED, CLOSED),
            ROUTED, EnumSet.of(CLOSED),
            CLOSED, EnumSet.noneOf(CaseStatus.class));

    public boolean canTransitionTo(CaseStatus next) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}
