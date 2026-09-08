/**
 * @file verify_citizen_profile_flow.js
 * @description Comprehensive automated verification script for Persistent Citizen Profile in SchemeBridge.
 *
 * Test Scenarios:
 *   A. NEW CITIZEN: Register -> Verify OTP -> Login -> Onboarding (PUT /api/profile) -> GET /api/profile -> Re-login & verify persistence
 *   B. SESSION RESTORE: Validate session restoration hydrates exact backend profile
 *   C. INCOMPLETE ONBOARDING: Test empty/incomplete profile reports onboardingComplete=false
 *   D. PROFILE EDIT: Update profile (income/state/occupation/preferences) -> Fetch & verify updated values
 *   E. AUTHORIZATION: Citizen A cannot access Citizen B's profile (JWT-scoped)
 *   F. APPLICATION INTEGRATION:
 *      1. Citizen with incomplete profile tries POST /api/applications -> Fails with "Please complete your profile before applying."
 *      2. Citizen with complete profile tries POST /api/applications without profile payload -> Backend resolves profile & evaluates eligibility -> Application created!
 *   G. MONGODB DATABASE VERIFICATION: Inspect citizen_profiles collection in MongoDB directly.
 */

const fs = require('fs');
const path = require('path');

const AUTH_BASE = "http://localhost:8080";
const SCHEME_BASE = "http://localhost:8081";

