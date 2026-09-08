const db = db.getSiblingDB("schemebridge_scheme_db");
const count = db.scheme_verified_data.aggregate([
  { $unwind: "$documents" },
  { $match: { "documents.alternativeGroup": { $ne: null } } },
  { $count: "count" }
]).toArray();
print("Count of documents with alternativeGroup != null: " + JSON.stringify(count));

const rules = db.scheme_verified_data.aggregate([
  { $unwind: "$documents" },
  { $match: { "documents.alternativeGroup": { $ne: null } } },
  { $group: { _id: "$documents.alternativeGroup.rule", count: { $sum: 1 } } }
]).toArray();
print("Rules: " + JSON.stringify(rules));
