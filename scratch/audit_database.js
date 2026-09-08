// Phase 22A Forensic Database Audit Script
const schemeDb = db.getSiblingDB("schemebridge_scheme_db");
const rootDb = db.getSiblingDB("schemebridge");
const appDb = db.getSiblingDB("schemebridge_application_db");

print("================================================================================");
print("PHASE 22A: MONGODB FORENSIC DATABASE AUDIT REPORT DATA");
print("================================================================================");

// 1. COLLECTION COUNTS
print("\n--- 1. GLOBAL COLLECTION COUNTS ---");
print("schemebridge_scheme_db.schemes: " + schemeDb.schemes.countDocuments());
print("schemebridge_scheme_db.scheme_verified_data: " + schemeDb.scheme_verified_data.countDocuments());
print("schemebridge_scheme_db.citizen_profiles: " + schemeDb.citizen_profiles.countDocuments());
print("schemebridge_scheme_db.applications: " + schemeDb.applications.countDocuments());
print("schemebridge_scheme_db.application_events: " + schemeDb.application_events.countDocuments());
print("schemebridge_scheme_db.application_reviews: " + schemeDb.application_reviews.countDocuments());
print("schemebridge_scheme_db.recommendation_events: " + schemeDb.recommendation_events.countDocuments());
print("schemebridge.applications: " + rootDb.applications.countDocuments());
print("schemebridge.citizens: " + rootDb.citizens.countDocuments());
print("schemebridge.citizen_profiles: " + rootDb.citizen_profiles.countDocuments());
print("schemebridge.ai_chat_conversations: " + rootDb.ai_chat_conversations.countDocuments());
print("schemebridge.saved_schemes: " + rootDb.saved_schemes.countDocuments());
print("schemebridge_application_db.applications: " + appDb.applications.countDocuments());

// 2. SCHEMES COLLECTION AUDIT
print("\n--- 2. SCHEMES COLLECTION (schemebridge_scheme_db.schemes) AUDIT ---");
const totalSchemes = schemeDb.schemes.countDocuments();

// Sample fields and types from first 500 documents
const sampleSchemes = schemeDb.schemes.find().limit(1000).toArray();
const fieldCounts = {};
const fieldTypes = {};

sampleSchemes.forEach(doc => {
  function inspectObj(obj, prefix = "") {
    for (const key of Object.keys(obj)) {
      const fullKey = prefix ? prefix + "." + key : key;
      const val = obj[key];
      fieldCounts[fullKey] = (fieldCounts[fullKey] || 0) + 1;
      let t = typeof val;
      if (val === null) t = "null";
      else if (Array.isArray(val)) t = "array";
      else if (val instanceof Date) t = "date";
      else if (t === "object") {
        fieldTypes[fullKey] = fieldTypes[fullKey] || new Set();
        fieldTypes[fullKey].add("object");
        inspectObj(val, fullKey);
        continue;
      }
      fieldTypes[fullKey] = fieldTypes[fullKey] || new Set();
      fieldTypes[fullKey].add(t);
    }
  }
  inspectObj(doc);
});

print("Total Schemes Analyzed in Sample: " + sampleSchemes.length);
print("Fields, Types, Presence Rate in schemes:");
for (const [f, cnt] of Object.entries(fieldCounts).sort((a, b) => b[1] - a[1])) {
  const pct = ((cnt / sampleSchemes.length) * 100).toFixed(1);
  const types = Array.from(fieldTypes[f] || []).join("|");
  print(`  ${f}: ${cnt}/${sampleSchemes.length} (${pct}%) [${types}]`);
}

// Check categorical distributions in schemes
print("\nScheme Status Distribution:");
schemeDb.schemes.aggregate([{ $group: { _id: "$status", count: { $sum: 1 } } }]).forEach(r => print(`  ${r._id}: ${r.count}`));

print("\nScheme Level Distribution:");
schemeDb.schemes.aggregate([{ $group: { _id: "$schemeLevel", count: { $sum: 1 } } }]).forEach(r => print(`  ${r._id}: ${r.count}`));

print("\nTop 10 Categories in Schemes:");
schemeDb.schemes.aggregate([
  { $group: { _id: "$category.name", code: { $first: "$category.code" }, count: { $sum: 1 } } },
  { $sort: { count: -1 } },
  { $limit: 10 }
]).forEach(r => print(`  ${r._id} (${r.code}): ${r.count}`));

print("\nTop 10 States/UTs in Schemes:");
schemeDb.schemes.aggregate([
  { $group: { _id: "$stateOrUt", count: { $sum: 1 } } },
  { $sort: { count: -1 } },
  { $limit: 10 }
]).forEach(r => print(`  ${r._id}: ${r.count}`));

