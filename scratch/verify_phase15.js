const schemesCount = db.schemes.countDocuments();
const canonicalCount = db.scheme_verified_data.countDocuments();

let totalBenefits = 0;
let totalTags = 0;

db.schemes.find({}, { benefits: 1, tags: 1 }).forEach(s => {
  if (Array.isArray(s.benefits)) totalBenefits += s.benefits.length;
  if (Array.isArray(s.tags)) totalTags += s.tags.length;
});

// Check duplicate schemeCodes and slugs in master schemes
const dupCodes = db.schemes.aggregate([
  { $group: { _id: "$schemeCode", count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } }
]).toArray();

const dupSlugs = db.schemes.aggregate([
  { $group: { _id: "$slug", count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } }
]).toArray();

const so2yt5ylm = db.scheme_verified_data.findOne({ schemeCode: 'SO2YT5YLM' });

print('=== PHASE 15 DATABASE INVARIANTS ===');
print(`Master Schemes Count: ${schemesCount}`);
print(`Master Embedded Benefits Count: ${totalBenefits}`);
print(`Master Embedded Tags Count: ${totalTags}`);
print(`Canonical Records (scheme_verified_data) Count: ${canonicalCount}`);
print(`Duplicate Scheme Codes: ${dupCodes.length}`);
print(`Duplicate Slugs: ${dupSlugs.length}`);
print(`SO2YT5YLM Canonical Record Found: ${!!so2yt5ylm}`);

if (so2yt5ylm) {
  print(`  Status: ${so2yt5ylm.documentStatus}`);
  print(`  Provenance: ${so2yt5ylm.overallProvenance}`);
  print(`  Official Source URL: ${so2yt5ylm.sourceMetadata ? so2yt5ylm.sourceMetadata.sourceUrl : 'N/A'}`);
  print(`  Total Canonical Documents: ${so2yt5ylm.documents ? so2yt5ylm.documents.length : 0}`);
}
