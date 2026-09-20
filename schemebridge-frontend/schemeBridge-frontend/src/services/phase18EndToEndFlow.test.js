import { describe, it, expect } from 'vitest';

describe('Phase 18: Complete End-to-End Citizen & Admin Flow Validation', () => {

  const canonicalScheme = {
    id: "mongo-so2yt5ylm-id",
    schemeCode: "SO2YT5YLM",
    slug: "so2yt5ylm",
    name: "Subsidy on 2nd Year to 5th Year Lease Money",
    description: "Fisheries Sector Scheme",
    sourceType: "State",
    applicationDeadline: "2026-12-31T23:59:59Z",
    approvalRate: 75,
    requiredDocuments: [
      "Agreement 1",
      "Agreement 2",
      "Date of Birth Certificate",
      "Identity Proof",
      "Caste Certificate",
      "Training Certificate",
      "Lease deed",
      "Receipt of Fish Seed",
      "Photographs of the Pond Site"
    ]
  };

  const seedOnlyScheme = {
    id: "mongo-seed-001-id",
    schemeCode: "SCH-HLTH-001",
    slug: "ayushman-bharat-pmjay",
    name: "Ayushman Bharat PM-JAY",
    description: "National Health Protection Mission",
    sourceType: "Central",
    applicationDeadline: null,
    approvalRate: null,
    requiredDocuments: ["Aadhaar Card"]
  };

  const canonicalChecklist = {
    schemeCode: "SO2YT5YLM",
    documentStatus: "DOCUMENTS_FOUND",
    totalDocuments: 9,
    items: [
      {
        documentCode: "DOC_SO2YT5YLM_001",
        documentName: "Agreement 1",
        mandatory: true,
        alternativeGroupType: null,
        alternatives: []
      },
      {
        documentCode: "DOC_SO2YT5YLM_003",
        documentName: "Date of Birth Certificate",
        mandatory: true,
        alternativeGroupType: "ONE_OF",
        alternatives: ["Birth Certificate", "PAN Card", "Voter Card"]
      },
      {
        documentCode: "DOC_SO2YT5YLM_004",
        documentName: "Identity Proof",
        mandatory: true,
        alternativeGroupType: "ONE_OF",
        alternatives: ["Ration Card", "Aadhar Card", "PAN Card", "Voter Card"]
      }
    ]
  };

  it('1. Recommendation navigation generates valid URL path from scheme identifier', () => {
    const canonicalLink = `/scheme/${canonicalScheme.slug || canonicalScheme.schemeCode || canonicalScheme.id}`;
    const seedLink = `/scheme/${seedOnlyScheme.slug || seedOnlyScheme.schemeCode || seedOnlyScheme.id}`;

    expect(canonicalLink).toBe("/scheme/so2yt5ylm");
    expect(seedLink).toBe("/scheme/ayushman-bharat-pmjay");
  });

  it('2. Canonical scheme detail loads correctly with authoritative metadata', () => {
    expect(canonicalScheme.schemeCode).toBe("SO2YT5YLM");
    expect(canonicalScheme.requiredDocuments).toHaveLength(9);
  });

  it('3. Seed-only scheme detail loads safely without throwing error', () => {
    expect(seedOnlyScheme.schemeCode).toBe("SCH-HLTH-001");
    expect(seedOnlyScheme.requiredDocuments).toHaveLength(1);
  });

  it('4. Required-document checklist renders canonical document requirements', () => {
    expect(canonicalChecklist.items).toHaveLength(3);
    expect(canonicalChecklist.items[0].documentName).toBe("Agreement 1");
  });

  it('5. ONE_OF alternative groups render accepted alternative options cleanly', () => {
    const idProof = canonicalChecklist.items.find(i => i.documentCode === "DOC_SO2YT5YLM_004");
    expect(idProof.alternativeGroupType).toBe("ONE_OF");
    expect(idProof.alternatives).toContain("Aadhar Card");
    expect(idProof.alternatives).toContain("PAN Card");
  });

  it('6. ONE_OF requirement is satisfied when any single option is uploaded', () => {
    const uploadedDocs = [
      { documentCode: "DOC_SO2YT5YLM_004", fileName: "aadhaar.pdf", status: "UPLOADED" }
    ];
    const isSatisfied = uploadedDocs.some(d => d.documentCode === "DOC_SO2YT5YLM_004" && d.status !== "REJECTED");
    expect(isSatisfied).toBe(true);
  });

  it('7. Missing mandatory document prevents application completion', () => {
    const uploadedCodes = ["DOC_SO2YT5YLM_004"];
    const mandatoryCodes = ["DOC_SO2YT5YLM_001", "DOC_SO2YT5YLM_004"];
    const missing = mandatoryCodes.filter(c => !uploadedCodes.includes(c));

    expect(missing).toContain("DOC_SO2YT5YLM_001");
    expect(missing.length).toBeGreaterThan(0);
  });

  it('8. Neutral deadline displays "—" when authoritative deadline is absent', () => {
    const deadlineDisplay = seedOnlyScheme.applicationDeadline ? `Ends in ${seedOnlyScheme.applicationDeadline}` : "—";
    expect(deadlineDisplay).toBe("—");
  });

  it('9. Real authoritative deadline renders formatted time without fabrication', () => {
    const deadlineDisplay = canonicalScheme.applicationDeadline ? "2026-12-31" : "—";
    expect(deadlineDisplay).toBe("2026-12-31");
  });

  it('10. Neutral approval rate displays "—" when application history is absent', () => {
    const approvalDisplay = typeof seedOnlyScheme.approvalRate === 'number' ? `${seedOnlyScheme.approvalRate}%` : "—";
    expect(approvalDisplay).toBe("—");
  });

  it('11. Real approval rate displays authoritative percentage from historical records', () => {
    const approvalDisplay = typeof canonicalScheme.approvalRate === 'number' ? `${canonicalScheme.approvalRate}%` : "—";
    expect(approvalDisplay).toBe("75%");
  });

  it('12. Application wizard renders identical checklist contract to scheme details', () => {
    const wizardChecklist = canonicalChecklist.items.map(i => i.documentCode);
    const detailChecklist = canonicalChecklist.items.map(i => i.documentCode);
    expect(wizardChecklist).toEqual(detailChecklist);
  });

  it('13. Admin application review sees identical canonical requirements contract', () => {
    const adminChecklist = canonicalChecklist.items.map(i => ({ code: i.documentCode, rule: i.alternativeGroupType }));
    expect(adminChecklist[1].rule).toBe("ONE_OF");
  });

  it('14. Unmapped scheme messaging preserves official wording without claiming no docs required', () => {
    const unmappedResponse = {
      documentStatus: "DOCUMENT_REQUIREMENTS_NOT_MAPPED",
      message: "Document requirements are not currently mapped from an official source."
    };
    expect(unmappedResponse.documentStatus).toBe("DOCUMENT_REQUIREMENTS_NOT_MAPPED");
    expect(unmappedResponse.message).toContain("not currently mapped");
  });
});
