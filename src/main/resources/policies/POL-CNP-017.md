---
documentId: POL-CNP-017
title: Card-Not-Present Dispute Handling Procedure
version: "3.2"
effectiveDate: 2026-10-01
owner: Payment Fraud Operations
classification: SYNTHETIC_TRAINING
---

## 3.1 Scope
This synthetic training procedure covers card-not-present disputes where the
customer reports a merchant they do not recognise. It applies to posted and
pending transactions on personal card accounts only.

## 3.2 Initial fraud routing
When the customer denies recognising the merchant AND at least two independent
anomaly signals are present (newly observed device, location inconsistent with
recent trusted activity, elevated merchant risk, or failed authentication),
route the case to FRAUD_INVESTIGATION with HIGH priority. A human fraud
reviewer must confirm any unauthorised-use classification before further
action. Card controls may never be applied automatically by the assistant.

## 3.3 Authentication evidence
If the authentication challenge outcome is unavailable, record it as missing
information and do not infer a result. The absence of authentication evidence
lowers recommendation confidence and must be shown to the reviewer.

## 3.4 Escalation and manual review
Cases with contradictory signals, or where no policy passage can be retrieved
above the similarity threshold, must be routed to MANUAL_REVIEW. The assistant
must state the gap explicitly rather than inventing a procedure.
