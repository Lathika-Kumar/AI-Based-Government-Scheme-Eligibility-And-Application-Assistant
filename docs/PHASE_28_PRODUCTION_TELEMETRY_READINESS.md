# PHASE 28 — PRODUCTION TELEMETRY READINESS & GENUINE OUTCOME ACCUMULATION

## 1. Executive Summary

Phase 28 hardens and verifies SchemeBridge's production telemetry infrastructure, ensuring that real citizen interactions can accumulate safely and cleanly without data pollution, PII leaks, or premature model training.

**Core Principles & Guarantees:**
1. **Statutory Eligibility as Sole Authority**: `EligibilityEngine.evaluate()` is evaluated before feature extraction, vector construction, relevance attribution, ranking, and training-pair generation (`STATUTORY_ELIGIBILITY_VIOLATION_RATE = 0.00%`).
2. **Impression Deduplication**: Server-side and client-side impression deduplication prevents duplicate `RECOMMENDATION_SHOWN` records from flooding the database on component re-renders or repeated queries.
3. **Session Continuity**: Interaction events are deterministically linked using `sessionId + schemeCode`, preserving user journey context without relying on citizen names, emails, phones, or government IDs.
4. **Strict Zero-PII Sanitization**: Telemetry ingestion sanitizes all explicit fields and arbitrary metadata case-insensitively and recursively, stripping forbidden keys (`aadhaar`, `uid`, `pan`, `phone`, `mobile`, `email`, `name`, `firstname`, `lastname`, `address`, `street`, `ip`, `location`, `pincode`, `udid`).
5. **Synthetic Fixture Quarantine**: All 29 historical `application_events` in MongoDB are synthetic automated test fixtures and remain permanently quarantined.
6. **Zero Data Fabrication**: Zero artificial clicks, applications, feedback, or labels are manufactured.
7. **Read-Only Database Integrity**: Production MongoDB experienced zero mutations (`INSERT=0, UPDATE=0, DELETE=0, DROP=0`).
8. **Active Model Unchanged**: The production recommendation model remains `2.2.0-hybrid-semantic-384d` with `1.0.0-deterministic` fallback and a 200 ms circuit breaker.

---

## 2. Telemetry Event Lifecycle & Context

Genuine citizen interaction telemetry traverses the standardized 7-stage lifecycle:

```mermaid
flowchart LR
    A[RECOMMENDATION_SHOWN] --> B[SCHEME_VIEWED]
    B --> C[SCHEME_EXPANDED]
    C --> D[SCHEME_SAVED]
    D --> E[APPLICATION_STARTED]
    E --> F[SCHEME_APPLIED]
    F --> G[APPLICATION_COMPLETED]
```

### Minimum Telemetry Context
Every interaction event captures:
- `sessionId`: Ephemeral UUID or session token
- `schemeCode`: Authoritative scheme identifier
- `eventType`: Canonical `RecommendationEventType`
- `timestamp`: UTC epoch millisecond / Instant
- `recommendationRank`: Rank position in recommendation list (where applicable)
- `recommendationScore`: Model scoring confidence (where applicable)
- `metadata`: Sanitized, non-PII technical context (e.g. `surface`, `viewportWidth`)

Direct citizen identifiers (name, phone, email, Aadhaar, PAN, address, IP) are strictly forbidden and automatically purged.

---

## 3. Impression Deduplication Architecture

### Client-Side Deduplication
In `Recommendations.jsx`, impression tracking utilizes `useRef(new Set())` tracking rendered scheme codes. Re-rendering due to state changes or user interactions does not trigger duplicate impression events.

### Server-Side Deduplication
In `RecommendationEventService.java`:
```java
if (request.getEventType() == RecommendationEventType.RECOMMENDATION_SHOWN
        && request.getSessionId() != null
        && !request.getSessionId().isBlank()) {
    Optional<RecommendationEvent> existing = eventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
            request.getSessionId(), scheme.getSchemeCode(), RecommendationEventType.RECOMMENDATION_SHOWN);
    if (existing.isPresent()) {
        return RecommendationEventResponse.builder()
                .eventId(existing.get().getId())
                .status("DEDUPLICATED")
                ...
                .build();
    }
}
```
This guarantees that repeated impression submissions within the same session return the existing event with status `DEDUPLICATED` without creating duplicate MongoDB entries.

---

## 4. Phase 28 Readiness Monitor Specification

The `Phase28ReadinessMonitor` continuously audits telemetry accumulation:
- **Legitimate Outcome Sessions**: Counts genuine, non-synthetic citizen sessions reaching Grade 2 (`INTENT_HIGH`) or Grade 3 (`CONVERTED`).
- **Required Threshold**: 100 sessions.
- **Current Status**: `TRAINING_NOT_READY` (0 legitimate outcome sessions currently exist).
- **Offline Evaluation Safety**: When outcome sessions = 0, offline benchmark strictly reports `INSUFFICIENT_DATA`. No NDCG, MAP, or MRR metrics are manufactured.
