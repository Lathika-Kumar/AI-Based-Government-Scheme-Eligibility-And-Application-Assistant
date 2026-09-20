/**
 * Document Readiness Utility — Phase 24 Production Hardened
 *
 * Computes how many required documents a citizen already has in their vault.
 * Supports:
 *   A. String requirements (e.g. "Aadhaar Card", "Income Certificate / BPL Card")
 *   B. Object requirements (e.g. { documentName: "Identity Proof", alternativeGroupType: "ONE_OF", alternatives: [...] })
 *   C. ONE_OF alternative groups (e.g. { type: "ONE_OF", options: ["Income Certificate", "BPL Card"] })
 *   D. Nested/structured document requirement objects
 *
 * Guaranteed:
 *   - NEVER crashes on object requirements
 *   - 100% ONE_OF semantics: If ANY valid alternative exists in vault, requirement is satisfied
 */

/**
 * Extract searchable string candidates and a display name from a requirement item.
 * @param {string|object} req
 * @returns {{ displayName: string, candidates: string[], isOneOf: boolean }}
 */
function parseRequirement(req) {
  if (!req) {
    return { displayName: "Required Document", candidates: [], isOneOf: false };
  }

  if (typeof req === "string") {
    const trimmed = req.trim();
    const candidates = [trimmed];
    let isOneOf = false;

    if (trimmed.includes("/") || /\s+or\s+/i.test(trimmed)) {
      isOneOf = true;
      const parts = trimmed.split(/\/|\s+or\s+/i);
      for (const part of parts) {
        const clean = part.replace(/[()]/g, "").trim();
        if (clean.length >= 2 && !candidates.includes(clean)) {
          candidates.push(clean);
        }
      }
    }

    return { displayName: trimmed, candidates, isOneOf };
  }

  // Handle Object requirements
  let displayName =
    req.documentName ||
    req.officialDocumentName ||
    req.name ||
    req.title ||
    "";

  const candidates = [];
  let isOneOf =
    req.type === "ONE_OF" ||
    req.rule === "ONE_OF" ||
    req.alternativeGroupType === "ONE_OF" ||
    req.alternativeGroup?.rule === "ONE_OF";

  // Check canonicalDocumentCode and documentCode
  if (req.canonicalDocumentCode) {
    candidates.push(req.canonicalDocumentCode.trim());
    candidates.push(req.canonicalDocumentCode.replace(/_/g, " ").trim());
  }
  if (req.documentCode) {
    candidates.push(req.documentCode.trim());
    candidates.push(req.documentCode.replace(/_/g, " ").trim());
  }

  // Check options (e.g., { type: "ONE_OF", options: ["Income Certificate", "BPL Card"] })
  if (Array.isArray(req.options)) {
    isOneOf = true;
    for (const opt of req.options) {
      const optStr = typeof opt === "string" ? opt : opt?.optionName || opt?.name || "";
      if (optStr.trim()) candidates.push(optStr.trim());
    }
  }

  // Check alternatives (e.g., { alternatives: ["PAN Card", "Voter ID"] })
  if (Array.isArray(req.alternatives)) {
    isOneOf = true;
    for (const alt of req.alternatives) {
      const altStr = typeof alt === "string" ? alt : alt?.optionName || alt?.name || "";
      if (altStr.trim()) candidates.push(altStr.trim());
    }
  }

  // Check nested alternativeGroup.options
  if (Array.isArray(req.alternativeGroup?.options)) {
    isOneOf = true;
    for (const opt of req.alternativeGroup.options) {
      const optStr = typeof opt === "string" ? opt : opt?.optionName || opt?.name || "";
      if (optStr.trim()) candidates.push(optStr.trim());
    }
  }

  if (!displayName) {
    if (candidates.length > 0) {
      displayName = isOneOf ? `Any ONE of: ${candidates.join(" / ")}` : candidates[0];
    } else {
      displayName = "Required Document";
    }
  }

  if (!candidates.includes(displayName)) {
    candidates.unshift(displayName);
  }

  // Also check if displayName itself contains / or "or"
  if (displayName.includes("/") || /\s+or\s+/i.test(displayName)) {
    isOneOf = true;
    const parts = displayName.split(/\/|\s+or\s+/i);
    for (const part of parts) {
      const clean = part.replace(/[()]/g, "").trim();
      if (clean.length >= 2 && !candidates.includes(clean)) {
        candidates.push(clean);
      }
    }
  }

  return { displayName, candidates, isOneOf };
}