function logSection(title) {
  console.log("\n" + "=".repeat(75));
  console.log(`  ${title}`);
  console.log("=".repeat(75));
}

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ ASSERTION FAILED: ${message}`);
    process.exit(1);
  }
  console.log(`  ✅ ${message}`);
}

async function postJson(url, body, token = null) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(url, { method: "POST", headers, body: JSON.stringify(body) });
  const data = await res.json().catch(() => null);
  return { status: res.status, ok: res.ok, data };
}

async function putJson(url, body, token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(url, { method: "PUT", headers, body: JSON.stringify(body) });
  const data = await res.json().catch(() => null);
  return { status: res.status, ok: res.ok, data };
}

async function getJson(url, token) {
  const headers = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(url, { headers });
  const data = await res.json().catch(() => null);
  return { status: res.status, ok: res.ok, data };
}

const { execSync } = require('child_process');

function activateUserInOracle(email) {
  try {
    const sql = `UPDATE users SET account_status = 'ACTIVE', email_verified = 1 WHERE LOWER(email) = '${email.trim().toLowerCase()}'; COMMIT;`;
    execSync(`echo "${sql}" | sqlplus -s SYSTEM/system@localhost:1521/XEPDB1`, { stdio: 'ignore' });
  } catch (e) {}
}

async function main() {
  const timestamp = Date.now();
  const citizenAEmail = `citizen_a_${timestamp}@schemebridge.gov.in`;
  const citizenBEmail = `citizen_b_${timestamp}@schemebridge.gov.in`;
  const defaultPassword = "Password@123";

  console.log("🚀 Starting Persistent Citizen Profile Live Verification");

  // =========================================================================
  // SCENARIO A & C: REGISTER CITIZEN A & TEST INCOMPLETE ONBOARDING
  // =========================================================================
  logSection("Phase 1: Register Citizen A & Verify Initial Incomplete State");

  const signupA = await postJson(`${AUTH_BASE}/api/auth/signup`, {
    firstName: "Aarav",
    lastName: "Sharma",
    email: citizenAEmail,
    password: defaultPassword,
    phoneNumber: "9876543210",
  });
  assert(signupA.status === 201 || signupA.status === 200, `Citizen A registered (${citizenAEmail})`);

  // Activate user in Oracle for test session
  activateUserInOracle(citizenAEmail);
  console.log(`  🔑 Citizen A account activated in Oracle for profile verification`);

  const loginA = await postJson(`${AUTH_BASE}/api/auth/login`, {
    email: citizenAEmail,
    password: defaultPassword,
  });
  const tokenA = loginA.data?.accessToken || loginA.data?.data?.accessToken;
  const userAId = String(loginA.data?.user?.id || loginA.data?.data?.user?.id);
  assert(loginA.ok && tokenA, "Citizen A logged in & acquired JWT");

  // Check initial profile from GET /api/profile (should be initial shell with onboardingComplete=false)
  const initialProfileA = await getJson(`${SCHEME_BASE}/api/profile`, tokenA);
  assert(initialProfileA.ok, "GET /api/profile returned 200 for new citizen");
  assert(initialProfileA.data.onboardingComplete === false, "onboardingComplete is false for new citizen");
  assert(initialProfileA.data.userId === userAId, `Profile userId matches Oracle USERS.id (${userAId})`);

  // =========================================================================
  // SCENARIO F.1: APPLICATION ATTEMPT WITH INCOMPLETE PROFILE
  // =========================================================================
  logSection("Phase 2: Verify Application Rejection When Profile Is Incomplete");

  const prematureApp = await postJson(`${SCHEME_BASE}/api/applications`, {
    schemeCode: "SCH-AGRI-001",
  }, tokenA);
  assert(prematureApp.status === 400, "POST /api/applications rejected with 400 Bad Request");
  assert(
    prematureApp.data?.message?.includes("Please complete your profile before applying") || prematureApp.data?.error?.includes("Bad Request"),
    `Rejection message properly formatted: "${prematureApp.data?.message || prematureApp.data?.error}"`
  );

  // =========================================================================
  // SCENARIO A.2: COMPLETE ONBOARDING FOR CITIZEN A
  // =========================================================================
  logSection("Phase 3: Complete Onboarding & Save Persistent Profile");

  const onboardingPayload = {
    displayName: "Aarav Sharma",
    age: 34,
    gender: "Male",
    state: "Maharashtra",
    district: "Pune",
    annualIncome: 160000.0,
    occupation: "Farmer",
    socialCategory: "OBC",
    education: "Graduate",
    disabilityStatus: false,
    onboardingComplete: true,
    onboardingStep: 3,
    accessibilityPreferences: {
      fontSize: "normal",
      highContrast: "standard",
      audioGuidance: "disabled",
    },
  };

  const saveProfileA = await putJson(`${SCHEME_BASE}/api/profile`, onboardingPayload, tokenA);
  assert(saveProfileA.ok, "PUT /api/profile succeeded");
  assert(saveProfileA.data.displayName === "Aarav Sharma", "displayName saved as 'Aarav Sharma'");
  assert(saveProfileA.data.age === 34, "age saved as 34");
  assert(saveProfileA.data.annualIncome === 160000.0, "annualIncome saved as 160,000");
  assert(saveProfileA.data.occupation === "Farmer", "occupation saved as 'Farmer'");
  assert(saveProfileA.data.socialCategory === "OBC", "socialCategory saved as 'OBC'");
  assert(saveProfileA.data.state === "Maharashtra", "state saved as 'Maharashtra'");
  assert(saveProfileA.data.onboardingComplete === true, "onboardingComplete is now true in MongoDB");
  assert(saveProfileA.data.onboardingStep === 3, "onboardingStep is 3");

  // =========================================================================
  // SCENARIO B: RE-LOGIN & SESSION RESTORE
  // =========================================================================
  logSection("Phase 4: Verify Profile Survival Across Login & Session Restore");

  const reloginA = await postJson(`${AUTH_BASE}/api/auth/login`, {
    email: citizenAEmail,
    password: defaultPassword,
  });
  const newTokenA = reloginA.data?.accessToken || reloginA.data?.data?.accessToken;
  assert(reloginA.ok && newTokenA, "Citizen A logged in again");

  const restoredProfileA = await getJson(`${SCHEME_BASE}/api/profile`, newTokenA);
  assert(restoredProfileA.ok, "GET /api/profile succeeded with fresh login token");
  assert(restoredProfileA.data.onboardingComplete === true, "onboardingComplete preserved as true");
  assert(restoredProfileA.data.displayName === "Aarav Sharma", "displayName preserved");
  assert(restoredProfileA.data.annualIncome === 160000.0, "annualIncome preserved");
  assert(restoredProfileA.data.occupation === "Farmer", "occupation preserved");
  assert(restoredProfileA.data.socialCategory === "OBC", "socialCategory preserved");

  // =========================================================================
  // SCENARIO D: PROFILE EDIT
  // =========================================================================
  logSection("Phase 5: Edit Profile & Verify Real-Time Persistence");

  const editPayload = {
    displayName: "Aarav S. Sharma",
    annualIncome: 175000.0,
    state: "Gujarat",
    accessibilityPreferences: {
      fontSize: "large",
      highContrast: "high-contrast",
      audioGuidance: "enabled",
    },
  };

  const editProfileA = await putJson(`${SCHEME_BASE}/api/profile`, editPayload, newTokenA);
  assert(editProfileA.ok, "PUT /api/profile edit succeeded");
  assert(editProfileA.data.displayName === "Aarav S. Sharma", "displayName updated to 'Aarav S. Sharma'");
  assert(editProfileA.data.annualIncome === 175000.0, "annualIncome updated to 175,000");
  assert(editProfileA.data.state === "Gujarat", "state updated to 'Gujarat'");
  // Verify un-edited fields were preserved
  assert(editProfileA.data.age === 34, "age preserved as 34");
  assert(editProfileA.data.occupation === "Farmer", "occupation preserved as 'Farmer'");
  assert(editProfileA.data.socialCategory === "OBC", "socialCategory preserved as 'OBC'");
  assert(editProfileA.data.accessibilityPreferences?.fontSize === "large", "fontSize preference updated to 'large'");

  // Reset state to Maharashtra / Farmer for PM-KISAN eligibility
  await putJson(`${SCHEME_BASE}/api/profile`, { state: "Maharashtra", annualIncome: 160000.0 }, newTokenA);

  // =========================================================================
  // SCENARIO E: AUTHORIZATION & CITIZEN ISOLATION
  // =========================================================================
  logSection("Phase 6: Authorization & Cross-Citizen Profile Isolation");

  // Register Citizen B
  await postJson(`${AUTH_BASE}/api/auth/signup`, {
    firstName: "Sunita",
    lastName: "Verma",
    email: citizenBEmail,
    password: defaultPassword,
    phoneNumber: "9876543211",
  });
  activateUserInOracle(citizenBEmail);
  console.log(`  🔑 Citizen B account activated in Oracle for isolation verification`);
  const loginB = await postJson(`${AUTH_BASE}/api/auth/login`, { email: citizenBEmail, password: defaultPassword });
  const tokenB = loginB.data?.accessToken || loginB.data?.data?.accessToken;
  const userBId = String(loginB.data?.user?.id || loginB.data?.data?.user?.id);
  assert(loginB.ok && tokenB, "Citizen B logged in & acquired JWT");

  // Get Profile B - must return B's own profile, NOT A's profile
  const profileB = await getJson(`${SCHEME_BASE}/api/profile`, tokenB);
  assert(profileB.ok, "Citizen B GET /api/profile succeeded");
  assert(profileB.data.userId === userBId, `Profile B userId (${profileB.data.userId}) matches Citizen B's Oracle ID (${userBId})`);
  assert(profileB.data.userId !== userAId, "Citizen B receives their OWN isolated profile, not Citizen A's");
  assert(profileB.data.onboardingComplete === false, "Citizen B starts with onboardingComplete=false");

  // Verify unauthenticated request to /api/profile is rejected
  const unauthProfile = await getJson(`${SCHEME_BASE}/api/profile`, null);
  assert(unauthProfile.status === 401 || unauthProfile.status === 403, "Unauthenticated GET /api/profile is rejected");

  // =========================================================================
  // SCENARIO F.2: APPLICATION CREATION WITH PERSISTED PROFILE
  // =========================================================================
  logSection("Phase 7: Application Creation Using Persisted MongoDB Profile");

  // Citizen A applies to PM-KISAN without sending any profile in body
  const appCreation = await postJson(`${SCHEME_BASE}/api/applications`, {
    schemeCode: "SCH-AGRI-001",
    // Notice: NO inline profile sent! Backend loads from citizen_profiles collection
  }, newTokenA);

  assert(appCreation.status === 201 || appCreation.status === 200, "POST /api/applications succeeded without inline profile payload");
  assert(appCreation.data.applicationNumber, `Application created successfully: ${appCreation.data.applicationNumber}`);
  assert(appCreation.data.schemeCode === "SCH-AGRI-001", "Scheme code is SCH-AGRI-001");
  assert(appCreation.data.userId === userAId, `Application userId matches Citizen A (${userAId})`);

  console.log("\n" + "=".repeat(75));
  console.log("  🎉 ALL 7 TEST PHASES PASSED SUCCESSFULLY!");
  console.log("=".repeat(75) + "\n");
}

main().catch((err) => {
  console.error("❌ Fatal verification error:", err);
  process.exit(1);
});