print("\nBeneficiary Types in Schemes:");
schemeDb.schemes.aggregate([
  { $group: { _id: "$beneficiaryType", count: { $sum: 1 } } },
  { $sort: { count: -1 } }
]).forEach(r => print(`  ${r._id}: ${r.count}`));

// Array lengths
const arrStats = schemeDb.schemes.aggregate([
  {
    $project: {
      bLen: { $size: { $ifNull: ["$benefits", []] } },
      dLen: { $size: { $ifNull: ["$requiredDocuments", []] } },
      tLen: { $size: { $ifNull: ["$tags", []] } },
      fLen: { $size: { $ifNull: ["$faqs", []] } }
    }
  },
  {
    $group: {
      _id: null,
      avgBenefits: { $avg: "$bLen" },
      maxBenefits: { $max: "$bLen" },
      zeroBenefits: { $sum: { $cond: [{ $eq: ["$bLen", 0] }, 1, 0] } },
      avgDocs: { $avg: "$dLen" },
      maxDocs: { $max: "$dLen" },
      zeroDocs: { $sum: { $cond: [{ $eq: ["$dLen", 0] }, 1, 0] } },
      avgTags: { $avg: "$tLen" },
      maxTags: { $max: "$tLen" },
      zeroTags: { $sum: { $cond: [{ $eq: ["$tLen", 0] }, 1, 0] } },
      avgFaqs: { $avg: "$fLen" },
      maxFaqs: { $max: "$fLen" },
      zeroFaqs: { $sum: { $cond: [{ $eq: ["$fLen", 0] }, 1, 0] } }
    }
  }
]).toArray()[0];

print("\nSchemes Array Statistics:");
print(`  Benefits: avg=${arrStats.avgBenefits.toFixed(2)}, max=${arrStats.maxBenefits}, zero=${arrStats.zeroBenefits}`);
print(`  Documents: avg=${arrStats.avgDocs.toFixed(2)}, max=${arrStats.maxDocs}, zero=${arrStats.zeroDocs}`);
print(`  Tags: avg=${arrStats.avgTags.toFixed(2)}, max=${arrStats.maxTags}, zero=${arrStats.zeroTags}`);
print(`  Faqs: avg=${arrStats.avgFaqs.toFixed(2)}, max=${arrStats.maxFaqs}, zero=${arrStats.zeroFaqs}`);

// Eligibility Rules Inspection in schemes
const rulesStat = schemeDb.schemes.aggregate([
  {
    $project: {
      hasRules: { $cond: [{ $ifNull: ["$eligibilityRules", false] }, 1, 0] },
      hasConditions: { $cond: [{ $gt: [{ $size: { $ifNull: ["$eligibilityRules.conditions", []] } }, 0] }, 1, 0] },
      hasGroups: { $cond: [{ $gt: [{ $size: { $ifNull: ["$eligibilityRules.groups", []] } }, 0] }, 1, 0] }
    }
  },
  {
    $group: {
      _id: null,
      total: { $sum: 1 },
      withRules: { $sum: "$hasRules" },
      withConditions: { $sum: "$hasConditions" },
      withGroups: { $sum: "$hasGroups" }
    }
  }
]).toArray()[0];

print("\nSchemes Eligibility Rules Presence:");
print(`  Total: ${rulesStat.total}, Has eligibilityRules: ${rulesStat.withRules}, Has conditions: ${rulesStat.withConditions}, Has nested groups: ${rulesStat.withGroups}`);

// Inspect unique conditions fields
const conditionFields = schemeDb.schemes.aggregate([
  { $unwind: { path: "$eligibilityRules.conditions", preserveNullAndEmptyArrays: false } },
  { $group: { _id: "$eligibilityRules.conditions.field", count: { $sum: 1 } } },
  { $sort: { count: -1 } }
]).toArray();
print("\nUnique Eligibility Rule Condition Fields across schemes:");
conditionFields.forEach(c => print(`  field: ${c._id}, occurrences: ${c.count}`));

// 3. SCHEME_VERIFIED_DATA AUDIT
print("\n--- 3. SCHEME_VERIFIED_DATA AUDIT ---");
const totalVerified = schemeDb.scheme_verified_data.countDocuments();
print("Total scheme_verified_data: " + totalVerified);

print("\nDocument Status Distribution:");
schemeDb.scheme_verified_data.aggregate([{ $group: { _id: "$documentStatus", count: { $sum: 1 } } }]).forEach(r => print(`  ${r._id}: ${r.count}`));

print("\nReconciliation Status Distribution:");
schemeDb.scheme_verified_data.aggregate([{ $group: { _id: "$reconciliationStatus", count: { $sum: 1 } } }]).forEach(r => print(`  ${r._id}: ${r.count}`));

print("\nOverall Provenance Distribution:");
schemeDb.scheme_verified_data.aggregate([{ $group: { _id: "$overallProvenance", count: { $sum: 1 } } }]).forEach(r => print(`  ${r._id}: ${r.count}`));