/**
 * Check if any candidate name matches any vault document name.
 */
const GENERIC_DOC_TERMS = new Set([
  "certificate", "certificates",
  "card", "cards",
  "proof", "proofs",
  "document", "documents",
  "letter", "letters",
  "copy", "copies",
  "affidavit",
  "form",
  "record", "records",
  "details",
  "passbook",
  "receipt",
  "slip",
  "statement",
  "report",
  "valid",
  "official",
  "government",
  "issued",
  "authority"
]);

/**
 * Validate whether a vault document is active, valid, not expired, and satisfies identity checks.
 * Disqualifies any documents with MISMATCH, UNCERTAIN, REJECTED, AI_REJECTED, AI_FAILED, or EXPIRED.
 * @param {object|string} vDoc
 * @returns {boolean}
 */
export function isVaultDocValidAndReusable(vDoc) {
  if (!vDoc) return false;
  if (typeof vDoc === "string") return true;

  const detailedStatus = String(vDoc.detailedStatus || "").toUpperCase();
  const verificationStatus = String(vDoc.verificationStatus || "").toUpperCase();
  const status = String(vDoc.status || "").toUpperCase();

  // Disqualify any rejected status
  if (
    detailedStatus === "AI_REJECTED" ||
    detailedStatus === "AI_FAILED" ||
    detailedStatus === "REJECTED" ||
    verificationStatus === "REJECTED" ||
    status === "REJECTED"
  ) {
    return false;
  }

  // Disqualify identity mismatch or uncertainty
  const idMatch = String(vDoc.identityMatchStatus || "").toUpperCase();
  if (idMatch === "MISMATCH" || idMatch === "UNCERTAIN") {
    return false;
  }

  // Disqualify expired documents
  if (vDoc.expiryDate) {
    const exp = new Date(vDoc.expiryDate);
    if (!isNaN(exp.getTime()) && exp.getTime() < Date.now()) {
      return false;
    }
  }

  // Disqualify low verification scores (< 50) if present
  if (vDoc.verificationScore != null && Number(vDoc.verificationScore) < 50) {
    return false;
  }

  return true;
}

/**
 * Find the matching document in citizen vault documents.
 * @param {string[]} candidates
 * @param {Array} vaultDocuments
 * @returns {object|null}
 */
export function findMatchingVaultDocument(candidates, vaultDocuments) {
  if (!candidates || candidates.length === 0 || !vaultDocuments || vaultDocuments.length === 0) return null;

  const getDocStrings = (vDoc) => {
    if (!vDoc) return [];
    if (typeof vDoc === "string") return [vDoc.toLowerCase().trim()];
    const list = [];
    if (vDoc.name) list.push(vDoc.name.toLowerCase().trim());
    if (vDoc.documentName) list.push(vDoc.documentName.toLowerCase().trim());
    if (vDoc.type) list.push(vDoc.type.toLowerCase().trim());
    if (vDoc.canonicalDocumentCode) {
      list.push(vDoc.canonicalDocumentCode.toLowerCase().trim());
      list.push(vDoc.canonicalDocumentCode.toLowerCase().replace(/_/g, " ").trim());
    }
    if (vDoc.documentCode) {
      list.push(vDoc.documentCode.toLowerCase().trim());
      list.push(vDoc.documentCode.toLowerCase().replace(/_/g, " ").trim());
    }
    return list.filter(Boolean);
  };

  for (const vDoc of vaultDocuments) {
    if (!vDoc || !isVaultDocValidAndReusable(vDoc)) continue;
    const vStrings = getDocStrings(vDoc);
    if (vStrings.length === 0) continue;

    for (const cand of candidates) {
      const cLower = cand.toLowerCase().trim();
      if (!cLower) continue;

      for (const vName of vStrings) {
        // Direct full equality or substring match in either direction
        if (vName === cLower) return vDoc;
        if (vName.includes(cLower) || cLower.includes(vName)) return vDoc;

        // Significant domain word token match excluding generic doc terms
        const candWords = cLower.split(/[\s,()/-]+/).filter((w) => w.length >= 3 && !GENERIC_DOC_TERMS.has(w));
        const vaultWords = vName.split(/[\s,()/-]+/).filter((w) => w.length >= 3 && !GENERIC_DOC_TERMS.has(w));

        if (candWords.length > 0 && vaultWords.length > 0) {
          const hasMatch = candWords.some((cw) => {
            if (vaultWords.includes(cw)) return true;
            // Recognized domain synonyms
            if ((cw === "aadhaar" && vaultWords.includes("aadhar")) || (cw === "aadhar" && vaultWords.includes("aadhaar"))) return true;
            if ((cw === "caste" && vaultWords.includes("community")) || (cw === "community" && vaultWords.includes("caste"))) return true;
            if (cw === "passbook" && vaultWords.includes("bank")) return true;
            if (cw === "khatauni" && (vaultWords.includes("land") || vaultWords.includes("patta") || vaultWords.includes("ror"))) return true;
            return false;
          });
          if (hasMatch) return vDoc;
        }
      }
    }
  }
  return null;
}

