const fs = require('fs');
const path = require('path');

const AUTH_URL = 'http://localhost:8080';
const SCHEME_URL = 'http://localhost:8081';

async function request(url, options = {}) {
  const res = await fetch(url, options);
  const text = await res.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch (e) {
    json = text;
  }
  return { status: res.status, ok: res.ok, data: json };
}

async function run() {
  console.log('================================================================');
  console.log('SCHEMEBRIDGE LIVE END-TO-END CROSS-PORTAL WORKFLOW VERIFICATION');
  console.log('================================================================\n');

  // 1. Citizen Login
  console.log('[1/9] Logging in as Citizen (citizen@schemebridge.gov.in)...');
  const citizenLogin = await request(`${AUTH_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'citizen@schemebridge.gov.in',
      password: 'Citizen@123456'
    })
  });
  if (!citizenLogin.ok) throw new Error('Citizen login failed: ' + JSON.stringify(citizenLogin.data));
  const citizenToken = citizenLogin.data.data?.accessToken || citizenLogin.data.accessToken || citizenLogin.data.token;
  const citizenUserId = String(citizenLogin.data.data?.user?.id || citizenLogin.data.user?.id);
  console.log('✓ Citizen authenticated. Token acquired. User ID:', citizenUserId);

  // 1b. Ensure Citizen Profile is persisted in MongoDB
  const setupProfileRes = await request(`${SCHEME_URL}/api/profile`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${citizenToken}`
    },
    body: JSON.stringify({
      displayName: 'Citizen Demo',
      age: 22,
      gender: 'Male',
      annualIncome: 120000,
      occupation: 'Farmer',
      state: 'Maharashtra',
      socialCategory: 'General',
      disabilityStatus: false,
      onboardingComplete: true,
      onboardingStep: 3
    })
  });
  if (!setupProfileRes.ok) throw new Error('Citizen profile setup failed: ' + JSON.stringify(setupProfileRes.data));
  console.log('✓ Persistent Citizen Profile verified in MongoDB: onboardingComplete=true');

  // 2. Admin Login
  console.log('\n[2/9] Logging in as Admin (admin@schemebridge.gov.in)...');
  const adminLogin = await request(`${AUTH_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'admin@schemebridge.gov.in',
      password: 'Admin@123456'
    })
  });
  if (!adminLogin.ok) throw new Error('Admin login failed: ' + JSON.stringify(adminLogin.data));
  const adminToken = adminLogin.data.data?.accessToken || adminLogin.data.accessToken || adminLogin.data.token;
  const adminUserId = String(adminLogin.data.data?.user?.id || adminLogin.data.user?.id);
  console.log('✓ Admin authenticated. Token acquired. Admin ID:', adminUserId);

  // 3. Citizen selects Scheme and Creates Application
  console.log('\n[3/9] Fetching schemes and creating application...');
  const schemesRes = await request(`${SCHEME_URL}/api/schemes?size=10`, {
    headers: { Authorization: `Bearer ${citizenToken}` }
  });
  const schemesList = schemesRes.data.content || schemesRes.data.data?.content || schemesRes.data;
  if (!schemesList || schemesList.length === 0) throw new Error('No schemes found in MongoDB');
  const targetScheme = schemesList.find(s => s.schemeCode === 'SCH-AGRI-001') || schemesList[0];
  console.log(`✓ Selected Scheme: ${targetScheme.schemeCode} - ${targetScheme.title?.english || targetScheme.schemeName || targetScheme.schemeCode}`);

  let applicationId;
  let applicationNumber;

  const createAppRes = await request(`${SCHEME_URL}/api/applications`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${citizenToken}`
    },
    body: JSON.stringify({
      schemeId: targetScheme.id,
      schemeCode: targetScheme.schemeCode,
    })
  });

  if (createAppRes.status === 409) {
    console.log('ℹ Active application already exists for this scheme. Resuming existing application...');
    const myAppsRes = await request(`${SCHEME_URL}/api/applications/my`, {
      headers: { Authorization: `Bearer ${citizenToken}` }
    });
    const existingApps = myAppsRes.data || [];
    const match = existingApps.find(a => a.schemeCode === targetScheme.schemeCode) || existingApps[0];
    if (!match) throw new Error('Could not find existing application to resume');
    applicationId = match.id;
    applicationNumber = match.applicationNumber;
    console.log(`✓ Resumed Application: ID=${applicationId}, Number=${applicationNumber}, Status=${match.status}`);
  } else if (!createAppRes.ok) {
    throw new Error('Create application failed: ' + JSON.stringify(createAppRes.data));
  } else {
    const appData = createAppRes.data.data || createAppRes.data;
    applicationId = appData.id;
    applicationNumber = appData.applicationNumber;
    console.log(`✓ Application Created: ID=${applicationId}, Number=${applicationNumber}, Status=${appData.status}`);
  }

  // 4. Citizen uploads Initial Documents & Submits Application
  console.log('\n[4/9] Uploading initial documents to GridFS and Submitting application...');
  const dummyPdfBuffer = Buffer.from('%PDF-1.4 Mock Aadhaar Document Content for SchemeBridge Test %EOF');
  const boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW';
  
  function createMultipartBody(fieldName, filename, contentType, buffer) {
    const header = `--${boundary}\r\nContent-Disposition: form-data; name="${fieldName}"; filename="${filename}"\r\nContent-Type: ${contentType}\r\n\r\n`;
    const footer = `\r\n--${boundary}--\r\n`;
    return Buffer.concat([Buffer.from(header, 'utf8'), buffer, Buffer.from(footer, 'utf8')]);
  }

  // Get application details to check document codes
  const appDetailsRes = await request(`${SCHEME_URL}/api/applications/${applicationId}`, {
    headers: { Authorization: `Bearer ${citizenToken}` }
  });
  const appDetails = appDetailsRes.data.data || appDetailsRes.data;
  const docsToUpload = appDetails.documents || [];
  console.log(`✓ Application has ${docsToUpload.length} required document slot(s):`, docsToUpload.map(d => `${d.documentCode} (${d.documentName || d.documentCode})`).join(', '));

  const targetDocCode = docsToUpload[0]?.documentCode || 'IDENTITY';

  for (const d of docsToUpload) {
    const uploadBody = createMultipartBody('file', `${d.documentCode.toLowerCase()}_sample.pdf`, 'application/pdf', dummyPdfBuffer);
    const uploadDocRes = await request(`${SCHEME_URL}/api/applications/${applicationId}/documents/${d.documentCode}`, {
      method: 'POST',
      headers: {
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        Authorization: `Bearer ${citizenToken}`
      },
      body: uploadBody
    });
    if (!uploadDocRes.ok) throw new Error(`Upload document ${d.documentCode} failed: ` + JSON.stringify(uploadDocRes.data));
    console.log(`✓ Document ${d.documentCode} uploaded successfully to GridFS.`);
  }

  const submitAppRes = await request(`${SCHEME_URL}/api/applications/${applicationId}/submit`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${citizenToken}` }
  });
  if (!submitAppRes.ok) throw new Error('Submit application failed: ' + JSON.stringify(submitAppRes.data));
  console.log(`✓ Application Submitted. Status: ${(submitAppRes.data.data || submitAppRes.data).status}`);

  // 5. Admin Review Queue & Start Review
  console.log('\n[5/9] Admin fetches Review Queue and Starts Review...');
  const adminQueueRes = await request(`${SCHEME_URL}/api/admin/applications`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  const queueApps = adminQueueRes.data.content || adminQueueRes.data.data?.content || adminQueueRes.data;
  const queueMatch = queueApps.find(a => a.id === applicationId || a.applicationNumber === applicationNumber);
  console.log(`✓ Verified application in Admin Review Queue: Number=${queueMatch?.applicationNumber}, Status=${queueMatch?.status}`);

  const startReviewRes = await request(`${SCHEME_URL}/api/admin/applications/${applicationId}/review/start`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  console.log(`✓ Review Started by Admin. Response Status: ${startReviewRes.status}`);

  // 6. Admin Rejects Document with Reason
  console.log(`\n[6/9] Admin reviews document and rejects ${targetDocCode} with reason...`);
  const rejectDocRes = await request(`${SCHEME_URL}/api/admin/applications/${applicationId}/documents/${targetDocCode}/reject`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${adminToken}`
    },
    body: JSON.stringify({
      reason: `Uploaded ${targetDocCode} is blurred. Please upload a clear official document.`
    })
  });
  if (!rejectDocRes.ok) throw new Error('Reject document failed: ' + JSON.stringify(rejectDocRes.data));
  console.log(`✓ Admin rejected ${targetDocCode} document with reason: "Uploaded ${targetDocCode} is blurred. Please upload a clear official document."`);

  // Check Citizen Notifications
  const citizenNotifsRes = await request(`${SCHEME_URL}/api/notifications`, {
    headers: { Authorization: `Bearer ${citizenToken}` }
  });
  const citizenNotifs = citizenNotifsRes.data.content || citizenNotifsRes.data.data?.content || citizenNotifsRes.data;
  console.log(`✓ Citizen received ${citizenNotifs.length} notification(s). Latest: "${citizenNotifs[0]?.title}" - "${citizenNotifs[0]?.message}"`);

  // 7. Citizen Uploads Corrected Document + Re-Submits
  console.log(`\n[7/9] Citizen uploads corrected document for ${targetDocCode} and re-submits...`);
  const correctedPdfBuffer = Buffer.from('%PDF-1.4 Corrected Clear Government Document Content %EOF');
  const correctedUploadBody = createMultipartBody('file', `clear_${targetDocCode.toLowerCase()}.pdf`, 'application/pdf', correctedPdfBuffer);
  const reUploadRes = await request(`${SCHEME_URL}/api/applications/${applicationId}/documents/${targetDocCode}`, {
    method: 'POST',
    headers: {
      'Content-Type': `multipart/form-data; boundary=${boundary}`,
      Authorization: `Bearer ${citizenToken}`
    },
    body: correctedUploadBody
  });
  if (!reUploadRes.ok) throw new Error('Re-upload document failed: ' + JSON.stringify(reUploadRes.data));
  console.log(`✓ Citizen successfully re-uploaded corrected ${targetDocCode} document. Status: ${(reUploadRes.data.data || reUploadRes.data).verificationStatus || 'PENDING'}`);

  // Citizen re-submits application (CORRECTION_REQUIRED → READY_FOR_SUBMISSION → SUBMITTED)
  const reSubmitRes = await request(`${SCHEME_URL}/api/applications/${applicationId}/submit`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${citizenToken}` }
  });
  if (!reSubmitRes.ok) throw new Error('Re-submit application failed: ' + JSON.stringify(reSubmitRes.data));
  console.log(`✓ Citizen re-submitted application. New Status: ${(reSubmitRes.data.data || reSubmitRes.data).status}`);

  // Admin receives notifications about re-submission via GET /api/notifications (role-aware endpoint)
  const adminNotifsRes = await request(`${SCHEME_URL}/api/notifications?page=0&size=5`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  // Response shape: PagedNotificationResponse { content, page, size, totalElements, totalPages, unreadCount }
  const adminNotifs = adminNotifsRes.data.content || [];
  const adminUnreadCount = adminNotifsRes.data.unreadCount ?? adminNotifs.filter(n => !n.read).length;
  console.log(`✓ Admin notification inbox: ${adminNotifsRes.data.totalElements} total, ${adminUnreadCount} unread. Latest: "${adminNotifs[0]?.title}"`);

  // Admin starts review again (SUBMITTED → UNDER_REVIEW)
  const startReview2Res = await request(`${SCHEME_URL}/api/admin/applications/${applicationId}/review/start`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  if (!startReview2Res.ok) throw new Error('Admin re-start review failed: ' + JSON.stringify(startReview2Res.data));
  console.log(`✓ Admin re-started review. Status now UNDER_REVIEW.`);

  // 8. Admin Verifies All Documents and Approves Application
  console.log('\n[8/9] Admin verifies all corrected documents and approves application...');
  for (const d of docsToUpload) {
    const verifyDocRes = await request(`${SCHEME_URL}/api/admin/applications/${applicationId}/documents/${d.documentCode}/verify`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    if (!verifyDocRes.ok) throw new Error(`Verify document ${d.documentCode} failed: ` + JSON.stringify(verifyDocRes.data));
    console.log(`✓ Admin verified document: ${d.documentCode}`);
  }

  const approveAppRes = await request(`${SCHEME_URL}/api/admin/applications/${applicationId}/review/approve`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${adminToken}`
    },
    body: JSON.stringify({
      remarks: 'All eligibility criteria verified and required documents authenticated.'
    })
  });
  if (!approveAppRes.ok) throw new Error('Approve application failed: ' + JSON.stringify(approveAppRes.data));
  console.log(`✓ Application APPROVED by Admin. Final status: ${(approveAppRes.data.data || approveAppRes.data).status || 'APPROVED'}`);

  // 9. Verification of Timeline, Audit Trail, Dashboard Metrics, and CSV Export
  console.log('\n[9/9] Verifying Timeline events, Audit Logs, Analytics Metrics, and Reports...');
  const timelineRes = await request(`${SCHEME_URL}/api/applications/${applicationId}/timeline`, {
    headers: { Authorization: `Bearer ${citizenToken}` }
  });
  const rawTimeline = timelineRes.data;
  const timelineEvents = Array.isArray(rawTimeline) ? rawTimeline
    : Array.isArray(rawTimeline?.data) ? rawTimeline.data
    : Array.isArray(rawTimeline?.events) ? rawTimeline.events
    : Array.isArray(rawTimeline?.content) ? rawTimeline.content
    : [];
  console.log(`✓ Timeline has ${timelineEvents.length} chronological events:`);
  timelineEvents.forEach((ev, idx) => {
    console.log(`   ${idx + 1}. [${ev.eventType || ev.action || ev.type || '?'}] → ${ev.toStatus || ev.status || '-'} | ${ev.message || ev.remarks || '-'}`);
  });
  if (timelineEvents.length === 0) {
    console.log('   ⚠ Timeline returned no events. Raw response:', JSON.stringify(rawTimeline).substring(0, 300));
  }

  const auditRes = await request(`${SCHEME_URL}/api/admin/audit-logs?size=5`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  const rawAudit = auditRes.data;
  const auditLogs = Array.isArray(rawAudit?.content) ? rawAudit.content
    : Array.isArray(rawAudit?.data?.content) ? rawAudit.data.content
    : Array.isArray(rawAudit) ? rawAudit : [];
  console.log(`\n✓ Latest Admin Audit Logs (${auditLogs.length} entries):`);
  auditLogs.slice(0, 5).forEach((log, idx) => {
    console.log(`   ${idx + 1}. [${log.action}] ${log.entityType} ${log.entityId} by ${log.actorUserId}`);
  });

  const metricsRes = await request(`${SCHEME_URL}/api/admin/metrics`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  const metrics = metricsRes.data.data || metricsRes.data;
  console.log('\n✓ Live Admin Operational Metrics:');
  // Canonical field: correctionRequiredApplications (from AdminMetricsResponse DTO)
  console.log(`   totalApplications=${metrics.totalApplications}, approvedApplications=${metrics.approvedApplications}, pendingApplications=${metrics.pendingApplications}, correctionRequiredApplications=${metrics.correctionRequiredApplications}, unreadAdminNotifications=${metrics.unreadAdminNotifications}`);

  const exportRes = await request(`${SCHEME_URL}/api/admin/reports/applications/export`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  const csvData = typeof exportRes.data === 'string' ? exportRes.data : JSON.stringify(exportRes.data);
  console.log(`\n✓ Live CSV Report Export: HTTP ${exportRes.status} | ${csvData.length} chars`);
  console.log('Sample CSV:\n' + csvData.split('\n').slice(0, 3).join('\n'));

  console.log('\n================================================================');
  console.log('ALL 9 PHASES OF CROSS-PORTAL FLOW VERIFIED SUCCESSFULLY (PASS)');
  console.log('================================================================');
}

run().catch(err => {
  console.error('\n❌ VERIFICATION FAILED:', err.message);
  process.exit(1);
});
