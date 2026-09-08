const totalSchemes = db.schemes.countDocuments();
const verifiedCount = db.scheme_verified_data.countDocuments();
const dupCodes = db.schemes.aggregate([
  { $group: { _id: "$schemeCode", count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } }
]).toArray();
const dupSlugs = db.schemes.aggregate([
  { $group: { _id: "$slug", count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } }
]).toArray();
const agg = db.schemes.aggregate([
  { $project: { bCount: { $size: { $ifNull: ["$benefits", []] } }, tCount: { $size: { $ifNull: ["$tags", []] } } } },
  { $group: { _id: null, totalBenefits: { $sum: "$bCount" }, totalTags: { $sum: "$tCount" } } }
]).toArray();

print("=== MONGODB INVARIANTS AUDIT ===");
print("Total Schemes:          " + totalSchemes);
print("Scheme Verified Data:   " + verifiedCount);
print("Difference (Seed-Only): " + (totalSchemes - verifiedCount));
print("Duplicate schemeCodes:  " + dupCodes.length);
print("Duplicate slugs:        " + dupSlugs.length);
print("Embedded Benefits:      " + agg[0]?.totalBenefits);
print("Embedded Tags:          " + agg[0]?.totalTags);

const valScheme = db.scheme_verified_data.findOne({ schemeCode: "SO2YT5YLM" });
if (valScheme) {
  print("SO2YT5YLM canonical doc count: " + (valScheme.documents?.length || valScheme.canonicalDocuments?.length));
}
