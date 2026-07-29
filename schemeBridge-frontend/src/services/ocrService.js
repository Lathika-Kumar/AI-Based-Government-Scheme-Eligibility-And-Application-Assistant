/**
 * @file ocrService.js
 * @description Simulated AI-OCR Data Extraction Engine for SchemeBridge government documents.
 * Extracts structured identity & financial attributes from uploaded documents (Aadhaar, PAN, Income Certificate, etc.)
 */

import { MOCK_LOADING_DELAY_MS } from "../config/constants";
import apiClient from "../utils/apiClient";
import { ENDPOINTS } from "../config/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API === "true";

const delay = (ms = MOCK_LOADING_DELAY_MS) =>
  new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Mock OCR Extraction templates for standard Indian government identity & welfare documents.
 */
const MOCK_OCR_TEMPLATES = {
  aadhaar: {
    documentName: "Aadhaar Card",
    documentType: "Aadhaar Card",
    issuer: "UIDAI (Govt of India)",
    category: "Identity Proof",
    holderName: "Rajesh Patel",
    extractedName: "Rajesh Patel",
    documentNumber: "5482-9102-4589",
    expiry: "No Expiration",
    verificationStatus: "Pending Verification",
    confidenceScore: 0.98,
    isVerified: true,
  },
  pan: {
    documentName: "PAN Card",
    documentType: "PAN Card",
    issuer: "Income Tax Department",
    category: "Identity Proof",
    holderName: "Rajesh Patel",
    extractedName: "Rajesh Patel",
    documentNumber: "BKPPS4589F",
    expiry: "No Expiration",
    verificationStatus: "Pending Verification",
    confidenceScore: 0.96,
    isVerified: true,
  },
  income_cert: {
    documentName: "Income Certificate",
    documentType: "Income Certificate",
    issuer: "Revenue Department",
    category: "Financial Proof",
    holderName: "Rajesh Patel",
    extractedName: "Rajesh Patel",
    annualIncome: 180000,
    documentNumber: "UP/INC/2026/098412",
    expiry: "2027-03-31",
    verificationStatus: "Pending Verification",
    confidenceScore: 0.94,
    isVerified: true,
  },
  caste_cert: {
    documentName: "Caste Certificate",
    documentType: "Caste Certificate",
    issuer: "Social Welfare Board",
    category: "Category Proof",
    holderName: "Rajesh Patel",
    extractedName: "Rajesh Patel",
    documentNumber: "UP/CST/2025/54129",
    expiry: "No Expiration",
    verificationStatus: "Pending Verification",
    confidenceScore: 0.95,
    isVerified: true,
  },
  ration_card: {
    documentName: "Ration Card",
    documentType: "Ration Card",
    issuer: "Department of Food & Public Distribution",
    category: "Identity Proof",
    holderName: "Rajesh Patel",
    extractedName: "Rajesh Patel",
    documentNumber: "RC-0914-8851-209",
    expiry: "No Expiration",
    verificationStatus: "Pending Verification",
    confidenceScore: 0.92,
    isVerified: true,
  },
  bank_passbook: {
    documentName: "Bank Passbook",
    documentType: "Bank Passbook",
    issuer: "State Bank of India",
    category: "Financial Proof",
    holderName: "Rajesh Patel",
    extractedName: "Rajesh Patel",
    documentNumber: "ACC: 38910482910",
    expiry: "No Expiration",
    verificationStatus: "Pending Verification",
    confidenceScore: 0.97,
    isVerified: true,
  },
  domicile_cert: {
    documentName: "Domicile Certificate",
    documentType: "Domicile Certificate",
    issuer: "District Revenue Authority",
    category: "Domicile Proof",
    holderName: "Rajesh Patel",
    extractedName: "Rajesh Patel",
    documentNumber: "UP/DOM/2024/8941",
    expiry: "No Expiration",
    verificationStatus: "Pending Verification",
    confidenceScore: 0.93,
    isVerified: true,
  },
};

/**
 * Perform AI OCR analysis on an uploaded document file or metadata.
 * Backend Integration: POST /api/v1/documents/ocr
 *
 * @param {{ file?: File, type?: string, fileName?: string, profileName?: string }} payload
 * @returns {Promise<{
 *   success: boolean,
 *   data: object,
 *   processingTimeMs: number
 * }>}
 */
