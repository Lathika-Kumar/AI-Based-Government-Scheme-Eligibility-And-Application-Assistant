/**
 * @file aiService.js
 * @description SchemeAI service — backend-integrated implementation with mock fallbacks.
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import apiClient from "@utils/apiClient";
import { ENDPOINTS, buildUrl } from "@config/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// ── Mock AI response templates ────────────────────────────────────────────────

const MOCK_DAILY_BRIEF = {
  generatedAt: new Date().toISOString(),
  summary: "Based on your profile and document readiness, you have 3 priority actions today.",
  actions: [
    {
      type: "apply",
      priority: "critical",
      title: "Submit PM-KISAN Application",
      description: "Deadline in 8 days. All documents are verified. 94% match score.",
      schemeId: "pm-kisan",
      ctaLabel: "Apply Now",
    },
    {
      type: "document",
      priority: "high",
      title: "Renew Domicile Certificate",
      description: "Your Domicile Certificate expired on May 10, 2024. Renewal is required for 2 saved schemes.",
      schemeId: null,
      ctaLabel: "Upload Document",
    },
    {
      type: "profile",
      priority: "normal",
      title: "Complete Profile",
      description: "Add your education details to improve match precision by 5%.",
      schemeId: null,
      ctaLabel: "Update Profile",
    },
  ],
  eligibleCount: 3,
  unappliedCount: 2,
  profileScore: 95,
};

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Get the AI-generated daily action brief for the current user.
 * Backend integration: GET /api/v1/ai/daily-brief
 *
 * @param {object} [context] - Optional user profile/context to personalize brief
 * @returns {Promise<object>} Daily brief object
 */
export async function getDailyBrief(context) {
  if (USE_MOCK) {
    await delay(900);
    return { ...MOCK_DAILY_BRIEF, generatedAt: new Date().toISOString() };
  }
  try {
    return await apiClient.get(ENDPOINTS.AI.DAILY_BRIEF, { params: context });
  } catch (error) {
    console.warn("getDailyBrief API failed, falling back to mock:", error);
    await delay(900);
    return { ...MOCK_DAILY_BRIEF, generatedAt: new Date().toISOString() };
  }
}

/**
 * Analyze scheme eligibility for the current user profile using AI.
 * Backend integration: POST /api/v1/ai/eligibility { profile, schemeIds[] }
 *
 * @param {{ profile: object, schemeIds?: string[] }} data
 * @returns {Promise<Array<{ schemeId: string, score: number, summary: string, reasons: string[] }>>}
 */
export async function analyzeEligibility({ profile, schemeIds = [] }) {
  const fallbackMock = async () => {
    await delay(1200);
    return schemeIds.map((id) => ({
      schemeId: id,
      score: Math.floor(65 + Math.random() * 31),
      summary: "Profile criteria match central/state eligibility guidelines.",
      reasons: [
        "Income within eligible bracket",
        "Age within specified range",
        "Required documents available",
      ],
    }));
  };

  if (USE_MOCK) {
    return fallbackMock();
  }
  try {
    return await apiClient.post(ENDPOINTS.AI.ELIGIBILITY_ANALYSIS, { profile, schemeIds });
  } catch (error) {
    console.warn("analyzeEligibility API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Send a chat message to SchemeAI and get a response.
 * Backend integration: POST /api/v1/ai/chat { message, conversationId, context }
 *
 * @param {{ message: string, conversationId?: string, context?: object }} data
 * @returns {Promise<{ response: string, conversationId: string, suggestions: string[] }>}
 */
export async function sendChatMessage({ message, conversationId, context }) {
  const fallbackMock = async () => {
    await delay(1000);

    const responses = [
      "Based on your profile, I recommend prioritizing the PM-KISAN application — your documents are all verified and the deadline is approaching.",
      "Your income certificate and Aadhaar card together make you eligible for 3 schemes in the housing and agriculture categories.",
      "To improve your match score, please update your educational qualification in your profile settings.",
      "The Atal Pension Yojana offers a guaranteed pension of ₹1,000–₹5,000/month after age 60. Given your age (32), starting now maximizes your corpus significantly.",
    ];

    const response = responses[Math.floor(Math.random() * responses.length)];

    return {
      response,
      conversationId: conversationId ?? `CONV-${  Date.now()}`,
      suggestions: [
        "Which documents do I need for PM-KISAN?",
        "Show my eligibility summary",
        "What is my profile completion score?",
      ],
    };
  };

  if (USE_MOCK) {
    return fallbackMock();
  }
  try {
    return await apiClient.post(ENDPOINTS.AI.CHAT, { message, conversationId, context });
  } catch (error) {
    console.warn("sendChatMessage API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Get AI-generated insights for a specific scheme.
 * Backend integration: GET /api/v1/ai/scheme/:id/insights
 *
 * @param {string} schemeId
 * @param {object} [profile]
 * @returns {Promise<object>}
 */
export async function getSchemeInsights(schemeId, profile) {
  const fallbackMock = async () => {
    await delay(800);
    return {
      schemeId,
      headline: "Strong Match — Apply Before Deadline",
      eligibilityConfidence: "High",
      estimatedBenefit: "₹6,000/year",
      keyFactors: [
        "Your occupation as Farmer directly qualifies you for this scheme.",
        "Your annual income (₹1,80,000) is well below the ₹3,00,000 ceiling.",
        "All primary documents are verified in your vault.",
      ],
      risks: [
        "Deadline approaching in 8 days — delay may result in missing this cycle.",
      ],
      nextSteps: [
        "Visit the scheme detail page",
        "Click 'Apply Now' to begin submission",
        "Upload any additional documents if required",
      ],
    };
  };

  if (USE_MOCK) {
    return fallbackMock();
  }
  try {
    return await apiClient.get(buildUrl(ENDPOINTS.AI.SCHEME_INSIGHTS, { id: schemeId }), { params: profile });
  } catch (error) {
    console.warn("getSchemeInsights API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Get AI-powered document upload suggestions based on missing documents.
 * Backend integration: POST /api/v1/ai/document-suggestions { profile, existingDocIds[] }
 *
 * @param {{ profile: object, existingDocIds: string[] }} data
 * @returns {Promise<Array<{ docName: string, reason: string, priority: string }>>}
 */
export async function getDocumentSuggestions({ profile, existingDocIds }) {
  const fallbackMock = async () => {
    await delay(600);
    return [
      {
        docName: "Bank Passbook",
        reason: "Required for Direct Benefit Transfer (DBT) for 3 schemes.",
        priority: "high",
      },
      {
        docName: "Land Ownership Record (Khatauni)",
        reason: "Required for PM-KISAN eligibility verification.",
        priority: "high",
      },
    ];
  };

  if (USE_MOCK) {
    return fallbackMock();
  }
  try {
    return await apiClient.post(ENDPOINTS.AI.DOCUMENT_SUGGESTIONS, { profile, existingDocIds });
  } catch (error) {
    console.warn("getDocumentSuggestions API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

const aiService = {
  getDailyBrief,
  analyzeEligibility,
  sendChatMessage,
  getSchemeInsights,
  getDocumentSuggestions,
};

export default aiService;
