package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the resolved attribution of citizen interactions for a single scheme within a session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributedSession {

    private String sessionId;
    private String schemeCode;
    private RelevanceGrade terminalGrade;

    @Builder.Default
    private List<SessionInteractionEvent> observedEvents = new ArrayList<>();

    private boolean isExcluded;
    private String exclusionReason;
    private String splitAssignment; // "TRAIN", "VALIDATION", "TEST", or "NONE"

    public List<SessionInteractionEvent> getObservedEvents() {
        return Collections.unmodifiableList(observedEvents);
    }
}
