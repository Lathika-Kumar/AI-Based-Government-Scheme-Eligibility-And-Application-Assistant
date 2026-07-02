/**
 * Document Readiness Utility — Phase 3
 *
 * Computes how many required documents a citizen already has in their vault.
 * Matching is done by case-insensitive substring so "Aadhaar Card" matches
 * a vault doc named "Aadhaar Card (Original)".
 */

/**
 * @param {string[]} requiredDocuments - List of document names from the scheme
 * @param {Array}    vaultDocuments    - Documents array from AppContext
 * @returns {{
 *   readinessScore: number,       // 0–100
 *   readinessLabel: string,       // 'Ready' | 'Partially Ready' | 'Missing Documents'
 *   availableDocs: string[],      // required docs already in vault
 *   missingDocs: string[],        // required docs not in vault
 *   totalRequired: number,
 *   totalAvailable: number,
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
    };
  }

  const verifiedVault = (vaultDocuments || []).filter(
    (d) => d.status === "verified" || d.status === "uploaded" || d.status === "pending_review"
  );

  const availableDocs = [];
  const missingDocs = [];

  for (const req of requiredDocuments) {
    const reqLower = req.toLowerCase();
    const found = verifiedVault.some((vDoc) =>
      vDoc.name.toLowerCase().includes(reqLower.split(" ")[0].toLowerCase()) ||
      reqLower.includes(vDoc.name.toLowerCase().split(" ")[0].toLowerCase())
    );
    if (found) {
      availableDocs.push(req);
    } else {
      missingDocs.push(req);
    }
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

  const allRequired = [
    ...new Set(schemes.flatMap((s) => s.requiredDocuments || [])),
  ];

  if (allRequired.length === 0) {
return { score: 100, label: "Ready" };
}

  const verifiedVault = (vaultDocuments || []).filter(
    (d) => d.status === "verified" || d.status === "uploaded" || d.status === "pending_review"
  );

  const covered = allRequired.filter((req) => {
    const reqLower = req.toLowerCase();
    return verifiedVault.some(
      (vDoc) =>
        vDoc.name.toLowerCase().includes(reqLower.split(" ")[0].toLowerCase()) ||
        reqLower.includes(vDoc.name.toLowerCase().split(" ")[0].toLowerCase())
    );
  });

  const score = Math.round((covered.length / allRequired.length) * 100);
  const label =
    score === 100 ? "Fully Ready" : score >= 50 ? "Partially Ready" : "Needs Attention";
  return { score, label };
}
