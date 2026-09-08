const sample = db.scheme_verified_data.find({'documents.officialDocumentName': {$regex: '/| or |Any one', $options: 'i'}}, {schemeCode: 1, 'documents.officialDocumentName': 1}).limit(10).toArray();

sample.forEach(s => {
  print(`\nScheme: ${s.schemeCode}`);
  s.documents.forEach(d => {
    if (d.officialDocumentName && (d.officialDocumentName.includes('/') || d.officialDocumentName.toLowerCase().includes(' or '))) {
      print(`  -> ${d.officialDocumentName}`);
    }
  });
});
