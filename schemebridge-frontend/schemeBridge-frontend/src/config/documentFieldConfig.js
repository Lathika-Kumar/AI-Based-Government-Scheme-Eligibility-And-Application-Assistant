/**
 * @file documentFieldConfig.js
 * @description Centralized document field requirements for extraction, display, and identity verification.
 * 
 * Rules:
 * - AADHAAR: Holder name, DOB/YOB, Aadhaar number, gender, address, issuing authority, expiry/validity ("No Expiration").
 * - CASTE_CERTIFICATE / COMMUNITY_CERTIFICATE: Holder name, community, certificate number, issuing authority, issue/validity dates ("No Expiration").
 * - DOMICILE_CERTIFICATE: Holder name, address, certificate number, issuing authority, issue/validity dates ("No Expiration").
 * - INCOME_CERTIFICATE: Holder name, annual income, certificate number, issuing authority, dates.
 * - BUSINESS_CERTIFICATE / OCCUPATION_CERTIFICATE: Holder name, business/occupation, registration number, issuing authority, relevant issue/validity dates.
 * - Do not force expiry date on documents that do not have one; display "No Expiration" where appropriate.
 */

export const DOCUMENT_TYPE_FIELD_CONFIG = {
  AADHAAR: {
    code: "AADHAAR",
    displayName: "Aadhaar Card",
    category: "Identity Proof",
    requiresDob: true,
    requiresDocNumber: true,
    hasExpiry: false,
    defaultExpiry: "No Expiration",
    defaultIssuer: "UIDAI (Govt of India)",
    fields: ["holderName", "dateOfBirth", "documentNumber", "gender", "address", "issuingAuthority", "expiryDate"],
    labels: {
      holderName: "Document Holder Name",
      dateOfBirth: "Date of Birth / Year of Birth",
      documentNumber: "Aadhaar Number",
      gender: "Gender",
      address: "Residential Address",
      issuingAuthority: "Issuing Authority",
      expiryDate: "Expiration / Validity"
    },
    placeholders: {
      holderName: "Extracted Aadhaar holder name",
      dateOfBirth: "DD/MM/YYYY",
      documentNumber: "XXXX-XXXX-1234",
      gender: "Male / Female / Transgender",
      address: "Address as printed on Aadhaar card",
      issuingAuthority: "UIDAI",
      expiryDate: "No Expiration"
    }
  },
  PAN: {
    code: "PAN",
    displayName: "PAN Card",
    category: "Identity Proof",
    requiresDob: true,
    requiresDocNumber: true,
    hasExpiry: false,
    defaultExpiry: "No Expiration",
    defaultIssuer: "Income Tax Department",
    fields: ["holderName", "dateOfBirth", "documentNumber", "issuingAuthority", "expiryDate"],
    labels: {
      holderName: "Cardholder Name",
      dateOfBirth: "Date of Birth",
      documentNumber: "PAN Number",
      issuingAuthority: "Issuing Authority",
      expiryDate: "Expiration / Validity"
    },
    placeholders: {
      holderName: "Extracted PAN cardholder name",
      dateOfBirth: "DD/MM/YYYY",
      documentNumber: "ABCDE1234F",
      issuingAuthority: "Income Tax Department",
      expiryDate: "No Expiration"
    }
  },
  INCOME_CERTIFICATE: {
    code: "INCOME_CERTIFICATE",
    displayName: "Income Certificate",
    category: "Financial Proof",
    requiresDob: false,
    requiresDocNumber: true,
    hasExpiry: true,
    defaultExpiry: "Valid for 1 Year",
    defaultIssuer: "Revenue Department",
    fields: ["holderName", "annualIncome", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"],
    labels: {
      holderName: "Applicant / Holder Name",
      annualIncome: "Annual Family Income (₹)",
      documentNumber: "Certificate Number",
      issuingAuthority: "Issuing Authority",
      issueDate: "Issue Date",
      expiryDate: "Validity Period / Expiry Date"
    },
    placeholders: {
      holderName: "Extracted income certificate holder name",
      annualIncome: "e.g. 120000",
      documentNumber: "e.g. TN-INC-2026-098412",
      issuingAuthority: "Tahsildar / Revenue Department",
      issueDate: "DD/MM/YYYY",
      expiryDate: "e.g. Valid for 1 Year"
    }
  },
  CASTE_CERTIFICATE: {
    code: "CASTE_CERTIFICATE",
    displayName: "Caste Certificate",
    category: "Category Proof",
    requiresDob: false,
    requiresDocNumber: true,
    hasExpiry: false,
    defaultExpiry: "No Expiration",
    defaultIssuer: "Competent Revenue Authority",
    fields: ["holderName", "community", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"],
    labels: {
      holderName: "Holder Name",
      community: "Caste / Category",
      documentNumber: "Certificate Number",
      issuingAuthority: "Issuing Authority",
      issueDate: "Issue Date",
      expiryDate: "Validity / Expiry"
    },
    placeholders: {
      holderName: "Extracted caste certificate holder name",
      community: "e.g. SC / ST / OBC / BC / MBC / General",
      documentNumber: "e.g. TN-CST-2025-54129",
      issuingAuthority: "Tahsildar / Competent Authority",
      issueDate: "DD/MM/YYYY",
      expiryDate: "No Expiration"
    }
  },
  COMMUNITY_CERTIFICATE: {
    code: "COMMUNITY_CERTIFICATE",
    displayName: "Community Certificate",
    category: "Category Proof",
    requiresDob: false,
    requiresDocNumber: true,
    hasExpiry: false,
    defaultExpiry: "No Expiration",
    defaultIssuer: "Competent Revenue Authority",
    fields: ["holderName", "community", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"],
    labels: {
      holderName: "Holder Name",
      community: "Community / Caste",
      documentNumber: "Certificate Number",
      issuingAuthority: "Issuing Authority",
      issueDate: "Issue Date",
      expiryDate: "Validity / Expiry"
    },
    placeholders: {
      holderName: "Extracted community certificate holder name",
      community: "e.g. BC / MBC / SC / ST / General",
      documentNumber: "e.g. TN-COM-2025-01948",
      issuingAuthority: "Tahsildar / Revenue Authority",
      issueDate: "DD/MM/YYYY",
      expiryDate: "No Expiration"
    }
  },
  DOMICILE_CERTIFICATE: {
    code: "DOMICILE_CERTIFICATE",
    displayName: "Domicile / Residence Certificate",
    category: "Domicile Proof",
    requiresDob: false,
    requiresDocNumber: true,
    hasExpiry: false,
    defaultExpiry: "No Expiration",
    defaultIssuer: "District Revenue Authority",
    fields: ["holderName", "address", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"],
    labels: {
      holderName: "Resident / Holder Name",
      address: "Residential Address",
      documentNumber: "Certificate Number",
      issuingAuthority: "Issuing Authority",
      issueDate: "Issue Date",
      expiryDate: "Validity / Expiry"
    },
    placeholders: {
      holderName: "Extracted domicile holder name",
      address: "e.g. Town/Village, Taluk, District",
      documentNumber: "e.g. TN-DOM-2024-8941",
      issuingAuthority: "Tahsildar / Revenue Department",
      issueDate: "DD/MM/YYYY",
      expiryDate: "No Expiration"
    }
  },
  BUSINESS_CERTIFICATE: {
    code: "BUSINESS_CERTIFICATE",
    displayName: "Business / Occupation Certificate",
    category: "Property Proof",
    requiresDob: false,
    requiresDocNumber: true,
    hasExpiry: true,
    defaultExpiry: "No Expiration",
    defaultIssuer: "Ministry of MSME / Municipal Authority",
    fields: ["holderName", "businessOccupation", "documentNumber", "issuingAuthority", "issueDate", "expiryDate"],
    labels: {
      holderName: "Proprietor / Holder Name",
      businessOccupation: "Business / Occupation",
      documentNumber: "Registration / License Number",
      issuingAuthority: "Issuing Authority",
      issueDate: "Registration / Issue Date",
      expiryDate: "Validity / Expiry Date"
    },
    placeholders: {
      holderName: "Extracted business proprietor name",
      businessOccupation: "e.g. Retail Trade, Agro Services, Manufacturing",
      documentNumber: "e.g. UDYAM-TN-01-0012345",
      issuingAuthority: "Ministry of MSME / Municipal Corporation",
      issueDate: "DD/MM/YYYY",
      expiryDate: "No Expiration"
    }
  },
  RATION_CARD: {
    code: "RATION_CARD",
    displayName: "Ration Card",
    category: "Identity Proof",
    requiresDob: false,
    requiresDocNumber: true,
    hasExpiry: false,
    defaultExpiry: "No Expiration",
    defaultIssuer: "Department of Food & Civil Supplies",
    fields: ["holderName", "address", "documentNumber", "issuingAuthority", "expiryDate"],
    labels: {
      holderName: "Head of Family / Holder Name",
      address: "Family Residential Address",
      documentNumber: "Ration Card Number",
      issuingAuthority: "Issuing Authority",
      expiryDate: "Validity / Expiry"
    },
    placeholders: {
      holderName: "Extracted family head name",
      address: "Address as printed on Ration card",
      documentNumber: "RC-XXXX-XXXX",
      issuingAuthority: "Food & Civil Supplies Dept",
      expiryDate: "No Expiration"
    }
  },
  BANK_PASSBOOK: {
    code: "BANK_PASSBOOK",
    displayName: "Bank Passbook",
    category: "Financial Proof",
    requiresDob: false,
    requiresDocNumber: true,
    hasExpiry: false,
    defaultExpiry: "No Expiration",
    defaultIssuer: "Bank Authority",
    fields: ["holderName", "documentNumber", "issuingAuthority", "expiryDate"],
    labels: {
      holderName: "Account Holder Name",
      documentNumber: "Bank Account Number",
      issuingAuthority: "Bank / Branch Name",
      expiryDate: "Validity / Expiry"
    },
    placeholders: {
      holderName: "Extracted account holder name",
      documentNumber: "Bank Account / IFSC Number",
      issuingAuthority: "e.g. State Bank of India",
      expiryDate: "No Expiration"
    }
  },
  DEFAULT: {
    code: "DEFAULT",
    displayName: "Government Document",
    category: "Identity Proof",
    requiresDob: false,
    requiresDocNumber: true,
    hasExpiry: false,
    defaultExpiry: "No Expiration",
    defaultIssuer: "Authorized Authority",
    fields: ["holderName", "documentNumber", "issuingAuthority", "expiryDate"],
    labels: {
      holderName: "Holder Name",
      documentNumber: "Document Number",
      issuingAuthority: "Issuing Authority",
      expiryDate: "Validity / Expiry"
    },
    placeholders: {
      holderName: "Holder Name as shown on document",
      documentNumber: "Document / Certificate Number",
      issuingAuthority: "Authorized Competent Authority",
      expiryDate: "No Expiration"
    }
  }
};

/**
 * Identify the canonical field configuration given a document name, type string, or category.
 * @param {string} typeOrName
 * @returns {typeof DOCUMENT_TYPE_FIELD_CONFIG.AADHAAR}
 */
export function getDocumentFieldConfig(typeOrName) {
  if (!typeOrName) return DOCUMENT_TYPE_FIELD_CONFIG.DEFAULT;
  const upper = String(typeOrName).toUpperCase().trim();

  if (upper.includes("AADHAAR") || upper.includes("AADHAR") || upper.includes("UIDAI")) {
    return DOCUMENT_TYPE_FIELD_CONFIG.AADHAAR;
  }
  if (upper.includes("PAN") || upper.includes("PERMANENT ACCOUNT")) {
    return DOCUMENT_TYPE_FIELD_CONFIG.PAN;
  }
  if (upper.includes("INCOME") || upper.includes("SALARY") || upper.includes("आय प्रमाण") || upper.includes("வருமானம்")) {
    return DOCUMENT_TYPE_FIELD_CONFIG.INCOME_CERTIFICATE;
  }
  if (upper.includes("COMMUNITY") || upper.includes("சமூகச் சான்றிதழ்")) {
    return DOCUMENT_TYPE_FIELD_CONFIG.COMMUNITY_CERTIFICATE;
  }
  if (upper.includes("CASTE") || upper.includes("जाति प्रमाण") || upper.includes("சாதிச் சான்றிதழ்")) {
    return DOCUMENT_TYPE_FIELD_CONFIG.CASTE_CERTIFICATE;
  }
  if (upper.includes("DOMICILE") || upper.includes("RESIDENCE") || upper.includes("मूल निवास") || upper.includes("இருப்பிடம்") || upper.includes("NATIVITY")) {
    return DOCUMENT_TYPE_FIELD_CONFIG.DOMICILE_CERTIFICATE;
  }
  if (
    upper.includes("BUSINESS") ||
    upper.includes("OCCUPATION") ||
    upper.includes("UDYAM") ||
    upper.includes("MSME") ||
    upper.includes("TRADE LICENSE") ||
    upper.includes("SHOP") ||
    upper.includes("COMMERCIAL") ||
    upper.includes("GST")
  ) {
    return DOCUMENT_TYPE_FIELD_CONFIG.BUSINESS_CERTIFICATE;
  }
  if (upper.includes("RATION") || upper.includes("राशन कार्ड") || upper.includes("குடும்ப அட்டை")) {
    return DOCUMENT_TYPE_FIELD_CONFIG.RATION_CARD;
  }
  if (upper.includes("BANK") || upper.includes("PASSBOOK") || upper.includes("बैंक पासबुक") || upper.includes("வங்கி")) {
    return DOCUMENT_TYPE_FIELD_CONFIG.BANK_PASSBOOK;
  }

  return DOCUMENT_TYPE_FIELD_CONFIG.DEFAULT;
}
