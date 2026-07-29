/**
 * @file documentTypes.js
 * @description Document type categories and status constants.
 * Extracted from Documents.jsx for reuse across the app.
 * Backend integration: aligns with DocumentDTO.type and DocumentDTO.status fields.
 */

/** Canonical document type categories */
export const DOCUMENT_TYPES = Object.freeze({
  IDENTITY: "Identity Proof",
  FINANCIAL: "Financial Proof",
  CATEGORY: "Category Proof",
  PROPERTY: "Property Proof",
  DOMICILE: "Domicile Proof",
  EDUCATION: "Education Proof",
  AGRICULTURE: "Agricultural Proof",
  HEALTH: "Health Proof",
  EMPLOYMENT: "Employment Proof",
});

/** Canonical document verification statuses */
export const DOCUMENT_STATUS = Object.freeze({
  VERIFIED: "verified",
  UPLOADED: "uploaded",
  PENDING_REVIEW: "pending_review",
  REJECTED: "rejected",
  EXPIRED: "expired",
});

/** Display metadata for each document status */
export const DOCUMENT_STATUS_META = Object.freeze({
  [DOCUMENT_STATUS.VERIFIED]: {
    label: "Verified",
    badge: "bg-emerald-50 text-emerald-800 border-emerald-200",
    iconColor: "text-emerald-500",
    dot: "bg-emerald-500",
    description: "Document verified against official registry.",
  },
  [DOCUMENT_STATUS.UPLOADED]: {
    label: "Uploaded",
    badge: "bg-blue-50 text-blue-800 border-blue-200",
    iconColor: "text-blue-500",
    dot: "bg-blue-400",
    description: "Document uploaded, pending verification.",
  },
  [DOCUMENT_STATUS.PENDING_REVIEW]: {
    label: "Pending Review",
    badge: "bg-amber-50 text-amber-800 border-amber-200",
    iconColor: "text-amber-500",
    dot: "bg-amber-400",
    description: "Document is under manual review.",
  },
  [DOCUMENT_STATUS.REJECTED]: {
    label: "Rejected",
    badge: "bg-rose-50 text-rose-800 border-rose-200",
    iconColor: "text-rose-500",
    dot: "bg-rose-500",
    description: "Document rejected. Please re-upload.",
  },
  [DOCUMENT_STATUS.EXPIRED]: {
    label: "Expired",
    badge: "bg-orange-50 text-orange-800 border-orange-200",
    iconColor: "text-orange-500",
    dot: "bg-orange-400",
    description: "Document has expired and needs renewal.",
  },
});

/** Document sources */
export const DOCUMENT_SOURCES = Object.freeze({
  DIGILOCKER: "DigiLocker",
  MANUAL: "Manual Upload",
  E_DISTRICT: "State e-District Portal",
  UIDAI: "UIDAI",
});

/**
 * Returns status metadata for a given document status.
 * Falls back to a neutral default.
 * @param {string} status
 */
export function getDocumentStatusMeta(status) {
  return (
    DOCUMENT_STATUS_META[status] ?? {
      label: status ?? "Unknown",
      badge: "bg-slate-50 text-slate-700 border-slate-200",
      iconColor: "text-slate-400",
      dot: "bg-slate-400",
      description: "Unknown document status.",
    }
  );
}

/**
 * Checks whether a document is expired based on its expiryDate field.
 * @param {string|null} expiryDate — ISO date string or "No Expiration"
 * @returns {boolean}
 */
export function isDocumentExpired(expiryDate) {
  if (!expiryDate || expiryDate === "No Expiration") {
return false;
}
  return new Date(expiryDate) < new Date();
}
