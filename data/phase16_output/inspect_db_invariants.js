const schemeCount = db.schemes.countDocuments();
const verifiedCount = db.scheme_verified_data.countDocuments();

let totalBenefits = 0;
db.schemes.find({}, { benefits: 1 }).forEach(doc => {
  if (doc.benefits && Array.isArray(doc.benefits)) {
    totalBenefits += doc.benefits.length;
  }
});

let totalTags = 0;
db.schemes.find({}, { tags: 1 }).forEach(doc => {
  if (doc.tags && Array.isArray(doc.tags)) {
    totalTags += doc.tags.length;
  }
});

const dupCode = db.schemes.aggregate([
  { $group: { _id: '$schemeCode', count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } }
]).toArray().length;

const dupSlug = db.schemes.aggregate([
  { $group: { _id: '$slug', count: { $sum: 1 } } },
  { $match: { count: { $gt: 1 } } }
]).toArray().length;

const so2yt5ylm = db.scheme_verified_data.findOne({ schemeCode: 'SO2YT5YLM' });
let so2yt5ylmDocCount = 0;
let oneOfGroups = 0;
let mandatoryDocs = 0;
if (so2yt5ylm && so2yt5ylm.documents) {
  so2yt5ylmDocCount = so2yt5ylm.documents.length;
  so2yt5ylm.documents.forEach(d => {
    if (d.groupType === 'ONE_OF') oneOfGroups++;
    if (d.groupType === 'STANDALONE' && d.mandatory) mandatoryDocs++;
  });
}

const verifiedCodes = {};
db.scheme_verified_data.find({}, { schemeCode: 1 }).forEach(d => {
  if (d.schemeCode) verifiedCodes[d.schemeCode] = true;
});

const onlyMasterSchemes = [];
db.schemes.find({}, { schemeCode: 1, slug: 1, title: 1 }).forEach(s => {
  if (!verifiedCodes[s.schemeCode]) {
    onlyMasterSchemes.push({ schemeCode: s.schemeCode, slug: s.slug, title: s.title?.english });
  }
});

print(JSON.stringify({
  schemeCount,
  verifiedCount,
  difference: schemeCount - verifiedCount,
  totalBenefits,
  totalTags,
  dupCode,
  dupSlug,
  so2yt5ylm: {
    exists: !!so2yt5ylm,
    schemeCode: so2yt5ylm?.schemeCode,
    slug: so2yt5ylm?.slug,
    documentStatus: so2yt5ylm?.documentStatus,
    overallProvenance: so2yt5ylm?.overallProvenance,
    docCount: so2yt5ylmDocCount,
    oneOfGroups,
    mandatoryDocs
  },
  onlyMasterSchemesCount: onlyMasterSchemes.length,
  onlyMasterSchemesSample: onlyMasterSchemes.slice(0, 5)
}, null, 2));
