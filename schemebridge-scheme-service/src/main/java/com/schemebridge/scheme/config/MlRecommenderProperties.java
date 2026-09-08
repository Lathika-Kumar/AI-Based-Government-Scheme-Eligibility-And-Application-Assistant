package com.schemebridge.scheme.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 22B: Configuration-driven weights and operational thresholds for ML Recommendation.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "recommendation")
public class MlRecommenderProperties {

    private Weights weights = new Weights();
    private Ml ml = new Ml();

    @Getter
    @Setter
    public static class Weights {
        private double occupation = 0.25;
        private double economic = 0.25;
        private double geographic = 0.20;
        private double benefit = 0.15;
        private double semantic = 0.15;
    }

    @Getter
    @Setter
    public static class Ml {
        private boolean enabled = true;
        private int timeoutMs = 200;
        private boolean shadowMode = true;
        private String embeddingIndexPath = "E:/SCHEMEBRIDGE/data/ml_models/scheme_embeddings.index";
        private String metadataPath = "E:/SCHEMEBRIDGE/data/ml_models/scheme_embeddings_metadata.json";
        private String modelVersion = "2.2.0-hybrid-semantic-384d";
    }
}
