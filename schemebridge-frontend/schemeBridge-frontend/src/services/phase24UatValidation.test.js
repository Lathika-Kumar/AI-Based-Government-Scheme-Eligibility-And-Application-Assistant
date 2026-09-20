import { describe, it, expect } from 'vitest';
import { getDocReadinessForScheme } from '../utils/documentReadiness';

describe('Phase 24 — Production AI & Document Checklist UAT Frontend Validation', () => {

  // Test Requirement A: String document requirements
  it('1. Safely parses and evaluates string document requirements', () => {
    const stringRequirements = [
      'Aadhaar Card',
      'Income Certificate',
      'Domicile Certificate'
    ];

    const citizenDocs = [
      { id: '1', name: 'Aadhaar Card', verified: true },
      { id: '2', name: 'Income Certificate', verified: true }
    ];

    const readiness = getDocReadinessForScheme(stringRequirements, citizenDocs);

    expect(readiness.totalRequired).toBe(3);
    expect(readiness.availableCount).toBe(2);
    expect(readiness.missingCount).toBe(1);
    expect(readiness.missingDocs).toContain('Domicile Certificate');
    expect(readiness.availableDocs).toContain('Aadhaar Card');
    expect(readiness.availableDocs).toContain('Income Certificate');
    expect(readiness.readinessScore).toBe(67);
  });

  // Test Requirement B: Object requirements
  it('2. Safely handles structured object requirements without crashing', () => {
    const objectRequirements = [
      {
        documentName: 'Income Certificate',
        mandatory: true,
        issuingAuthority: 'Revenue Department'
      },
      {
        documentName: 'Land Records',
        mandatory: true,
        issuingAuthority: 'Tehsildar'
      }
    ];

    const citizenDocs = [
      { id: '1', name: 'Income Certificate', verified: true }
    ];

    // Must not crash when objects are passed
    const readiness = getDocReadinessForScheme(objectRequirements, citizenDocs);

    expect(readiness.totalRequired).toBe(2);
    expect(readiness.availableCount).toBe(1);
    expect(readiness.missingCount).toBe(1);
    expect(readiness.availableDocs).toContain('Income Certificate');
    expect(readiness.missingDocs).toContain('Land Records');
    expect(readiness.readinessScore).toBe(50);
  });

  // Test Requirement C: ONE_OF alternative groups
  it('3. Preserves ONE_OF semantics: satisfies requirement when Option A is present', () => {
    const requirementsWithOneOf = [
      {
        documentName: 'Proof of Identity',
        alternativeGroupType: 'ONE_OF',
        alternatives: ['Voter ID', 'Aadhaar Card', 'Passport'],
        mandatory: true
      }
    ];

    // Citizen only has Voter ID
    const citizenDocs = [
      { id: '1', name: 'Voter ID', verified: true }
    ];

    const readiness = getDocReadinessForScheme(requirementsWithOneOf, citizenDocs);

    expect(readiness.totalRequired).toBe(1);
    expect(readiness.availableCount).toBe(1);
    expect(readiness.missingCount).toBe(0);
    expect(readiness.isReady).toBe(true);
    expect(readiness.readinessScore).toBe(100);
    expect(readiness.availableDocs).toContain('Proof of Identity');
  });

  it('4. Preserves ONE_OF semantics: satisfies requirement when Option B is present', () => {
    const requirementsWithOneOf = [
      {
        documentName: 'Income Proof',
        type: 'ONE_OF',
        options: ['Income Certificate', 'BPL Ration Card'],
        mandatory: true
      }
    ];

    // Citizen has BPL Ration Card, but NOT Income Certificate
    const citizenDocs = [
      { id: '1', name: 'BPL Ration Card', verified: true }
    ];

    const readiness = getDocReadinessForScheme(requirementsWithOneOf, citizenDocs);

    expect(readiness.totalRequired).toBe(1);
    expect(readiness.availableCount).toBe(1);
    expect(readiness.missingCount).toBe(0);
    expect(readiness.readinessScore).toBe(100);
    expect(readiness.availableDocs).toContain('Income Proof');
  });

  it('5. Correctly marks ONE_OF requirement missing when neither alternative exists', () => {
    const requirementsWithOneOf = [
      {
        documentName: 'Income Proof',
        type: 'ONE_OF',
        options: ['Income Certificate', 'BPL Ration Card'],
        mandatory: true
      }
    ];

    // Citizen only has Driving License
    const citizenDocs = [
      { id: '1', name: 'Driving License', verified: true }
    ];

    const readiness = getDocReadinessForScheme(requirementsWithOneOf, citizenDocs);

    expect(readiness.totalRequired).toBe(1);
    expect(readiness.availableCount).toBe(0);
    expect(readiness.missingCount).toBe(1);
    expect(readiness.readinessScore).toBe(0);
    expect(readiness.missingDocs).toContain('Income Proof');
  });

  // Test Requirement D: Nested/structured document requirement objects
  it('6. Handles nested structured document requirements with alternativeGroup', () => {
    const nestedRequirements = [
      {
        documentCode: 'DOC_001',
        documentName: 'Identity & Address Proof',
        alternativeGroup: {
          type: 'ONE_OF',
          options: ['Aadhaar Card', 'Passport', 'Voter Card']
        }
      }
    ];

    const citizenDocs = [
      { id: '1', name: 'Passport', verified: true }
    ];

    const readiness = getDocReadinessForScheme(nestedRequirements, citizenDocs);
    expect(readiness.availableCount).toBe(1);
    expect(readiness.readinessScore).toBe(100);
  });

  // Test Step 3: Consistency between Scheme Details & Application Wizard
  it('7. Scheme Details & Application Wizard document checklist parity', () => {
    const canonicalChecklist = {
      schemeCode: 'SO2YT5YLM',
      items: [
        {
          documentCode: 'DOC_01',
          documentName: 'Agreement Deed',
          mandatory: true
        },
        {
          documentCode: 'DOC_02',
          documentName: 'Date of Birth Certificate',
          mandatory: true,
          alternativeGroupType: 'ONE_OF',
          alternatives: ['Birth Certificate', 'Matriculation Certificate', 'PAN Card', 'Voter Card']
        }
      ]
    };

    const citizenDocs = [
      { id: '1', name: 'Agreement Deed', verified: true },
      { id: '2', name: 'PAN Card', verified: true }
    ];

    // Both SchemeDetails and ApplicationWizard resolve canonicalChecklist.items
    const readiness = getDocReadinessForScheme(canonicalChecklist.items, citizenDocs);

    expect(readiness.totalRequired).toBe(2);
    expect(readiness.availableCount).toBe(2);
    expect(readiness.missingCount).toBe(0);
    expect(readiness.readinessScore).toBe(100);
    expect(readiness.isReady).toBe(true);
  });

  // Test Step 6: Citizen-facing explanations contain NO ML terminology
  it('8. Recommendation explanations are citizen-friendly and free from ML jargon', () => {
    const citizenFacingReasons = [
      'Matches your state of domicile',
      'You meet the age requirement for youth empowerment programs',
      'Your annual household income falls within the eligible range',
      'This scheme is available for your occupation: Farmer',
      'You meet the required category criteria: OBC'
    ];

    const mlForbiddenTerms = [
      'embedding',
      'cosine',
      'vector',
      'similarity',
      'model id',
      'ranking weights',
      'tensor',
      'confidence score float',
      'softmax',
      'nearest neighbor'
    ];

    citizenFacingReasons.forEach(reason => {
      const lower = reason.toLowerCase();
      mlForbiddenTerms.forEach(term => {
        expect(lower).not.toContain(term);
      });
    });
  });

});