export async function extractDocumentData({ file, type, fileName, profileName = "Rajesh Patel" }) {
  const startTime = Date.now();
  const nameToSearch = (file?.name || fileName || type || "").toLowerCase();

  let detectedKey = "aadhaar";
  if (nameToSearch.includes("pan")) {
    detectedKey = "pan";
  } else if (nameToSearch.includes("income") || nameToSearch.includes("salary")) {
    detectedKey = "income_cert";
  } else if (nameToSearch.includes("caste") || nameToSearch.includes("category")) {
    detectedKey = "caste_cert";
  } else if (nameToSearch.includes("ration")) {
    detectedKey = "ration_card";
  } else if (nameToSearch.includes("bank") || nameToSearch.includes("passbook")) {
    detectedKey = "bank_passbook";
  } else if (nameToSearch.includes("domicile") || nameToSearch.includes("residence")) {
    detectedKey = "domicile_cert";
  } else if (type && MOCK_OCR_TEMPLATES[type.toLowerCase()]) {
    detectedKey = type.toLowerCase();
  }

  const fallbackMock = async () => {
    await delay(1200);
    const template = MOCK_OCR_TEMPLATES[detectedKey] || {
      documentName: file?.name?.replace(/\.[^/.]+$/, "") || "Government Certificate",
      issuer: "Authorized Competent Authority",
      category: "Identity Proof",
      holderName: profileName,
      documentNumber: `DOC-${Math.floor(100000 + Math.random() * 900000)}`,
      expiry: "No Expiration",
      verificationStatus: "Pending Verification",
      confidenceScore: 0.95,
      isVerified: true,
    };

    return {
      success: true,
      data: {
        ...template,
        holderName: profileName || template.holderName,
        fileName: file?.name || fileName || `${detectedKey}_scan.pdf`,
        fileSize: file ? `${(file.size / (1024 * 1024)).toFixed(1)} MB` : "1.4 MB",
        extractedAt: new Date().toISOString(),
      },
      processingTimeMs: Date.now() - startTime,
    };
  };

  if (USE_MOCK) {
    return fallbackMock();
  }

  try {
    const formData = new FormData();
    if (file) {
      formData.append("document", file);
    }
    formData.append("documentType", type);

    const response = await apiClient.post(ENDPOINTS.DOCUMENTS.OCR || "/documents/ocr", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    return {
      success: true,
      data: response.data || response,
      processingTimeMs: Date.now() - startTime,
    };
  } catch (error) {
    console.warn("OCR API call failed, falling back to mock engine:", error);
    return fallbackMock();
  }
}

/**
 * Validate OCR extracted data against user profile data for identity consistency.
 *
 * @param {object} ocrData - Data extracted from document
 * @param {object} profileData - Citizen user profile data
 * @returns {{
 *   isMatch: boolean,
 *   matchScore: number,
 *   discrepancies: Array<{ field: string, expected: string|number, found: string|number }>
 * }}
 */
export function crossVerifyWithProfile(ocrData, profileData) {
  if (!ocrData || !profileData) {
    return { isMatch: false, matchScore: 0, discrepancies: [] };
  }

  const discrepancies = [];
  let checkedFields = 0;
  let matchedFields = 0;

  // 1. Verify Name Match
  if (ocrData.extractedName && profileData.fullName) {
    checkedFields++;
    const ocrName = ocrData.extractedName.trim().toLowerCase();
    const profName = profileData.fullName.trim().toLowerCase();

    if (ocrName === profName || ocrName.includes(profName) || profName.includes(ocrName)) {
      matchedFields++;
    } else {
      discrepancies.push({
        field: "fullName",
        expected: profileData.fullName,
        found: ocrData.extractedName,
      });
    }
  }

  // 2. Verify Category Match
  if (ocrData.category && profileData.category) {
    checkedFields++;
    if (ocrData.category.toUpperCase() === profileData.category.toUpperCase()) {
      matchedFields++;
    } else {
      discrepancies.push({
        field: "category",
        expected: profileData.category,
        found: ocrData.category,
      });
    }
  }

  // 3. Verify Income Ceiling Match
  if (ocrData.annualIncome && profileData.annualIncome) {
    checkedFields++;
    const diff = Math.abs(ocrData.annualIncome - profileData.annualIncome);
    if (diff <= 20000) {
      matchedFields++;
    } else {
      discrepancies.push({
        field: "annualIncome",
        expected: profileData.annualIncome,
        found: ocrData.annualIncome,
      });
    }
  }

  const matchScore = checkedFields > 0 ? Math.round((matchedFields / checkedFields) * 100) : 100;

  return {
    isMatch: discrepancies.length === 0,
    matchScore,
    discrepancies,
  };
}

const ocrService = {
  extractDocumentData,
  crossVerifyWithProfile,
};

export default ocrService;
