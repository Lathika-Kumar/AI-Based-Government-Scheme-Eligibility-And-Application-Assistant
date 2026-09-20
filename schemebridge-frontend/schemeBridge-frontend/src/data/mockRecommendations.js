// Mock Recommendations data mapping scheme IDs to Opportunity Scores, AI Badges, and countdowns.

export const MOCK_SCHEME_RECOMMENDATIONS = {
  "pm-kisan": {
    opportunityScore: 94,
    opportunityScoreExplanation: "High priority. Small landholding farmers receive direct benefit transfers (DBT) with minimal compliance requirements.",
    aiBadgeText: "High Fit: Income Support",
    deadlineDays: 8,
    benefitSummary: "₹6,000 / year paid in 3 equal installments directly to your bank account.",
    whyYouMatch: [
      "Your occupation is registered as Farmer",
      "Your annual income is below the ₹3,00,000 threshold",
      "Your state of residence matches central guidelines"
    ]
  },
  "pm-awas-yojana": {
    opportunityScore: 88,
    opportunityScoreExplanation: "Substantial capital grant. Ideal for citizens matching the Economically Weaker Section (EWS) income criteria.",
    aiBadgeText: "Priority: Capital Grant",
    deadlineDays: 22,
    benefitSummary: "Interest subsidy of up to 6.5% on home loans or ₹1.5 lakh central assistance.",
    whyYouMatch: [
      "Your annual income aligns with the EWS/LIG limit (₹6,00,000)",
      "Your age is within the eligible range (21-70 years)",
      "You have a verified Aadhaar card"
    ]
  },
  "atal-pension-yojana": {
    opportunityScore: 92,
    opportunityScoreExplanation: "Crucial long-term security. Highly recommended for self-employed individuals to secure a guaranteed monthly pension.",
    aiBadgeText: "Top Pick: Pension",
    deadlineDays: 4,
    benefitSummary: "Guaranteed minimum pension of ₹1,000 to ₹5,000 per month after age 60.",
    whyYouMatch: [
      "Your age (32 yrs) is in the perfect contribution bracket (18-40)",
      "Your caste category qualifies for central pension criteria",
      "All required document vaults are 100% verified"
    ]
  }
};

/**
 * Resolves recommendation attributes for any scheme.
 * Generates deterministic fallback values if the scheme ID is not explicitly pre-defined.
 */
export function getSchemeRecommendationDetails(schemeId, schemeName = "") {
  const predefined = MOCK_SCHEME_RECOMMENDATIONS[schemeId];
  if (predefined) {
    return predefined;
  }

  // Deterministic generator based on schemeName length/characters
  const hash = (schemeName || schemeId || "").split("").reduce((acc, char) => acc + char.charCodeAt(0), 0);
  const opportunityScore = 65 + (hash % 31); // 65 to 95
  const deadlineDays = 5 + (hash % 85); // 5 to 90 days
  const scoreExpl = opportunityScore > 85
    ? "Highly recommended. Excellent financial benefit with high auto-approval rate."
    : "Moderate opportunity. Requires additional form-filling steps.";

  const aiBadge = opportunityScore > 85 ? "AI: Top Recommendation" : "AI: Eligible Match";

  return {
    opportunityScore,
    opportunityScoreExplanation: scoreExpl,
    aiBadgeText: aiBadge,
    deadlineDays,
    benefitSummary: "Government-subsidized development benefits and financial welfare assistance.",
    whyYouMatch: [
      "Your demographic criteria matches state/central guidelines.",
      "All primary verification checklists successfully parsed."
    ]
  };
}
