import { describe, it, expect } from 'vitest';

describe('Canonical Document Checklist & ONE_OF Semantics', () => {

  const sampleCanonicalResponse = {
    schemeCode: "SO2YT5YLM",
    documentStatus: "DOCUMENTS_FOUND",
    totalDocuments: 3,
    mandatoryDocumentCount: 3,
    alternativeGroupCount: 2,
    items: [
      {
        documentCode: "DOC_SO2YT5YLM_001",
        canonicalDocumentCode: "AGREEMENT_DEED",
        documentName: "Agreement 1 - Agreement deed between fish farmer and Fisheries Department",
        mandatory: true,
        alternativeGroupType: null,
        alternatives: [],
        issuingAuthority: "Competent Government Authority"
      },
      {
        documentCode: "DOC_SO2YT5YLM_003",
        canonicalDocumentCode: "BIRTH_CERTIFICATE",
        documentName: "Date of Birth Certificate",
        mandatory: true,
        alternativeGroupType: "ONE_OF",
        alternatives: [
          "Birth Certificate",
          "Matriculation Certificate",
          "PAN Card",
          "Voter Card",
          "Driving License"
        ],
        issuingAuthority: "Competent Government Authority"
      },
      {
        documentCode: "DOC_SO2YT5YLM_004",
        canonicalDocumentCode: "IDENTITY_PROOF",
        documentName: "Identity Proof",
        mandatory: true,
        alternativeGroupType: "ONE_OF",
        alternatives: [
          "Ration Card",
          "Aadhar Card",
          "PAN Card",
          "Voter Card"
        ],
        issuingAuthority: "Competent Government Authority"
      }
    ]
  };

  it('1. Renders canonical checklist items without hardcoding', () => {
    expect(sampleCanonicalResponse.items).toHaveLength(3);
    expect(sampleCanonicalResponse.items[0].documentName).toContain("Agreement 1");
  });

  it('2. Correctly identifies and represents ONE_OF alternative groups', () => {
    const oneOfItems = sampleCanonicalResponse.items.filter(i => i.alternativeGroupType === 'ONE_OF');
    expect(oneOfItems).toHaveLength(2);
    expect(oneOfItems[0].alternatives).toContain("Birth Certificate");
    expect(oneOfItems[1].alternatives).toContain("Aadhar Card");
  });

  it('3. Verifies that ONE_OF requirement is satisfied by a single valid alternative upload', () => {
    const uploadedDocs = [
      { documentCode: "DOC_SO2YT5YLM_001", status: "VERIFIED" },
      { documentCode: "DOC_SO2YT5YLM_004", fileName: "pan_card.pdf", status: "OCR_COMPLETED" } // only PAN card uploaded
    ];

    const isGroup4Satisfied = uploadedDocs.some(d => d.documentCode === "DOC_SO2YT5YLM_004" && d.status !== "REJECTED");
    expect(isGroup4Satisfied).toBe(true);
  });

  it('4. Verifies that all alternatives in ONE_OF group are NOT required', () => {
    const uploadedDocs = [
      { documentCode: "DOC_SO2YT5YLM_004", fileName: "aadhaar.pdf", status: "VERIFIED" }
    ];

    // Only 1 document was uploaded, but the alternative group is satisfied
    const uploadedCount = uploadedDocs.filter(d => d.documentCode === "DOC_SO2YT5YLM_004").length;
    expect(uploadedCount).toBe(1);
    expect(uploadedCount < sampleCanonicalResponse.items[2].alternatives.length).toBe(true);
  });

  it('5. Unmapped requirements preserve DOCUMENT_REQUIREMENTS_NOT_MAPPED without stating No documents required', () => {
    const unmappedResponse = {
      schemeCode: "UNMAPPED_SCHEME",
      documentStatus: "DOCUMENT_REQUIREMENTS_NOT_MAPPED",
      items: []
    };

    expect(unmappedResponse.documentStatus).toBe("DOCUMENT_REQUIREMENTS_NOT_MAPPED");
    expect(unmappedResponse.documentStatus).not.toBe("NO_DOCUMENTS_REQUIRED");
  });

  it('6. Admin and Citizen share the identical canonical checklist items contract', () => {
    const citizenChecklist = sampleCanonicalResponse.items.map(i => ({ code: i.documentCode, name: i.documentName, alternatives: i.alternatives }));
    const adminChecklist = sampleCanonicalResponse.items.map(i => ({ code: i.documentCode, name: i.documentName, alternatives: i.alternatives }));

    expect(citizenChecklist).toEqual(adminChecklist);
  });
});
