const db = db.getSiblingDB("schemebridge_scheme_db");

const totalWithAltGroup = db.scheme_verified_data.countDocuments({ "documents.alternativeGroup": { $ne: null } });
print("Total schemes with documents.alternativeGroup != null: " + totalWithAltGroup);

// Check if any officialDocumentName or sourceEvidence contains ' / ' or ' or '
const altTextMatches = db.scheme_verified_data.aggregate([
  { $unwind: "$documents" },
  { $match: {
      $or: [
        { "documents.officialDocumentName": { $regex: " / | or | either | any one | Voter ID/Passport", $options: "i" } },
        { "documents.sourceEvidence": { $regex: " / | or | either | any one ", $options: "i" } }
      ]
    }
  },
  { $limit: 10 },
  { $project: { schemeCode: 1, docName: "$documents.officialDocumentName", evidence: "$documents.sourceEvidence" } }
]).toArray();

print("Sample text-based alternative documents found in dataset: " + altTextMatches.length);
altTextMatches.forEach(m => print(JSON.stringify(m)));

// Check canonical document codes distribution
print("\nTop 20 Canonical Document Codes in scheme_verified_data:");
db.scheme_verified_data.aggregate([
  { $unwind: "$documents" },
  { $group: { _id: "$documents.canonicalDocumentCode", count: { $sum: 1 } } },
  { $sort: { count: -1 } },
  { $limit: 20 }
]).forEach(r => print("  " + r._id + ": " + r.count));

// Check missing fields in documents
print("\nDocuments Field Completeness in scheme_verified_data (total docs = 6389):");
const docStats = db.scheme_verified_data.aggregate([
  { $unwind: "$documents" },
  {
    $group: {
      _id: null,
      total: { $sum: 1 },
      hasOfficialName: { $sum: { $cond: [{ $ifNull: ["$documents.officialDocumentName", false] }, 1, 0] } },
      hasCanonicalCode: { $sum: { $cond: [{ $ifNull: ["$documents.canonicalDocumentCode", false] }, 1, 0] } },
      hasDescription: { $sum: { $cond: [{ $ifNull: ["$documents.description", false] }, 1, 0] } },
      hasIssuingAuthority: { $sum: { $cond: [{ $ifNull: ["$documents.issuingAuthority", false] }, 1, 0] } },
      hasSourceUrl: { $sum: { $cond: [{ $ifNull: ["$documents.sourceUrl", false] }, 1, 0] } },
      hasSourceEvidence: { $sum: { $cond: [{ $ifNull: ["$documents.sourceEvidence", false] }, 1, 0] } },
      hasProvenance: { $sum: { $cond: [{ $ifNull: ["$documents.provenance", false] }, 1, 0] } },
      mandatoryTrue: { $sum: { $cond: [{ $eq: ["$documents.mandatory", true] }, 1, 0] } },
      optionalTrue: { $sum: { $cond: [{ $eq: ["$documents.optional", true] }, 1, 0] } }
    }
  }
]).toArray()[0];
print(JSON.stringify(docStats, null, 2));
