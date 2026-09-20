/**
 * @file documentService.js
 * @description Citizen Document Vault service — backend-integrated with MongoDB GridFS and mock fallbacks.
 *
 * Set VITE_USE_MOCK_API=true in .env to force mock responses.
 * All functions fall back to mock logic if the API call throws.
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import { DEFAULT_DOCUMENTS } from "../data/mockDocuments";
import { DOCUMENT_STATUS } from "../constants/documentTypes";
import { schemeApi } from "@utils/apiClient";
import { ENDPOINTS, buildUrl } from "@config/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// In-memory mock store (reset on page reload — backend replaces this)
let _mockDocuments = [...DEFAULT_DOCUMENTS];

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Fetch all documents in the citizen's permanent Document Vault.
 * Backend integration: GET /api/documents/vault
 *
 * @returns {Promise<object[]>}
 */
export async function getDocuments() {
  const fallbackMock = async () => {
    await delay();
    return [..._mockDocuments];
  };

  if (USE_MOCK) return fallbackMock();
  try {
    const res = await schemeApi.get(ENDPOINTS.DOCUMENTS.LIST);
    if (!res.error && res.data) {
      const list = Array.isArray(res.data) ? res.data : (res.data.content || []);
      // Normalize backend entity fields to frontend expectations
      return list.map((d) => ({
        id: d.id,
        name: d.documentName || d.name,
        documentName: d.documentName || d.name,
        documentCode: d.documentCode,
        canonicalDocumentCode: d.canonicalDocumentCode,
        type: d.documentType || d.type || "Identity Proof",
        status: d.detailedStatus === "ADMIN_VERIFIED" ? "verified" :
                d.detailedStatus === "REJECTED" ? "rejected" :
                d.detailedStatus === "CORRECTION_REQUIRED" ? "reupload_requested" :
                d.detailedStatus === "UPLOADED" ? "uploaded" : "pending_review",
        detailedStatus: d.detailedStatus,
        verificationStatus: d.verificationStatus,
        officerStatus: d.detailedStatus,
        aiStatus: d.aiVerificationResult || "AI_PASSED",
        aiScore: d.verificationScore ?? 96,
        date: d.uploadedAt ? new Date(d.uploadedAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
        uploadDate: d.uploadedAt ? new Date(d.uploadedAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
        issuer: d.issuer || "Uploaded by Citizen",
        expiryDate: d.expiryDate || "No Expiration",
        holderName: d.holderName || "Citizen",
        docNumber: d.docNumber || "—",
        source: d.source || "Manual Upload",
        linkedSchemes: d.linkedApplications || [],
        fileName: d.fileName || "document.pdf",
        fileSize: d.fileSize ? `${(d.fileSize / (1024 * 1024)).toFixed(1)} MB` : "1.4 MB",
        storageReference: d.storageReference,
        gridFsFileId: d.gridFsFileId,
      }));
    }
    return fallbackMock();
  } catch (error) {
    console.warn("getDocuments API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Upload a new document into the citizen's permanent Document Vault.
 * Backend integration: POST /api/documents/vault/upload (multipart/form-data)
 *
 * @param {{ name: string, type: string, file: File, source: string, documentCode: string }} data
 * @returns {Promise<object>} Newly created document object
 */
export async function uploadDocument(data) {
  const fallbackMock = async () => {
    await delay(600);
    const name = data?.name || "Document";
    const type = data?.type || "Identity Proof";
    const newDoc = {
      id: `DOC-${Date.now()}`,
      name,
      documentName: name,
      type,
      status: DOCUMENT_STATUS.UPLOADED,
      detailedStatus: "UPLOADED",
      date: new Date().toISOString().split("T")[0],
      issuer: data?.issuer || "Uploaded by Citizen",
      expiryDate: data?.expiryDate || "No Expiration",
      source: data?.source || "Manual Upload",
      linkedSchemes: [],
      fileName: data?.file?.name ?? "document.pdf",
      fileSize: data?.file?.size ? `${(data.file.size / (1024 * 1024)).toFixed(1)} MB` : "1.4 MB",
    };
    _mockDocuments = [..._mockDocuments, newDoc];
    return { ...newDoc };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    let formData;
    if (data instanceof FormData) {
      formData = data;
    } else {
      formData = new FormData();
      if (data.name) formData.append("name", data.name);
      if (data.type) formData.append("type", data.type);
      if (data.source) formData.append("source", data.source);
      if (data.documentCode) formData.append("documentCode", data.documentCode);
      if (data.issuer) formData.append("issuer", data.issuer);
      if (data.expiryDate) formData.append("expiryDate", data.expiryDate);
      if (data.holderName) formData.append("holderName", data.holderName);
      if (data.docNumber) formData.append("docNumber", data.docNumber);
      if (data.file) formData.append("file", data.file);
    }
    const res = await schemeApi.upload(ENDPOINTS.DOCUMENTS.UPLOAD, formData);
    if (!res.error && res.data) {
      const d = res.data;
      return {
        id: d.id,
        name: d.documentName || d.name,
        documentName: d.documentName || d.name,
        documentCode: d.documentCode,
        canonicalDocumentCode: d.canonicalDocumentCode,
        type: d.documentType || d.type || "Identity Proof",
        status: d.detailedStatus === "ADMIN_VERIFIED"
          ? "verified"
          : (d.verificationStatus === "REJECTED" || d.detailedStatus === "AI_REJECTED")
          ? "rejected"
          : "uploaded",
        detailedStatus: d.detailedStatus,
        verificationStatus: d.verificationStatus,
        officerStatus: d.detailedStatus,
        aiStatus: d.aiVerificationResult || (d.verificationStatus === "REJECTED" ? "AI_REJECTED" : "AI_PASSED"),
        aiScore: d.verificationScore ?? (d.verificationStatus === "REJECTED" ? 25 : 96),
        date: d.uploadedAt ? new Date(d.uploadedAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
        uploadDate: d.uploadedAt ? new Date(d.uploadedAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
        issuer: d.issuer || "Uploaded by Citizen",
        expiryDate: d.expiryDate || "No Expiration",
        holderName: d.holderName || "—",
        docNumber: d.docNumber || "—",
        rejectionReason: d.rejectionReason,
        identityMatchStatus: d.identityMatchStatus,
        source: d.source || "Manual Upload",
        linkedSchemes: d.linkedApplications || [],
        fileName: d.fileName || "document.pdf",
        fileSize: d.fileSize ? `${(d.fileSize / (1024 * 1024)).toFixed(1)} MB` : "1.4 MB",
        storageReference: d.storageReference,
        gridFsFileId: d.gridFsFileId,
      };
    }
    return fallbackMock();
  } catch (error) {
    console.warn("uploadDocument API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Delete a document by ID from citizen vault.
 * Backend integration: DELETE /api/documents/vault/:documentId
 *
 * @param {number|string} id
 * @returns {Promise<{ success: boolean, id: number|string }>}
 */
export async function deleteDocument(id) {
  const fallbackMock = async () => {
    await delay(300);
    _mockDocuments = _mockDocuments.filter((d) => d.id !== id);
    return { success: true, id };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    const res = await schemeApi.delete(buildUrl(ENDPOINTS.DOCUMENTS.DELETE, { documentId: id }));
    if (!res.error) {
      return { success: true, id };
    }
    return fallbackMock();
  } catch (error) {
    console.warn("deleteDocument API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Download a document binary from citizen vault.
 * Backend integration: GET /api/documents/vault/:documentId/download
 */
export async function downloadVaultDocument(id) {
  return await schemeApi.download(buildUrl(ENDPOINTS.DOCUMENTS.DOWNLOAD, { documentId: id }));
}

/**
 * Trigger verification for a document (e.g., via official verification or manual review).
 * Backend integration: POST /api/documents/:id/verify
 *
 * @param {number|string} id
 * @returns {Promise<object>} Updated document
 */
export async function verifyDocument(id) {
  const fallbackMock = async () => {
    await delay(500);
    const doc = _mockDocuments.find((d) => d.id === id);
    if (!doc) throw new Error(`Document with id "${id}" not found.`);
    const updated = { ...doc, status: DOCUMENT_STATUS.VERIFIED };
    _mockDocuments = _mockDocuments.map((d) => (d.id === id ? updated : d));
    return { ...updated };
  };

  if (USE_MOCK) return fallbackMock();
  try {
    const res = await schemeApi.post(buildUrl(ENDPOINTS.DOCUMENTS.VERIFY, { documentId: id }));
    if (!res.error && res.data) {
      return res.data;
    }
    return fallbackMock();
  } catch (error) {
    console.warn("verifyDocument API failed, falling back to mock:", error);
    return fallbackMock();
  }
}

/**
 * Get a download URL for a document.
 * @param {number|string} id
 * @returns {Promise<{ downloadUrl: string }>}
 */
export async function getDownloadUrl(id) {
  return { downloadUrl: `/api/documents/vault/${encodeURIComponent(id)}/download` };
}

/**
 * Get overall vault health score.
 * @param {object[]} [documents] - Optional local documents to compute from
 * @returns {Promise<{ score: number, verified: number, total: number }>}
 */
export async function getVaultScore(documents) {
  const docs = documents ?? _mockDocuments;
  const verified = docs.filter((d) => d.status === DOCUMENT_STATUS.VERIFIED || d.detailedStatus === "ADMIN_VERIFIED").length;
  const total = docs.length;
  const score = total > 0 ? Math.round((verified / total) * 100) : 0;
  return { score, verified, total };
}

const documentService = {
  getDocuments,
  uploadDocument,
  deleteDocument,
  downloadVaultDocument,
  verifyDocument,
  getDownloadUrl,
  getVaultScore,
};

export default documentService;
