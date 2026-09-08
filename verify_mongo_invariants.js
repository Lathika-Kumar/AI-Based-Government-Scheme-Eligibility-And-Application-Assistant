const totalSchemes = db.schemes.countDocuments();
const centralSchemes = db.schemes.countDocuments({ schemeLevel: 'CENTRAL' });
const stateSchemes = db.schemes.countDocuments({ schemeLevel: 'STATE' });
const agg = db.schemes.aggregate([
  { $project: { bCount: { $size: { $ifNull: ["$benefits", []] } }, tCount: { $size: { $ifNull: ["$tags", []] } } } },
  { $group: { _id: null, totalBenefits: { $sum: "$bCount" }, totalTags: { $sum: "$tCount" } } }
]).toArray();

console.log('=== MongoDB schemebridge_scheme_db Reconciliation ===');
console.log('  Total Schemes:          ', totalSchemes);
console.log('  Central Schemes:        ', centralSchemes);
console.log('  State Schemes:          ', stateSchemes);
console.log('  Total Embedded Benefits:', agg[0]?.totalBenefits);
console.log('  Total Embedded Tags:    ', agg[0]?.totalTags);

if (totalSchemes === 4682 && agg[0]?.totalBenefits === 35560 && agg[0]?.totalTags === 22500) {
  console.log('STATUS: PERFECT MATCH (0 REGRESSIONS)');
} else {
  console.error('STATUS: MISMATCH DETECTED!');
}
