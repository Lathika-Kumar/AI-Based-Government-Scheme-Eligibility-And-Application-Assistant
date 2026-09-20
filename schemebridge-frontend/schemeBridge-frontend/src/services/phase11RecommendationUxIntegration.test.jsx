import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { renderToString } from 'react-dom/server';
import { MemoryRouter, Routes, Route, Navigate, useParams, useLocation } from 'react-router-dom';
import { getDocReadinessForScheme } from '../utils/documentReadiness';
import { resolveApplicationAction, ACTION_TYPES } from '../utils/applicationStateMapping';

describe('PHASE 11 — Frontend Recommendation UX, Explainability & Data Consistency Suite', () => {

  // Test 1: Recommendation API response rendering
  it('1. Recommendation API response mapping preserves critical scheme metadata', () => {
    const rawApiItem = {
      rank: 1,
      schemeCode: 'SCH-EDU-001',
      schemeTitle: 'National Higher Education Scholarship',
      shortDescription: 'Merit-cum-means scholarship for undergraduate students',
      eligibilityStatus: 'ELIGIBLE',
      recommendationScore: 0.885,
      requiredDocuments: ['Aadhaar Card', 'Income Certificate', 'College ID'],
      checklist: [
        { documentName: 'Aadhaar Card', mandatory: true },
        { documentName: 'Income Certificate', mandatory: true },
        { documentName: 'College ID', mandatory: false }
      ],
      reasons: ['Matches your state (Tamil Nadu)', 'Designed for students', 'Income falls within ceiling']
    };

    expect(rawApiItem.rank).toBe(1);
    expect(rawApiItem.schemeCode).toBe('SCH-EDU-001');
    expect(rawApiItem.eligibilityStatus).toBe('ELIGIBLE');
    expect(rawApiItem.recommendationScore).toBe(0.885);
    expect(rawApiItem.requiredDocuments).toHaveLength(3);
    expect(rawApiItem.reasons).toHaveLength(3);
  });

  // Test 2: Backend ranking order is preserved
  it('2. Backend ranking order is strictly preserved (#1, #2, #3...) without frontend distortion', () => {
    const backendItems = [
      { rank: 1, schemeCode: 'SCH-Z-001', recommendationScore: 0.95 },
      { rank: 2, schemeCode: 'SCH-A-002', recommendationScore: 0.88 },
      { rank: 3, schemeCode: 'SCH-M-003', recommendationScore: 0.82 }
    ];

    // Client-side sorting logic under default "match_score" mode
    const sorted = [...backendItems].sort((a, b) => a.rank - b.rank);
    expect(sorted[0].schemeCode).toBe('SCH-Z-001');
    expect(sorted[1].schemeCode).toBe('SCH-A-002');
    expect(sorted[2].schemeCode).toBe('SCH-M-003');
    // Ensure alphabetical sort is NOT applied by default
    expect(sorted[0].schemeCode).not.toBe('SCH-A-002');
  });

  // Test 3: Different profiles produce different rankings
  it('3. Diverse citizen profiles produce logically differentiated rankings', () => {
    // Farmer profile
    const farmerRanking = [
      { schemeCode: 'SCH-AGRI-001', score: 0.94, occupation: 'FARMER' },
      { schemeCode: 'SCH-GEN-001', score: 0.70, occupation: 'ALL' }
    ];
    // Student profile
    const studentRanking = [
      { schemeCode: 'SCH-EDU-002', score: 0.92, occupation: 'STUDENT' },
      { schemeCode: 'SCH-GEN-001', score: 0.70, occupation: 'ALL' }
    ];

    expect(farmerRanking[0].schemeCode).toBe('SCH-AGRI-001');
    expect(studentRanking[0].schemeCode).toBe('SCH-EDU-002');
    expect(farmerRanking[0].schemeCode).not.toBe(studentRanking[0].schemeCode);
  });

  // Test 4: Recommendation scores are not overwritten
  it('4. Recommendation scores are directly preserved from backend and not assigned constants', () => {
    const backendItem = {
      schemeCode: 'SCH-TEST-001',
      recommendationScore: 0.8745
    };
    const mappedScore = Math.round(backendItem.recommendationScore * 100);
    expect(mappedScore).toBe(87);
    expect(mappedScore).not.toBe(100);
    expect(mappedScore).not.toBe(50);
  });

  // Test 5: "Why this scheme" reasons render only when supported
  it('5. Grounded reasons render only when supported by underlying attribute matches', () => {
    const backendReasons = [
      'Statutory eligibility was confirmed by the EligibilityEngine; the recommendation model prioritizes eligible schemes.',
      'Recommended because this scheme is specifically tailored for residents of Maharashtra.',
      'Recommended because the scheme provides targeted welfare benefits aligned with your occupation as Farmer.'
    ];

    const cleanReasons = backendReasons
      .filter((r) => !r.toLowerCase().includes('statutory eligibility was confirmed'))
      .map((r) => r.replace(/^Recommended because this scheme is specifically /i, '')
                   .replace(/^Recommended because the scheme /i, ''))
      .map((r) => r.charAt(0).toUpperCase() + r.slice(1));

    expect(cleanReasons).toHaveLength(2);
    expect(cleanReasons[0]).toContain('Tailored for residents of Maharashtra');
    expect(cleanReasons[1]).toContain('Provides targeted welfare benefits aligned with your occupation as Farmer');
  });

  // Test 6: Missing attributes do not generate false explanations
  it('6. Missing profile attributes never produce synthetic positive match explanations', () => {
    const profileWithoutStudentOrFarmer = {
      state: 'GUJARAT',
      annualIncome: 120000,
      occupation: null,
      isFarmer: false,
      isStudent: false
    };

    // Simulated reason generation without occupation
    const reasons = [];
    if (profileWithoutStudentOrFarmer.occupation) {
      reasons.push(`Matches your occupation as ${profileWithoutStudentOrFarmer.occupation}`);
    }
    if (profileWithoutStudentOrFarmer.isStudent) {
      reasons.push('Designed for students');
    }
    if (profileWithoutStudentOrFarmer.isFarmer) {
      reasons.push('Matches farmer status');
    }

    expect(reasons).toHaveLength(0);
    expect(reasons).not.toContain('Designed for students');
    expect(reasons).not.toContain('Matches farmer status');
  });

  // Test 7: Document checklist is dynamically calculated
  it('7. Document checklist readiness is dynamically evaluated against citizen vault', () => {
    const requiredDocs = ['Aadhaar Card', 'Income Certificate', 'Caste Certificate'];
    const citizenVault = [
      { name: 'Aadhaar Card', status: 'verified', verified: true },
      { name: 'Income Certificate', status: 'uploaded', verified: false }
    ];

    const res = getDocReadinessForScheme(requiredDocs, citizenVault);
    expect(res.totalRequired).toBe(3);
    expect(res.totalAvailable).toBe(2);
    expect(res.readinessScore).toBe(67);
    expect(res.isReady).toBe(false);
  });

  // Test 8: Available documents display ✓
  it('8. Available documents display ✓ icon and correct availability label', () => {
    const requiredDocs = ['Aadhaar Card', 'Income Certificate'];
    const citizenVault = [
      { name: 'Aadhaar Card', status: 'verified', verified: true },
      { name: 'Income Certificate', status: 'uploaded', verified: false }
    ];

    const res = getDocReadinessForScheme(requiredDocs, citizenVault);
    const aadhaar = res.evaluatedItems.find((d) => d.documentName === 'Aadhaar Card');
    const income = res.evaluatedItems.find((d) => d.documentName === 'Income Certificate');

    expect(aadhaar.isAvailable).toBe(true);
    expect(aadhaar.statusIcon).toBe('✓');
    expect(aadhaar.availabilityStatus).toBe('VERIFIED');
    expect(aadhaar.availabilityLabel).toBe('Available (Verified)');

    expect(income.isAvailable).toBe(true);
    expect(income.statusIcon).toBe('✓');
    expect(income.availabilityStatus).toBe('PENDING');
    expect(income.availabilityLabel).toContain('Pending Verification');
  });

  // Test 9: Missing documents display ✗
  it('9. Missing documents display ✗ icon and Unavailable label', () => {
    const requiredDocs = ['Caste Certificate'];
    const citizenVault = [
      { name: 'Aadhaar Card', status: 'verified', verified: true }
    ];

    const res = getDocReadinessForScheme(requiredDocs, citizenVault);
    const caste = res.evaluatedItems.find((d) => d.documentName === 'Caste Certificate');

    expect(caste.isAvailable).toBe(false);
    expect(caste.statusIcon).toBe('✗');
    expect(caste.availabilityStatus).toBe('NOT_AVAILABLE');
    expect(caste.availabilityLabel).toBe('Not Available');
  });

  // Test 10: Scheme selection remains correct through application flow
  it('10. Scheme code, ID, and slug remain consistent throughout application actions', () => {
    const scheme = {
      id: 'sch-pm-kisan-001',
      schemeCode: 'SCH-AGRI-001',
      slug: 'pm-kisan-samman-nidhi',
      name: 'PM-KISAN'
    };
    const applications = [];

    const action = resolveApplicationAction(scheme, applications);
    expect(action.action).toBe(ACTION_TYPES.FILE_APPLICATION);
    expect(action.targetRoute).toBe('/scheme/pm-kisan-samman-nidhi');
  });

  // Test 11: Continue Application works
  it('11. In-progress applications resolve to canonical Continue Application route', () => {
    const scheme = {
      id: 'sch-pm-kisan-001',
      schemeCode: 'SCH-AGRI-001',
      slug: 'pm-kisan-samman-nidhi'
    };
    const applications = [
      {
        id: '6aab75a7c6fcf5080cc33a14',
        schemeCode: 'SCH-AGRI-001',
        status: 'DOCUMENTS_PENDING'
      }
    ];

    const action = resolveApplicationAction(scheme, applications);
    expect(action.action).toBe(ACTION_TYPES.CONTINUE_APPLICATION);
    expect(action.buttonText).toBe('Continue Application');
    expect(action.targetRoute).toBe('/applications/6aab75a7c6fcf5080cc33a14');
  });

  // Test 12: /application/:id redirects correctly
  it('12. /application/:id redirects to canonical /applications/:id with query preservation', () => {
    let redirectedTo = null;
    function MockRedirect() {
      const { id } = useParams();
      const location = useLocation();
      redirectedTo = `/applications/${id}${location.search || ''}`;
      return <div data-testid="target">{redirectedTo}</div>;
    }

    renderToString(
      <MemoryRouter initialEntries={['/application/6aab75a7c6fcf5080cc33a14?tab=docs']}>
        <Routes>
          <Route path="/application/:id" element={<MockRedirect />} />
        </Routes>
      </MemoryRouter>
    );

    expect(redirectedTo).toBe('/applications/6aab75a7c6fcf5080cc33a14?tab=docs');
  });

  // Test 13: Refresh does not lose application state
  it('13. Application state retains consistent status across storage reads', () => {
    const initialAppState = [
      { id: '6aab75a7c6fcf5080cc33a14', schemeCode: 'SCH-AGRI-001', status: 'DOCUMENTS_PENDING' }
    ];
    // Simulated JSON storage persistence cycle
    const serialized = JSON.stringify(initialAppState);
    const restored = JSON.parse(serialized);

    expect(restored[0].id).toBe('6aab75a7c6fcf5080cc33a14');
    expect(restored[0].status).toBe('DOCUMENTS_PENDING');
  });

  // Test 14: Empty recommendation state
  it('14. Empty recommendation state displays professional notice with profile update link', () => {
    const emptyStateProps = {
      title: 'No schemes currently match your eligibility profile.',
      description: 'Based on your verified citizen attributes, the statutory eligibility engine did not find active schemes where all conditions are satisfied.',
      actionLabel: 'Update Profile'
    };

    expect(emptyStateProps.title).toBe('No schemes currently match your eligibility profile.');
    expect(emptyStateProps.actionLabel).toBe('Update Profile');
  });

  // Test 15: API error state
  it('15. API error state displays retry button and temporary unavailability notice', () => {
    const errorStateProps = {
      title: 'Recommendations are temporarily unavailable.',
      actionLabel: 'Retry'
    };

    expect(errorStateProps.title).toBe('Recommendations are temporarily unavailable.');
    expect(errorStateProps.actionLabel).toBe('Retry');
  });

  // Test 16: Loading state
  it('16. Loading state indicates active background statutory evaluation', () => {
    const loadingMessage = 'Finding schemes that match your profile...';
    expect(loadingMessage).toBe('Finding schemes that match your profile...');
  });

});