// Document requirements inspection in scheme_verified_data
const docAgg = schemeDb.scheme_verified_data.aggregate([
  {
    $project: {
      docCount: { $size: { $ifNull: ["$documents", []] } },
      hasAlts: {
        $filter: {
          input: { $ifNull: ["$documents", []] },
          as: "d",
          cond: { $ne: ["$$d.alternativeGroup", null] }
        }
      }
    }
  },
  {
    $group: {
      _id: null,
      totalDocs: { $sum: "$docCount" },
      avgDocsPerScheme: { $avg: "$docCount" },
      schemesWithDocs: { $sum: { $cond: [{ $gt: ["$docCount", 0] }, 1, 0] } },
      schemesWithAlts: { $sum: { $cond: [{ $gt: [{ $size: "$hasAlts" }, 0] }, 1, 0] } }
    }
  }
]).toArray()[0];

print("\nCanonical Document Requirements Aggregation:");
print(`  Total Document Requirement Records: ${docAgg.totalDocs}`);
print(`  Avg Documents per Scheme: ${docAgg.avgDocsPerScheme.toFixed(2)}`);
print(`  Schemes with At Least 1 Document: ${docAgg.schemesWithDocs}`);
print(`  Schemes with Alternative/ONE_OF Groups: ${docAgg.schemesWithAlts}`);

// Sample 3 Canonical Document Requirements with ONE_OF
const oneOfSample = schemeDb.scheme_verified_data.aggregate([
  { $unwind: "$documents" },
  { $match: { "documents.alternativeGroup.rule": "ONE_OF" } },
  { $limit: 2 }
]).toArray();

print("\nSample ONE_OF Canonical Document Requirement:");
oneOfSample.forEach(s => {
  print(JSON.stringify({
    schemeCode: s.schemeCode,
    doc: s.documents
  }, null, 2));
});

// 4. CITIZEN PROFILES AUDIT
print("\n--- 4. CITIZEN PROFILES AUDIT ---");
const cpCountSchemeDb = schemeDb.citizen_profiles.countDocuments();
const cpCountRootDb = rootDb.citizen_profiles.countDocuments();
print(`schemebridge_scheme_db.citizen_profiles: ${cpCountSchemeDb}`);
print(`schemebridge.citizen_profiles: ${cpCountRootDb}`);

const profiles = schemeDb.citizen_profiles.find().toArray();
print("\nschemebridge_scheme_db.citizen_profiles attributes summary (count=" + profiles.length + "):");
const pFields = {};
profiles.forEach(p => {
  for (const k of Object.keys(p)) {
    pFields[k] = (pFields[k] || 0) + 1;
  }
});
for (const [k, v] of Object.entries(pFields).sort((a, b) => b[1] - a[1])) {
  print(`  ${k}: ${v}/${profiles.length} (${((v/profiles.length)*100).toFixed(0)}%)`);
}

// 5. APPLICATIONS AUDIT
print("\n--- 5. APPLICATIONS AUDIT ---");
print(`schemebridge_scheme_db.applications: ${schemeDb.applications.countDocuments()}`);
print(`schemebridge.applications: ${rootDb.applications.countDocuments()}`);
print(`schemebridge_application_db.applications: ${appDb.applications.countDocuments()}`);

const rootApps = rootDb.applications.find().toArray();
print(`\nschemebridge.applications details (count=${rootApps.length}):`);
rootApps.forEach((a, i) => {
  print(`  [${i+1}] id: ${a._id}, schemeCode: ${a.schemeCode || a.scheme_code}, status: ${a.status}, citizenId/userId: ${a.citizenId || a.citizen_id || a.userId}, createdAt: ${a.createdAt || a.created_at}`);
});

// 6. SCHEMECODE & SLUG RELATIONSHIPS
print("\n--- 6. SCHEMECODE & SLUG CROSS-COLLECTION RELATIONSHIPS ---");
const orphanSchemes = schemeDb.schemes.aggregate([
  {
    $lookup: {
      from: "scheme_verified_data",
      localField: "schemeCode",
      foreignField: "schemeCode",
      as: "verified"
    }
  },
  { $match: { "verified.0": { $exists: false } } },
  { $count: "orphanCount" }
]).toArray();
print("Schemes without scheme_verified_data entry: " + (orphanSchemes[0] ? orphanSchemes[0].orphanCount : 0));

const orphanVerified = schemeDb.scheme_verified_data.aggregate([
  {
    $lookup: {
      from: "schemes",
      localField: "schemeCode",
      foreignField: "schemeCode",
      as: "master"
    }
  },
  { $match: { "master.0": { $exists: false } } },
  { $count: "orphanCount" }
]).toArray();
print("scheme_verified_data entries without schemes master: " + (orphanVerified[0] ? orphanVerified[0].orphanCount : 0));

print("\nAudit script complete.");
