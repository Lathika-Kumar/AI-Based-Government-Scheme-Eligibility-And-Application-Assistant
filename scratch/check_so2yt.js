const db = db.getSiblingDB("schemebridge_scheme_db");
const doc = db.scheme_verified_data.findOne({ schemeCode: "SO2YT5YLM" });
if (doc) {
  print("Found SO2YT5YLM in scheme_verified_data!");
  print("Document count: " + (doc.documents ? doc.documents.length : 0));
  if (doc.documents) {
    doc.documents.forEach((d, i) => {
      print(`Doc [${i+1}]: name=${d.officialDocumentName}, altRule=${d.alternativeGroup ? d.alternativeGroup.rule : 'null'}, opts=${d.alternativeGroup ? d.alternativeGroup.options.length : 0}`);
    });
  }
} else {
  print("SO2YT5YLM NOT found in scheme_verified_data");
}
