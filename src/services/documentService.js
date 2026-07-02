/**
 * @file documentService.js
 * @description Document vault service — mock implementation.
 *
 * Backend integration: replace function bodies with fetch() calls to ENDPOINTS.DOCUMENTS paths.
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import { DEFAULT_DOCUMENTS } from "../data/mockDocuments";
import { DOCUMENT_STATUS } from "../constants/documentTypes";

const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// In-memory mock store (reset on page reload — backend replaces this)
let _mockDocuments = [...DEFAULT_DOCUMENTS];

// ── Service functions ─────────────────────────────────────────────────────────

/**
 * Fetch all documents for the current user.
 * Backend integration: GET /api/v1/documents
 *
 * @returns {Promise<object[]>}
 */
export async function getDocuments() {
  await delay();
  return [..._mockDocuments];
}

/**
 * Upload a new document.
 * Backend integration: POST /api/v1/documents (multipart/form-data)
 *
 * @param {{ name: string, type: string, file: File, source: string }} data
 * @returns {Promise<object>} Newly created document object
 */
export async function uploadDocument({ name, type, file, source = "Manual Upload" }) {
  await delay(1000);

  if (!name || !type) {
    throw new Error("Document name and type are required.");
  }

  const newDoc = {
    id: Date.now(),
    name,
    type,
    status: DOCUMENT_STATUS.UPLOADED,
    date: new Date().toISOString().split("T")[0],
    issuer: "Uploaded by Citizen",
    expiryDate: "No Expiration",
    source,
    linkedSchemes: [],
    fileName: file?.name ?? "document",
    fileSize: file?.size ?? 0,
  };

  _mockDocuments = [..._mockDocuments, newDoc];
  return { ...newDoc };
}

/**
 * Delete a document by ID.
 * Backend integration: DELETE /api/v1/documents/:id
 *
 * @param {number|string} id
 * @returns {Promise<{ success: boolean, id: number|string }>}
 */
export async function deleteDocument(id) {
  await delay(400);
  const exists = _mockDocuments.some((d) => d.id === id);
  if (!exists) {
    const error = new Error(`Document with id "${id}" not found.`);
    error.status = 404;
    throw error;
  }
  _mockDocuments = _mockDocuments.filter((d) => d.id !== id);
  return { success: true, id };
}

/**
 * Trigger verification for a document (e.g., via DigiLocker or manual review).
 * Backend integration: POST /api/v1/documents/:id/verify
 *
 * @param {number|string} id
 * @returns {Promise<object>} Updated document
 */
export async function verifyDocument(id) {
  await delay(800);
  const doc = _mockDocuments.find((d) => d.id === id);
  if (!doc) {
    throw new Error(`Document with id "${id}" not found.`);
  }
  const updated = { ...doc, status: DOCUMENT_STATUS.VERIFIED };
  _mockDocuments = _mockDocuments.map((d) => (d.id === id ? updated : d));
  return { ...updated };
}

/**
 * Sync documents from DigiLocker.
 * Backend integration: POST /api/v1/documents/digilocker/sync
 *
 * @returns {Promise<{ synced: number, documents: object[] }>}
 */
export async function syncFromDigiLocker() {
  await delay(1500);
  // Mock: returns empty sync (no new docs from DigiLocker in mock)
  return { synced: 0, documents: [] };
}

/**
 * Get a download URL for a document.
 * Backend integration: GET /api/v1/documents/:id/download → presigned URL
 *
 * @param {number|string} id
 * @returns {Promise<{ downloadUrl: string }>}
 */
export async function getDownloadUrl(id) {
  await delay(300);
  return { downloadUrl: `#mock-download-${id}` };
}

/**
 * Get overall vault health score.
 * Backend integration: GET /api/v1/documents/vault-score
 *
 * @param {object[]} [documents] - Optional local documents to compute from
 * @returns {Promise<{ score: number, verified: number, total: number }>}
 */
export async function getVaultScore(documents) {
  await delay(200);
  const docs = documents ?? _mockDocuments;
  const verified = docs.filter((d) => d.status === DOCUMENT_STATUS.VERIFIED).length;
  const total = docs.length;
  const score = total > 0 ? Math.round((verified / total) * 100) : 0;
  return { score, verified, total };
}

const documentService = {
  getDocuments,
  uploadDocument,
  deleteDocument,
  verifyDocument,
  syncFromDigiLocker,
  getDownloadUrl,
  getVaultScore,
};

export default documentService;
