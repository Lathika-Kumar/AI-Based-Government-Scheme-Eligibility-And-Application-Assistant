import { getDocReadinessForScheme } from "./documentReadiness";

/**
 * Enhanced Weighted Eligibility Engine — Phase 4
 *
 * Evaluates a user profile against a scheme's eligibility rules using a weighted model.
 *
 * Mandatory Criteria (State, Age, Caste, Gender) - if any fail:
 *   - Status = 'not_eligible'
 *   - Match Score = 0
 *
 * Non-Mandatory Criteria Weights:
 *   - Base score for passing mandatory gates: 40%
 *   - Income Eligibility: 25%
 *   - Occupation Alignment: 20%
 *   - Document Readiness: 15%
 *
 * @param {Object} profile - User profile: { age, annualIncome, occupation, caste, gender, state }
 * @param {Object} scheme  - Scheme object with eligibility rules
 * @param {Array}  [vaultDocuments] - List of documents in vault (optional)
 * @returns {Object} {
 *   status: 'eligible' | 'possibly_eligible' | 'not_eligible',
 *   matchScore: number (0–100),
 *   qualifyingReasons: string[],
 *   unmetConditions: string[],
 *   totalCriteria: number,
 *   passedCriteria: number,
 * }
 */
export function checkEligibility(profile = {}, scheme = {}, vaultDocuments = []) {
  const qualifyingReasons = [];
  const unmetConditions = [];
  const rules = scheme?.eligibility || {};

  // If vaultDocuments is not passed, attempt to read from localStorage for backward compatibility
  if (!vaultDocuments || vaultDocuments.length === 0) {
    try {
      const savedUser = localStorage.getItem("schemebridge_user");
      const userKey = savedUser
        ? `schemebridge_documents_${JSON.parse(savedUser).email || "default"}`
        : "schemebridge_documents";
      const savedDocs = localStorage.getItem(userKey);
      if (savedDocs) {
        vaultDocuments = JSON.parse(savedDocs);
      }
    } catch {
      // Fallback to empty array
    }
  }

  // ── PHASE 1: MANDATORY GATEKEEPERS ─────────────────────────────────────────
  let mandatoryFailed = false;
  const userState = profile.state || "";
  const userAge = Number(profile.age) || 0;
  const userCaste = profile.caste || "";
  const userGender = profile.gender || "";

  // 1. State check
  if (rules.states && rules.states.length > 0) {
    if (!rules.states.includes(userState)) {
      mandatoryFailed = true;
      unmetConditions.push(
        `This scheme is available only for ${rules.states.join(", ")} residents.`
      );
    } else {
      qualifyingReasons.push(
        `You reside in the eligible state of ${userState}.`
      );
    }
  }

  // 2. Age check
  if (rules.minAge !== undefined || rules.maxAge !== undefined) {
    const min = rules.minAge ?? 0;
    const max = rules.maxAge ?? 100;

    if (userAge < min) {
      mandatoryFailed = true;
      unmetConditions.push(
        `Your age (${userAge} yrs) is below the minimum required age of ${min} years.`
      );
    } else if (userAge > max) {
      mandatoryFailed = true;
      unmetConditions.push(
        `Your age (${userAge} yrs) exceeds the maximum cutoff of ${max} years.`
      );
    } else {
      qualifyingReasons.push(
        `Your age (${userAge} yrs) matches the eligible criteria of ${min}–${max} years.`
      );
    }
  }

  // 3. Social Category / Caste check
  if (rules.castes && rules.castes.length > 0) {
    if (!rules.castes.includes(userCaste)) {
      mandatoryFailed = true;
      unmetConditions.push(
        `This scheme is reserved for [${rules.castes.join(", ")}] categories. Your profile is "${userCaste || "not specified"}".`
      );
    } else {
      qualifyingReasons.push(
        `Your social category (${userCaste}) is covered under this scheme.`
      );
    }
  }

  // 4. Gender check
  if (rules.genders && rules.genders.length > 0) {
    if (!rules.genders.includes(userGender)) {
      mandatoryFailed = true;
      unmetConditions.push(
        `This scheme is exclusive to ${rules.genders.join(" / ")} applicants.`
      );
    } else {
      qualifyingReasons.push(
        `This scheme matches your gender (${userGender}).`
      );
    }
  }

  // If any mandatory criteria failed, return 0% match score immediately
  if (mandatoryFailed) {
    return {
      status: "not_eligible",
      matchScore: 0,
      qualifyingReasons,
      unmetConditions,
      totalCriteria: 4,
      passedCriteria: 0,
    };
  }

  // ── PHASE 2: WEIGHTED SCORING FOR ELIGIBLE CITIZENS ────────────────────────
  // Passing all mandatory gatekeepers guarantees a base score of 40%
  let score = 40;

  // A. Income (25% Weight)
  const userIncome = Number(profile.annualIncome) || Number(profile.income) || 0;
  if (rules.maxIncome !== undefined) {
    const limit = rules.maxIncome;
    if (userIncome <= limit) {
      score += 25;
      qualifyingReasons.push(
        `Your income (₹${userIncome.toLocaleString("en-IN")}) is within the ceiling of ₹${limit.toLocaleString("en-IN")}.`
      );
    } else {
      unmetConditions.push(
        `Your income (₹${userIncome.toLocaleString("en-IN")}) exceeds the scheme's limit of ₹${limit.toLocaleString("en-IN")}.`
      );
    }
  } else {
    // If no income rule, subscriber automatically gains the full weight
    score += 25;
  }

  // B. Occupation (20% Weight)
  const userOccupation = profile.occupation || "";
  if (rules.occupations && rules.occupations.length > 0) {
    if (rules.occupations.includes(userOccupation)) {
      score += 20;
      qualifyingReasons.push(
        `Your occupation (${userOccupation}) is eligible for this scheme.`
      );
    } else {
      unmetConditions.push(
        `This scheme targets [${rules.occupations.join(", ")}], but your occupation is "${userOccupation || "not specified"}".`
      );
    }
  } else {
    // If no occupation rule, subscriber gains the full weight
    score += 20;
  }

  // C. Document Readiness (15% Weight)
  const readiness = getDocReadinessForScheme(scheme?.requiredDocuments || [], vaultDocuments);
  const docWeightContribution = Math.round((readiness.readinessScore / 100) * 15);
  score += docWeightContribution;

  // Round final score to max 100
  const finalScore = Math.min(Math.max(Math.round(score), 0), 100);

  // Set status based on score and document completion
  let status = "possibly_eligible";
  if (finalScore === 100 && unmetConditions.length === 0 && readiness.readinessScore === 100) {
    status = "eligible";
  } else if (finalScore < 50) {
    status = "not_eligible";
  }

  return {
    status,
    matchScore: finalScore,
    qualifyingReasons,
    unmetConditions,
    totalCriteria: 6,
    passedCriteria: qualifyingReasons.length,
  };
}
