const db = db.getSiblingDB("schemebridge_scheme_db");

const sample = db.scheme_verified_data.findOne({ "documents.alternativeGroup": { $ne: null } });
if (sample) {
  print("Found scheme with alt group: " + sample.schemeCode);
  const altDoc = sample.documents.find(d => d.alternativeGroup !== null && d.alternativeGroup !== undefined);
  print(JSON.stringify(altDoc, null, 2));
} else {
  print("No scheme with alt group found");
}
