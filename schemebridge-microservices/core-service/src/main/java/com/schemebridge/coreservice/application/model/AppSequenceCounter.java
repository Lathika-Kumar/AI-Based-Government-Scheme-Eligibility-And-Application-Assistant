package com.schemebridge.coreservice.application.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * AppSequenceCounter provides a MongoDB-based atomic sequence counter
 * for generating application numbers like SB-APP-2026-000001.
 * Each year gets its own counter (seq_2026, seq_2027, etc.)
 */
@Document(collection = "app_sequence_counters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSequenceCounter {
    @Id
    private String id;     // e.g., "seq_2026"
    private long sequence; // auto-incremented
}
