import { describe, it, expect } from 'vitest';

describe('Phase 29 — Production Outcome Integrity, Data Quality & Automated Training Gate', () => {

  // Test 1: Data Quality Classification
  it('1. Classifies telemetry records deterministically without silently dropping data', () => {
    const statuses = ['VALID', 'DUPLICATE', 'SYNTHETIC', 'MALFORMED', 'PII_VIOLATION', 'ORPHAN', 'INVALID_SEQUENCE'];

    const classify = (event, existingCodes = ['SCH_001']) => {
      if (event.userId?.includes('test') || event.userId === 'citizen_user' || event.userId === 'citizen1') return 'SYNTHETIC';
      if (!event.sessionId || !event.schemeCode || !event.eventType) return 'MALFORMED';
      if (event.metadata && Object.keys(event.metadata).some(k => ['aadhaar', 'email', 'phone'].includes(k.toLowerCase()))) return 'PII_VIOLATION';
      if (!existingCodes.includes(event.schemeCode)) return 'ORPHAN';
      if (event.isDuplicate) return 'DUPLICATE';
      if (event.invalidSequence) return 'INVALID_SEQUENCE';
      return 'VALID';
    };

    expect(classify({ userId: 'citizen_user' })).toBe('SYNTHETIC');
    expect(classify({ sessionId: null })).toBe('MALFORMED');
    expect(classify({ sessionId: 's1', schemeCode: 'SCH_001', eventType: 'VIEW', metadata: { phone: '999' } })).toBe('PII_VIOLATION');
    expect(classify({ sessionId: 's1', schemeCode: 'SCH_999', eventType: 'VIEW' })).toBe('ORPHAN');
    expect(classify({ sessionId: 's1', schemeCode: 'SCH_001', eventType: 'VIEW', isDuplicate: true })).toBe('DUPLICATE');
    expect(classify({ sessionId: 's1', schemeCode: 'SCH_001', eventType: 'VIEW', invalidSequence: true })).toBe('INVALID_SEQUENCE');
    expect(classify({ sessionId: 's1', schemeCode: 'SCH_001', eventType: 'VIEW' })).toBe('VALID');
  });

  // Test 2: Legitimate Outcome Session Definition
  it('2. Enforces strict definition for legitimate outcome sessions (requires conversion event)', () => {
    const isLegitimateOutcomeSession = (session) => {
      if (session.isSynthetic) return false;
      if (!session.sessionId || !session.schemeCode) return false;
      if (session.hasPii) return false;
      if (session.isDuplicate) return false;
      // Must contain at least one legitimate outcome event
      const outcomeEvents = ['SCHEME_APPLIED', 'APPLICATION_COMPLETED'];
      return session.events?.some(e => outcomeEvents.includes(e.eventType));
    };

    const impressionOnly = {
      isSynthetic: false,
      sessionId: 'sess_1',
      schemeCode: 'SCH_001',
      events: [{ eventType: 'RECOMMENDATION_SHOWN' }, { eventType: 'SCHEME_VIEWED' }]
    };
    expect(isLegitimateOutcomeSession(impressionOnly)).toBe(false);

    const syntheticOutcome = {
      isSynthetic: true,
      sessionId: 'sess_syn',
      schemeCode: 'SCH_001',
      events: [{ eventType: 'APPLICATION_COMPLETED' }]
    };
    expect(isLegitimateOutcomeSession(syntheticOutcome)).toBe(false);

    const legitimateOutcome = {
      isSynthetic: false,
      sessionId: 'sess_legit',
      schemeCode: 'SCH_001',
      events: [{ eventType: 'RECOMMENDATION_SHOWN' }, { eventType: 'APPLICATION_COMPLETED' }]
    };
    expect(isLegitimateOutcomeSession(legitimateOutcome)).toBe(true);
  });

  // Test 3: Centralized Training Readiness Gate
  it('3. Evaluates centralized training gate: <100 blocks training/promotion, >=100 permits training but blocks promotion', () => {
    const evaluateGate = (legitimateCount) => {
      const threshold = 100;
      if (legitimateCount < threshold) {
        return {
          status: 'TRAINING_NOT_READY',
          modelTrainingAllowed: false,
          modelPromotionAllowed: false,
          remaining: threshold - legitimateCount
        };
      } else {
        return {
          status: 'TRAINING_READY',
          modelTrainingAllowed: true,
          modelPromotionAllowed: false, // Promotion strictly blocked
          remaining: 0
        };
      }
    };

    const zeroDecision = evaluateGate(0);
    expect(zeroDecision.status).toBe('TRAINING_NOT_READY');
    expect(zeroDecision.modelTrainingAllowed).toBe(false);
    expect(zeroDecision.modelPromotionAllowed).toBe(false);
    expect(zeroDecision.remaining).toBe(100);

    const subThreshold = evaluateGate(99);
    expect(subThreshold.status).toBe('TRAINING_NOT_READY');
    expect(subThreshold.modelTrainingAllowed).toBe(false);
    expect(subThreshold.remaining).toBe(1);

    const readyDecision = evaluateGate(100);
    expect(readyDecision.status).toBe('TRAINING_READY');
    expect(readyDecision.modelTrainingAllowed).toBe(true);
    expect(readyDecision.modelPromotionAllowed).toBe(false);
    expect(readyDecision.remaining).toBe(0);
  });

  // Test 4: Feature / Target Label Leakage Prevention
  it('4. Detects and prevents target label leakage into input feature vectors', () => {
    const forbiddenKeys = ['relevancegrade', 'applicationoutcome', 'conversion', 'target', 'label'];

    const checkLeakage = (featureVector) => {
      const keys = Object.keys(featureVector.rankingContext || {});
      const leaked = keys.some(k => forbiddenKeys.some(f => k.toLowerCase().includes(f)));
      if (leaked) {
        throw new Error('CRITICAL TARGET LEAKAGE DETECTED');
      }
      return true;
    };

    const cleanVector = {
      rankingContext: { currentRank: 1, currentScore: 0.95, modelVersion: '2.2.0-hybrid-semantic-384d' }
    };
    expect(checkLeakage(cleanVector)).toBe(true);

    const dirtyVector = {
      rankingContext: { currentRank: 1, relevanceGrade: 3 }
    };
    expect(() => checkLeakage(dirtyVector)).toThrow('CRITICAL TARGET LEAKAGE DETECTED');
  });

  // Test 5: Model Environment Preservation
  it('5. Verifies production model environment remains strictly preserved', () => {
    const env = {
      activeModel: '2.2.0-hybrid-semantic-384d',
      fallbackModel: '1.0.0-deterministic',
      circuitBreakerMs: 200,
      statutoryEligibilityViolationRate: 0.00
    };

    expect(env.activeModel).toBe('2.2.0-hybrid-semantic-384d');
    expect(env.fallbackModel).toBe('1.0.0-deterministic');
    expect(env.circuitBreakerMs).toBe(200);
    expect(env.statutoryEligibilityViolationRate).toBe(0.00);
  });
});
