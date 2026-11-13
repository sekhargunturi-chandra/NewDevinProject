---
documentId: POL-MCH-022
title: Merchant Dispute Triage
version: "2.1"
effectiveDate: 2026-08-01
owner: Dispute Operations
classification: SYNTHETIC_TRAINING
---

## 2.1 Recognised merchant complaints
When the customer recognises the merchant and the complaint concerns
fulfilment, cancellation or the charged amount, classify the case as a
merchant dispute rather than unauthorised use.

## 2.2 Routing
Merchant disputes route to DISPUTE_OPERATIONS with a 24-hour training SLA
target. Fulfilment disputes require the merchant category and risk band from
the merchant risk tool to be present in the evidence set.

## 2.3 Contradictions
If the customer statement both recognises and denies the merchant, treat the
signals as contradictory, reduce confidence and prefer manual review.
