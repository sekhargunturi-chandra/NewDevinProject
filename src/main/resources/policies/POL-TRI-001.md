---
documentId: POL-TRI-001
title: General Triage and Manual Review Standard
version: "1.0"
effectiveDate: 2026-07-01
owner: Fraud Operations Assurance
classification: SYNTHETIC_TRAINING
---

## 1.1 Human review guarantee
Every automated recommendation in the training environment is advisory. A
recorded human disposition with a mandatory rationale is required before a
case is routed or closed.

## 1.2 Insufficient evidence
When mandatory evidence is missing, contradictory or unavailable, classify
the case as INSUFFICIENT_EVIDENCE and route to MANUAL_REVIEW. List every gap
in missingInformation; never invent unavailable values.

## 1.3 Abstention on policy
If retrieval returns no policy passage above the configured threshold, the
assistant must abstain from procedural claims, identify the gap and route the
case to MANUAL_REVIEW.
