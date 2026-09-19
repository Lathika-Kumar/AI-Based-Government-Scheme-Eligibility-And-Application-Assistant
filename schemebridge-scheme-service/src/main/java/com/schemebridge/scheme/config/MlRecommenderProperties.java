package com.schemebridge.scheme.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 22B / Phase B: Configuration-driven weights and operational thresholds for ML Recommendation.
 * Configured weights are validated (must be >= 0.0) and automatically normalized
 * by dividing by the sum of active weights in the scoring pipeline.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "recommendation")
public class MlRecommenderProperties {

    private Weights weights = new Weights();
    private Ranking ranking = new Ranking();
    private Ml ml = new Ml();

    @Getter
    @Setter
    public static class Ranking {
        private boolean configured = false;
        private double semanticWeight = 0.20;
        private double stateWeight = 0.20;
        private double ageWeight = 0.05;
        private double incomeWeight = 0.10;
        private double occupationWeight = 0.15;
        private double categoryWeight = 0.05;
        private double genderWeight = 0.05;
        private double educationWeight = 0.05;
        private double benefitWeight = 0.15;

        public void setSemanticWeight(double w) { this.semanticWeight = Math.max(0.0, w); this.configured = true; }
        public void setStateWeight(double w) { this.stateWeight = Math.max(0.0, w); this.configured = true; }
        public void setAgeWeight(double w) { this.ageWeight = Math.max(0.0, w); this.configured = true; }
        public void setIncomeWeight(double w) { this.incomeWeight = Math.max(0.0, w); this.configured = true; }
        public void setOccupationWeight(double w) { this.occupationWeight = Math.max(0.0, w); this.configured = true; }
        public void setCategoryWeight(double w) { this.categoryWeight = Math.max(0.0, w); this.configured = true; }
        public void setGenderWeight(double w) { this.genderWeight = Math.max(0.0, w); this.configured = true; }
        public void setEducationWeight(double w) { this.educationWeight = Math.max(0.0, w); this.configured = true; }
        public void setBenefitWeight(double w) { this.benefitWeight = Math.max(0.0, w); this.configured = true; }
    }

    @Getter
    @Setter
    public static class Weights {
        private double occupation = 0.25;
        private double economic = 0.25;
        private double geographic = 0.20;
        private double benefit = 0.15;
        private double semantic = 0.15;
        private double demographic = 0.0;

        // Named alias weights for recommendation.weights
        private Double semanticWeight;
        private Double stateWeight;
        private Double ageWeight;
        private Double incomeWeight;
        private Double occupationWeight;
        private Double categoryWeight;
        private Double genderWeight;
        private Double educationWeight;
        private Double benefitWeight;

        public double getOccupation() {
            return occupationWeight != null ? occupationWeight : occupation;
        }

        public void setOccupation(double w) {
            this.occupation = Math.max(0.0, w);
        }

        public double getEconomic() {
            return incomeWeight != null ? incomeWeight : economic;
        }

        public void setEconomic(double w) {
            this.economic = Math.max(0.0, w);
        }

        public double getGeographic() {
            return stateWeight != null ? stateWeight : geographic;
        }

        public void setGeographic(double w) {
            this.geographic = Math.max(0.0, w);
        }

        public double getBenefit() {
            return benefitWeight != null ? benefitWeight : benefit;
        }

        public void setBenefit(double w) {
            this.benefit = Math.max(0.0, w);
        }

        public double getSemantic() {
            return semanticWeight != null ? semanticWeight : semantic;
        }

        public void setSemantic(double w) {
            this.semantic = Math.max(0.0, w);
        }

        public double getDemographic() {
            return demographic;
        }

        public void setDemographic(double w) {
            this.demographic = Math.max(0.0, w);
        }

        public double getSemanticWeight() { return getSemantic(); }
        public void setSemanticWeight(double w) { this.semanticWeight = Math.max(0.0, w); this.semantic = this.semanticWeight; }

        public double getStateWeight() { return getGeographic(); }
        public void setStateWeight(double w) { this.stateWeight = Math.max(0.0, w); this.geographic = this.stateWeight; }

        public double getIncomeWeight() { return getEconomic(); }
        public void setIncomeWeight(double w) { this.incomeWeight = Math.max(0.0, w); this.economic = this.incomeWeight; }

        public double getOccupationWeight() { return getOccupation(); }
        public void setOccupationWeight(double w) { this.occupationWeight = Math.max(0.0, w); this.occupation = this.occupationWeight; }

        public double getBenefitWeight() { return getBenefit(); }
        public void setBenefitWeight(double w) { this.benefitWeight = Math.max(0.0, w); this.benefit = this.benefitWeight; }

        public double getAgeWeight() { return ageWeight != null ? ageWeight : 0.05; }
        public void setAgeWeight(double w) { this.ageWeight = Math.max(0.0, w); }

        public double getCategoryWeight() { return categoryWeight != null ? categoryWeight : 0.05; }
        public void setCategoryWeight(double w) { this.categoryWeight = Math.max(0.0, w); }

        public double getGenderWeight() { return genderWeight != null ? genderWeight : 0.05; }
        public void setGenderWeight(double w) { this.genderWeight = Math.max(0.0, w); }

        public double getEducationWeight() { return educationWeight != null ? educationWeight : 0.05; }
        public void setEducationWeight(double w) { this.educationWeight = Math.max(0.0, w); }
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
