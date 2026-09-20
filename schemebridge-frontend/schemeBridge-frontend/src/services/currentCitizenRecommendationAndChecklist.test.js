import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getPersonalizedRecommendations, getRecommendations, getSchemeChecklist } from './schemeService';
import { getSchemeDocumentChecklist } from './applicationService';
import { schemeApi } from '@utils/apiClient';

vi.mock('@utils/apiClient', () => ({
  schemeApi: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('Current Citizen Recommendation & Scheme-Specific Checklist Service Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('A. Cold Start: Newly authenticated citizen receives recommendations on first login without historical users', async () => {
    const mockPersonalizedResponse = {
      data: {
        userId: 'citizen_new_login_001',
        citizenState: 'MAHARASHTRA',
        totalCatalogEvaluated: 15,
        eligibleCandidatesFound: 2,
        totalRecommendationsReturned: 2,
        page: 0,
        size: 50,
        recommendations: [
          {
            rank: 1,
            schemeCode: 'SCH-FARMER-001',
            schemeTitle: 'Kisan Input Assistance Scheme',
            shortDescription: 'Financial support for active farmers',
            eligibilityStatus: 'ELIGIBLE',
            recommendationScore: 0.92,
            requiredDocuments: ['Aadhaar Card', 'Land 7/12 Extract', 'Bank Passbook'],
            checklist: [
              {
                documentCode: 'AADHAAR',
                documentName: 'Aadhaar Card',
                mandatory: true,
                required: true,
                issuingAuthority: 'UIDAI',
                acceptedFormats: ['PDF', 'JPG'],
              },
              {
                documentCode: 'LAND_RECORDS',
                documentName: 'Land 7/12 Extract',
                mandatory: true,
                required: true,
                issuingAuthority: 'Revenue Department',
                acceptedFormats: ['PDF'],
              }
            ],
            applicationSteps: [
              'Navigate to official PM-Kisan portal.',
              'Submit Aadhaar number and land khata details.',
              'Review bank account for Direct Benefit Transfer.'
            ],
            applicationUrl: 'https://pmkisan.gov.in',
            benefits: ['Annual assistance of ₹6,000 via DBT'],
            reasons: ['Statutory eligibility confirmed by EligibilityEngine; matches occupation FARMER']
          }
        ]
      }
    };

    schemeApi.get.mockResolvedValueOnce(mockPersonalizedResponse);

    const result = await getPersonalizedRecommendations({ page: 0, size: 50 });

    expect(schemeApi.get).toHaveBeenCalledWith('/api/recommendations', {
      params: { page: 0, size: 50 }
    });
    expect(result.data.userId).toBe('citizen_new_login_001');
    expect(result.data.eligibleCandidatesFound).toBe(2);
    expect(result.data.recommendations[0].eligibilityStatus).toBe('ELIGIBLE');
    expect(result.data.recommendations[0].checklist).toHaveLength(2);
    expect(result.data.recommendations[0].applicationSteps).toHaveLength(3);
    expect(result.data.recommendations[0].applicationUrl).toBe('https://pmkisan.gov.in');
  });

  it('B. Zero fake defaults: profile payload retains only authentic citizen attributes', async () => {
    // When a citizen has only partial information (e.g., age 28, state Tamil Nadu, but no occupation or income entered)
    // The client MUST NOT inject fake defaults like "30", "MALE", "180000", "FARMER", "GUJARAT", "OBC"
    const authenticPartialProfile = {
      age: 28,
      state: 'TAMIL_NADU',
    };

    schemeApi.post.mockResolvedValueOnce({
      data: {
        totalSchemesEvaluated: 12,
        eligibleCount: 1,
        recommendations: [
          {
            schemeCode: 'SCH-TN-001',
            schemeTitle: 'Tamil Nadu Youth Welfare Scheme',
            eligibilityStatus: 'ELIGIBLE',
            recommendationScore: 0.85,
          }
        ]
      }
    });

    await getRecommendations(authenticPartialProfile, { page: 0, size: 10 });

    expect(schemeApi.post).toHaveBeenCalledWith(
      '/api/schemes/recommendations',
      authenticPartialProfile,
      { params: { page: 0, size: 10 } }
    );

    const passedProfile = schemeApi.post.mock.calls[0][1];
    expect(passedProfile.age).toBe(28);
    expect(passedProfile.state).toBe('TAMIL_NADU');
    expect(passedProfile.occupation).toBeUndefined();
    expect(passedProfile.annualIncome).toBeUndefined();
    expect(passedProfile.gender).toBeUndefined();
    expect(passedProfile.socialCategory).toBeUndefined();
  });

  it('C. Scheme-specific checklist: returns canonical documents, issuing authorities, and alternatives', async () => {
    const mockChecklistResponse = {
      data: {
        schemeCode: 'SCH-FARMER-001',
        schemeTitle: 'Kisan Input Assistance Scheme',
        documentStatus: 'DOCUMENTS_FOUND',
        overallProvenance: 'VERIFIED_OFFICIAL',
        totalDocuments: 2,
        totalRequired: 2,
        items: [
          {
            documentCode: 'AADHAAR',
            documentName: 'Aadhaar Card',
            mandatory: true,
            required: true,
            issuingAuthority: 'UIDAI',
            acceptedFormats: ['PDF', 'JPG', 'PNG'],
            whyRequired: 'Statutory identity authentication for Direct Benefit Transfer.'
          },
          {
            documentCode: 'LAND_DOC',
            documentName: 'Land Ownership Record',
            mandatory: true,
            required: true,
            alternativeGroupType: 'ONE_OF',
            alternatives: ['7/12 Extract', 'Khasra/Khatauni', 'Pattadar Passbook'],
            issuingAuthority: 'Revenue Department'
          }
        ],
        applicationSteps: [
          'Visit official government portal.',
          'Authenticate via Aadhaar OTP.',
          'Submit land record details.'
        ],
        officialApplicationUrl: 'https://pmkisan.gov.in/RegistrationFormNew.aspx',
        helplineNumber: '155261'
      }
    };

    schemeApi.get.mockResolvedValueOnce(mockChecklistResponse);

    const res = await getSchemeChecklist('SCH-FARMER-001');

    expect(schemeApi.get).toHaveBeenCalledWith('/api/schemes/SCH-FARMER-001/checklist');
    expect(res.data.schemeCode).toBe('SCH-FARMER-001');
    expect(res.data.items).toHaveLength(2);
    expect(res.data.items[1].alternativeGroupType).toBe('ONE_OF');
    expect(res.data.items[1].alternatives).toContain('7/12 Extract');
    expect(res.data.officialApplicationUrl).toContain('pmkisan.gov.in');
    expect(res.data.helplineNumber).toBe('155261');
  });

  it('D. Backward-compatible document checklist endpoint: getSchemeDocumentChecklist calls /api/schemes/:code/document-checklist', async () => {
    schemeApi.get.mockResolvedValueOnce({
      data: { schemeCode: 'SCH-001', items: [] }
    });

    await getSchemeDocumentChecklist('SCH-001');

    expect(schemeApi.get).toHaveBeenCalledWith('/api/schemes/SCH-001/document-checklist');
  });

  it('E. Empty eligible result: handles 0 eligible schemes safely without crashing', async () => {
    const mockEmptyResponse = {
      data: {
        userId: 'citizen_ineligible_user',
        totalCatalogEvaluated: 25,
        eligibleCandidatesFound: 0,
        totalRecommendationsReturned: 0,
        recommendations: []
      }
    };

    schemeApi.get.mockResolvedValueOnce(mockEmptyResponse);

    const res = await getPersonalizedRecommendations({ page: 0, size: 50 });

    expect(res.data.eligibleCandidatesFound).toBe(0);
    expect(res.data.recommendations).toEqual([]);
  });

  it('F. Service failure handled safely: returns error object for graceful frontend error banner', async () => {
    schemeApi.get.mockRejectedValueOnce(new Error('Network connectivity issue'));

    await expect(getPersonalizedRecommendations({ page: 0, size: 50 })).rejects.toThrow('Network connectivity issue');
  });
});
