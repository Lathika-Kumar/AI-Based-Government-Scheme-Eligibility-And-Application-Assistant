package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.RecommendationContextSnapshot;
import com.schemebridge.scheme.document.RecommendationEvent;
import com.schemebridge.scheme.document.RecommendationEventType;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.dto.request.RecommendationEventRequest;
import com.schemebridge.scheme.dto.response.RecommendationEventMetricsResponse;
import com.schemebridge.scheme.dto.response.RecommendationEventResponse;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.RecommendationEventRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for securely recording genuine citizen interaction events
 * for future offline recommendation evaluation and Learning-to-Rank (LTR).
 * Enforces strict PII sanitization on telemetry metadata.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationEventService {

    private static final Set<String> BLOCKED_EXACT_KEYS = Set.of(
            "ip", "pan", "uid", "name", "lat", "lon"
    );

    private static final Set<String> BLOCKED_KEYWORD_KEYS = Set.of(
            "aadhaar", "aadhaarnumber", "phone", "mobilenumber", "mobile",
            "email", "firstname", "lastname", "fullname", "address",
            "street", "pincode", "udid", "udidnumber", "ipaddress",
            "location", "latitude", "longitude"
    );

    private final RecommendationEventRepository eventRepository;
    private final SchemeRepository schemeRepository;

    public RecommendationEventResponse recordEvent(String authenticatedUserId, RecommendationEventRequest request) {
        log.info("Recording interaction event: userId={}, schemeCode={}, eventType={}",
                authenticatedUserId, request.getSchemeCode(), request.getEventType());

        // 1. Validate Scheme Existence
        Scheme scheme = schemeRepository.findBySchemeCode(request.getSchemeCode())
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with code: " + request.getSchemeCode()));

        // 2. Impression Deduplication: Prevent duplicate RECOMMENDATION_SHOWN flooding within the same session
        if (request.getEventType() == RecommendationEventType.RECOMMENDATION_SHOWN
                && request.getSessionId() != null
                && !request.getSessionId().isBlank()) {
            Optional<RecommendationEvent> existing = eventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    request.getSessionId(), scheme.getSchemeCode(), RecommendationEventType.RECOMMENDATION_SHOWN);
            if (existing.isPresent()) {
                log.debug("Deduplicated RECOMMENDATION_SHOWN event: sessionId={}, schemeCode={}",
                        request.getSessionId(), scheme.getSchemeCode());
                RecommendationEvent existingEvent = existing.get();
                return RecommendationEventResponse.builder()
                        .eventId(existingEvent.getId())
                        .userId(existingEvent.getUserId())
                        .schemeCode(existingEvent.getSchemeCode())
                        .eventType(existingEvent.getEventType())
                        .recommendationContext(existingEvent.getRecommendationContext())
                        .timestamp(existingEvent.getTimestamp())
                        .status("DEDUPLICATED")
                        .build();
            }
        }

        // 3. Build Recommendation Context Snapshot
        RecommendationContextSnapshot context = RecommendationContextSnapshot.builder()
                .modelVersion(EligibleSchemeRecommendationService.MODEL_VERSION)
                .recommendationRank(request.getRecommendationRank())
                .recommendationScore(request.getRecommendationScore())
                .eligibilityStatus("ELIGIBLE") // Verified server-side context
                .build();

        // 4. Construct Event Entity with Sanitized Non-PII Metadata
        Map<String, Object> sanitizedMeta = sanitizeMetadata(request.getMetadata());

        RecommendationEvent event = RecommendationEvent.builder()
                .userId(authenticatedUserId)
                .schemeCode(scheme.getSchemeCode())
                .eventType(request.getEventType())
                .recommendationContext(context)
                .timestamp(Instant.now())
                .sessionId(request.getSessionId())
                .metadata(sanitizedMeta)
                .build();

        // 5. Persist to MongoDB
        RecommendationEvent saved = eventRepository.save(event);

        return RecommendationEventResponse.builder()
                .eventId(saved.getId())
                .userId(saved.getUserId())
                .schemeCode(saved.getSchemeCode())
                .eventType(saved.getEventType())
                .recommendationContext(saved.getRecommendationContext())
                .timestamp(saved.getTimestamp())
                .status("RECORDED")
                .build();
    }

    public RecommendationEventMetricsResponse getMetrics() {
        long total = eventRepository.count();
        Map<String, Long> byType = new LinkedHashMap<>();
        for (RecommendationEventType type : RecommendationEventType.values()) {
            byType.put(type.name(), eventRepository.countByEventType(type));
        }

        List<RecommendationEvent> allEvents = eventRepository.findAll();
        long uniqueCitizens = allEvents.stream().map(RecommendationEvent::getUserId).filter(Objects::nonNull).distinct().count();
        long uniqueSchemes = allEvents.stream().map(RecommendationEvent::getSchemeCode).filter(Objects::nonNull).distinct().count();

        return RecommendationEventMetricsResponse.builder()
                .totalEvents(total)
                .eventsByType(byType)
                .uniqueCitizens(uniqueCitizens)
                .uniqueSchemes(uniqueSchemes)
                .currentModelVersion(EligibleSchemeRecommendationService.MODEL_VERSION)
                .calculatedAt(Instant.now())
                .build();
    }

    /**
     * Sanitizes telemetry metadata by recursively stripping any direct PII keys
     * (case-insensitive, hyphen/underscore/whitespace normalized).
     * Does not log stripped values.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> sanitizeMetadata(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getKey() == null) continue;
            String rawKey = entry.getKey();
            if (isKeyBlocked(rawKey)) {
                // Drop PII key completely without logging value
                continue;
            }
            Object val = entry.getValue();
            if (val instanceof Map<?, ?> nestedMap) {
                val = sanitizeMetadata((Map<String, Object>) nestedMap);
            }
            sanitized.put(rawKey, val);
        }
        return Collections.unmodifiableMap(sanitized);
    }

    private boolean isKeyBlocked(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return false;
        String normalizedKey = rawKey.toLowerCase().replace("_", "").replace("-", "").replace(" ", "").trim();

        if (BLOCKED_EXACT_KEYS.contains(normalizedKey)) {
            return true;
        }

        for (String kw : BLOCKED_KEYWORD_KEYS) {
            if (normalizedKey.contains(kw)) {
                return true;
            }
        }

        if (normalizedKey.endsWith("ip") || normalizedKey.startsWith("ip")
                || normalizedKey.contains("pancard") || normalizedKey.contains("pannumber")
                || normalizedKey.contains("uidnumber") || normalizedKey.contains("username")
                || normalizedKey.contains("clientip") || normalizedKey.contains("userip")) {
            if (!normalizedKey.contains("platform") && !normalizedKey.contains("equipment") && !normalizedKey.contains("recipient")) {
                return true;
            }
        }

        return false;
    }
}
