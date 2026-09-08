const db = db.getSiblingDB("schemebridge_scheme_db");
const sample = db.scheme_verified_data.findOne({ schemeCode: "PM_KISAN_2026" });
if (sample) {
  print("PM_KISAN_2026 documents count: " + (sample.documents ? sample.documents.length : 0));
  print(JSON.stringify(sample.documents, null, 2));
}
