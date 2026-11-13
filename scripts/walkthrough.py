#!/usr/bin/env python3
"""
End-to-end demonstration of the Fraud Case Assistant.

Drives the full capstone journey against a running instance:
  intake -> validate -> investigate (tools + policy) -> recommendation
  -> citations -> case-scoped follow-up -> human review -> audit trail
  -> safety checks (missing reason, wrong role, illegal state transition)

Usage:  python3 scripts/walkthrough.py [base-url]     # default http://localhost:8081
Requires: the application running (any processor mode) and python3 stdlib only.
"""

import json
import sys
import urllib.request
import urllib.error
import base64

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081"

USERS = {
    "agent": ("agent", "agent123"),
    "investigator": ("investigator", "investigator123"),
    "reviewer": ("reviewer", "reviewer123"),
}

STEP = [0]


def banner(title):
    STEP[0] += 1
    print("\n" + "=" * 78)
    print(f"  STEP {STEP[0]:>2} | {title}")
    print("=" * 78)


def note(text):
    print(f"      -> {text}")


def req(method, path, user=None, body=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if user:
        creds = base64.b64encode(f"{USERS[user][0]}:{USERS[user][1]}".encode()).decode()
        r.add_header("Authorization", f"Basic {creds}")
    try:
        with urllib.request.urlopen(r) as resp:
            raw = resp.read() or b"null"
            try:
                return resp.status, json.loads(raw)
            except Exception:
                return resp.status, {"raw": raw.decode(errors="replace")[:500]}
    except urllib.error.HTTPError as e:
        try:
            payload = json.loads(e.read() or b"null")
        except Exception:
            payload = None
        return e.code, payload


def show_json(obj, indent=6):
    print(json.dumps(obj, indent=2).replace("\n", "\n" + " " * indent))


# ----------------------------------------------------------------------------
banner("Health check - is the app running?")
code, _ = req("GET", "/login")
assert code == 200, f"app not reachable at {BASE} (got {code})"
note(f"{BASE} is UP")

# ----------------------------------------------------------------------------
banner("Intake - create the sample fraud case (service agent)")
intake = {
    "maskedCustomerRef": "CUST-NEWDEV-4821",
    "homeRegion": "IN-MH",
    "transactionRef": "TXN-2026-784102",
    "amount": 48750.00,
    "currency": "INR",
    "occurredAt": "2026-11-06T08:42:00+05:30",
    "status": "POSTED",
    "channel": "CARD_NOT_PRESENT",
    "maskedMerchant": "NORTHSTAR DIGITAL ••••73",
    "issueType": "MERCHANT_NOT_RECOGNISED",
    "customerStatement": "I do not recognise this purchase and still have my card.",
    "cardPossession": True,
    "merchantRecognised": False,
    "syntheticDataConfirmed": True,
    "reviewRequired": True,
}
code, body = req("POST", "/api/cases", "agent", intake)
assert code == 201, body
case_id = body["caseId"]
note(f"HTTP {code} - case created: {case_id}, status={body['status']}")

# ----------------------------------------------------------------------------
banner("Intake validation - unmasked / incomplete data is rejected")
code, body = req("POST", "/api/cases", "agent", {"maskedCustomerRef": "X"})
note(f"HTTP {code} - expected 400")
show_json(body)
assert code == 400

# ----------------------------------------------------------------------------
banner("Case read-back - summary + current state")
code, body = req("GET", f"/api/cases/{case_id}", "investigator")
note(f"HTTP {code} - status={body['status']} (state machine starts at NEW)")

# ----------------------------------------------------------------------------
banner("Investigation - bounded plan/act/retrieve (investigator)")
code, rec = req("POST", f"/api/cases/{case_id}/investigations", "investigator")
assert code == 200, rec
note(f"HTTP {code} - analysis complete")
show_json({
    "classification": rec["classification"],
    "riskLevel": rec["riskLevel"],
    "recommendedRoute": rec["recommendedRoute"],
    "confidence": rec["confidence"],
    "humanReviewRequired": rec["humanReviewRequired"],
    "ruleIds": rec["ruleIds"],
    "missingInformation": rec["missingInformation"],
    "rationale": rec["rationale"],
})

# ----------------------------------------------------------------------------
banner("Tool execution trace - every called tool, status, duration")
code, traces = req("GET", f"/api/cases/{case_id}/tool-traces", "investigator")
note(f"HTTP {code} - {len(traces)} tool invocations")
for t in traces:
    print(f"      {t['toolName']:<26} {t['status']:<12} {t['durationMs']:>4} ms   {t['traceId']}")
assert len(traces) >= 4

# ----------------------------------------------------------------------------
banner("Evidence workspace - provenance + severity per item")
code, evidence = req("GET", f"/api/cases/{case_id}/evidence", "investigator")
note(f"HTTP {code} - {len(evidence)} evidence items")
for e in evidence:
    print(f"      {e['evidenceId']:<18} {e['type']:<15} {e['severity']:<8} {e['provenance']}")

# ----------------------------------------------------------------------------
banner("Latest recommendation - the validated output contract")
code, saved = req("GET", f"/api/cases/{case_id}/recommendation", "reviewer")
note(f"HTTP {code} - mode={saved['mode']} persisted as {saved['recommendationId']}")
note(f"classification={saved['classification']} route={saved['recommendedRoute']}")

# ----------------------------------------------------------------------------
banner("Policy citations - exact retrieval metadata (RAG)")
code, citations = req("GET", f"/api/cases/{case_id}/citations", "reviewer")
note(f"HTTP {code} - {len(citations)} citation(s)")
for c in citations:
    print(f"      {c['documentId']} v{c['version']} | {c['section']} "
          f"| chunk={c['chunkId']} | score={c['score']:.3f}")
if not citations:
    note("no passage met threshold -> abstention, manual review (by design)")

# ----------------------------------------------------------------------------
banner("Case-scoped follow-up - memory isolated by caseId")
code, ans = req("POST", f"/api/cases/{case_id}/messages", "investigator",
                {"question": "What evidence is missing?"})
note(f"HTTP {code} - answer:")
print(f"      {ans['answer']}")

dup_intake = {
    **intake,
    "maskedCustomerRef": "CUST-••••-2222",
    "transactionRef": "TXN-DUP-900001",
    "amount": 999.00,
    "channel": "CARD_PRESENT",
    "maskedMerchant": "LOCAL MART ••••22",
    "issueType": "DUPLICATE_CHARGE",
    "customerStatement": "I was charged twice for the same purchase.",
    "merchantRecognised": True,
}
code, other = req("POST", "/api/cases", "agent", dup_intake)
other_id = other["caseId"]
code, ans2 = req("POST", f"/api/cases/{other_id}/messages", "investigator",
                 {"question": "What did we discuss?"})
note(f"second case {other_id} gets an independent context (no leakage)")

# ----------------------------------------------------------------------------
banner("Human review - rationale is MANDATORY (T12)")
code, body = req("POST", f"/api/cases/{case_id}/reviews", "reviewer",
                 {"disposition": "ACCEPT"})
note(f"HTTP {code} - submission without a reason is rejected")
assert code == 400

# ----------------------------------------------------------------------------
banner("Role enforcement - an agent cannot record a disposition")
code, body = req("POST", f"/api/cases/{case_id}/reviews", "agent",
                 {"disposition": "ACCEPT", "reason": "trying anyway"})
note(f"HTTP {code} - forbidden for non-reviewer role")
assert code == 403

# ----------------------------------------------------------------------------
banner("Human review - reviewer accepts the recommended route")
code, body = req("POST", f"/api/cases/{case_id}/reviews", "reviewer",
                 {"disposition": "ACCEPT",
                  "reason": "Multiple independent high-severity anomalies support fraud routing."})
assert code == 201, body
note(f"HTTP {code} - disposition recorded: {body['reviewId']}")

code, case = req("GET", f"/api/cases/{case_id}", "reviewer")
note(f"case state after review: {case['status']}  (UNDER_REVIEW -> ROUTED)")

# ----------------------------------------------------------------------------
banner("State machine guard - a routed case cannot be re-investigated")
code, body = req("POST", f"/api/cases/{case_id}/investigations", "investigator")
note(f"HTTP {code} - illegal transition rejected")
assert code == 409

# ----------------------------------------------------------------------------
banner("Audit trail - append-only reconstruction")
code, audit = req("GET", f"/api/cases/{case_id}/audit", "reviewer")
note(f"HTTP {code} - {len(audit)} events")
for a in audit:
    print(f"      #{a['seq']:<3} {a['eventType']:<28} actor={a['actorType']}:{a.get('actorId') or '-'}")

# ----------------------------------------------------------------------------
banner("Second scenario - duplicate processing takes a different route")
code, rec2 = req("POST", f"/api/cases/{other_id}/investigations", "investigator")
show_json({k: rec2[k] for k in ("classification", "riskLevel", "recommendedRoute", "confidence")})

# ----------------------------------------------------------------------------
banner("Walkthrough complete")
print("      The assistant recommended routes only - no card was blocked, no")
print("      refund issued, no customer contacted. Human review closed the loop.")
print()