export function isRequirementSatisfied(candidates, verifiedVault) {
  return Boolean(findMatchingVaultDocument(candidates, verifiedVault));
}

/**
 * @param {Array<string|object>} requiredDocuments - List of document names or objects
 * @param {Array}                vaultDocuments    - Documents array from AppContext
 * @returns {{
 *   readinessScore: number,       // 0–100
 *   readinessLabel: string,       // 'Ready' | 'Partially Ready' | 'Missing Documents'
 *   availableDocs: string[],      // required docs already in vault
 *   missingDocs: string[],        // required docs not in vault
 *   totalRequired: number,
 *   totalAvailable: number,
 *   availableCount: number,
 *   missingCount: number,
 *   isReady: boolean,
 *   evaluatedItems: Array<{
 *     documentName: string,
 *     name: string,
 *     mandatory: boolean,
 *     issuingAuthority: string|null,
 *     alternatives: string[],
 *     isAvailable: boolean,
 *     isVerified: boolean,
 *     availabilityStatus: "VERIFIED" | "PENDING" | "NOT_AVAILABLE",
 *     statusLabel: string,
 *     statusIcon: "✓" | "◐" | "✗",
 *     matchedVaultDoc: any,
 *     rawRequirement: any
 *   }>
 * }}
 */
export function getDocReadinessForScheme(requiredDocuments, vaultDocuments) {
  if (!requiredDocuments || requiredDocuments.length === 0) {
    return {
      readinessScore: 100,
      readinessLabel: "Ready",
      availableDocs: [],
      missingDocs: [],
      totalRequired: 0,
      totalAvailable: 0,
      availableCount: 0,
      missingCount: 0,
      isReady: true,
      evaluatedItems: [],
    };
  }

  const availableDocs = [];
  const missingDocs = [];
  const evaluatedItems = [];

  for (const req of requiredDocuments) {
    const { displayName, candidates } = parseRequirement(req);
    const isVaultLoaded = Array.isArray(vaultDocuments);
    const matchedDoc = isVaultLoaded ? findMatchingVaultDocument(candidates, vaultDocuments) : null;
    let availabilityStatus = "NOT_AVAILABLE";
    let availabilityLabel = "Not Available";
    let statusIcon = "✗";
    let isAvailable = false;
    let isVerified = false;
    const st = String(matchedDoc?.status || matchedDoc?.detailedStatus || matchedDoc?.verificationStatus || "").toLowerCase();
    const isDocOfficerVerified = Boolean(matchedDoc && (matchedDoc.verified === true || st === "verified" || st === "admin_verified"));
    const isDocAiVerified = Boolean(matchedDoc && (st === "ai_verified" || Boolean(matchedDoc.aiVerificationResult)));
    const isDocCorrection = Boolean(matchedDoc && (st === "correction_required" || Boolean(matchedDoc.correctionReason)));

    if (matchedDoc) {
      isAvailable = true;
      if (isDocOfficerVerified) {
        availabilityStatus = "VERIFIED";
        availabilityLabel = "Available (Verified)";
        statusIcon = "✓";
        isVerified = true;
      } else if (isDocAiVerified) {
        availabilityStatus = "PENDING";
        availabilityLabel = "Available (AI Verified — Pending Officer Review)";
        statusIcon = "✓";
        isVerified = false;
      } else if (isDocCorrection) {
        availabilityStatus = "CORRECTION_REQUIRED";
        availabilityLabel = "Correction Required";
        statusIcon = "!";
        isVerified = false;
      } else {
        availabilityStatus = "PENDING";
        availabilityLabel = "Available (Uploaded — Pending Verification)";
        statusIcon = "✓";
        isVerified = false;
      }
    } else if (!isVaultLoaded) {
      // Do NOT treat req.verified on raw requirement as proof citizen has verified document
      availabilityStatus = "UNAVAILABLE";
      availabilityLabel = "Document status unavailable";
      statusIcon = "—";
      isAvailable = false;
      isVerified = false;
    } else {
      availabilityStatus = "NOT_AVAILABLE";
      availabilityLabel = "Not Available";
      statusIcon = "✗";
      isAvailable = false;
      isVerified = false;
    }

    const statusLabel = isAvailable ? "Uploaded" : (!isVaultLoaded ? "Unavailable" : "Missing");

    const mandatory = typeof req === "object" && req !== null ? req.mandatory !== false : true;
    const issuingAuthority = typeof req === "object" && req !== null ? req.issuingAuthority : null;
    const alternatives = typeof req === "object" && req !== null ? (req.alternatives || []) : [];

    if (isAvailable) {
      availableDocs.push(displayName);
    } else {
      missingDocs.push(displayName);
    }

    evaluatedItems.push({
      documentName: displayName,
      name: displayName,
      mandatory,
      issuingAuthority,
      alternatives,
      isAvailable,
      isVerified,
      isOfficerVerified: isDocOfficerVerified,
      isAiVerified: isDocAiVerified,
      isCorrectionRequired: isDocCorrection,
      availabilityStatus,
      availabilityLabel,
      statusLabel,
      statusIcon,
      matchedVaultDoc: matchedDoc,
      rawRequirement: req,
    });
  }

  const totalRequired = requiredDocuments.length;
  const totalAvailable = availableDocs.length;
  const readinessScore =
    totalRequired > 0 ? Math.round((totalAvailable / totalRequired) * 100) : 100;

  let readinessLabel;
  if (readinessScore === 100) {
    readinessLabel = "Ready";
  } else if (readinessScore >= 50) {
    readinessLabel = "Partially Ready";
  } else {
    readinessLabel = "Missing Documents";
  }

  return {
    readinessScore,
    readinessLabel,
    availableDocs,
    missingDocs,
    totalRequired,
    totalAvailable,
    availableCount: totalAvailable,
    missingCount: missingDocs.length,
    isReady: readinessScore === 100,
    evaluatedItems,
  };
}

/**
 * Computes overall vault completeness score across all required documents
 * for a list of schemes (e.g., applied or saved schemes).
 *
 * @param {Array} schemes        - List of scheme objects
 * @param {Array} vaultDocuments - Documents array from AppContext
 * @returns {{ score: number, label: string }}
 */
export function getOverallVaultScore(schemes, vaultDocuments) {
  if (!schemes || schemes.length === 0) {
    return { score: 0, label: "No schemes tracked" };
  }

  const allRequirements = schemes.flatMap((s) => s.requiredDocuments || []);
  if (allRequirements.length === 0) {
    return { score: 100, label: "Ready" };
  }

  const verifiedVault = (vaultDocuments || []).filter(
    (d) => d.status === "verified" || d.status === "uploaded" || d.status === "pending_review"
  );

  let coveredCount = 0;
  for (const req of allRequirements) {
    const { candidates } = parseRequirement(req);
    if (isRequirementSatisfied(candidates, verifiedVault)) {
      coveredCount++;
    }
  }

  const score = Math.round((coveredCount / allRequirements.length) * 100);
  const label =
    score === 100 ? "Fully Ready" : score >= 50 ? "Partially Ready" : "Needs Attention";
  return { score, label };
}
