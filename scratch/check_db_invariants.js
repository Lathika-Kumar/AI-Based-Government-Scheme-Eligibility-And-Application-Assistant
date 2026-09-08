const { MongoClient } = require('mongodb');

const uri = "mongodb://schemebridge_user:31Rfp1OuAafIzBhm1Xu6G5p7@localhost:27017/schemebridge_scheme_db?authSource=admin";

async function check() {
  const client = new MongoClient(uri);
  try {
    await client.connect();
    const db = client.db('schemebridge_scheme_db');

    const schemesCount = await db.collection('schemes').countDocuments();
    const verifiedCount = await db.collection('scheme_verified_data').countDocuments();
    
    // Check duplicates
    const dupCodes = await db.collection('schemes').aggregate([
      { $group: { _id: "$schemeCode", count: { $sum: 1 } } },
      { $match: { count: { $gt: 1 } } }
    ]).toArray();

    const dupSlugs = await db.collection('schemes').aggregate([
      { $group: { _id: "$slug", count: { $sum: 1 } } },
      { $match: { count: { $gt: 1 } } }
    ]).toArray();

    const agg = await db.collection('schemes').aggregate([
      { $project: { bCount: { $size: { $ifNull: ["$benefits", []] } }, tCount: { $size: { $ifNull: ["$tags", []] } } } },
      { $group: { _id: null, totalBenefits: { $sum: "$bCount" }, totalTags: { $sum: "$tCount" } } }
    ]).toArray();

    console.log("=== MONGODB INVARIANTS CHECK ===");
    console.log("schemes count:              ", schemesCount);
    console.log("scheme_verified_data count: ", verifiedCount);
    console.log("difference:                 ", schemesCount - verifiedCount);
    console.log("duplicate schemeCodes:      ", dupCodes.length);
    console.log("duplicate slugs:            ", dupSlugs.length);
    console.log("embedded benefits:          ", agg[0]?.totalBenefits);
    console.log("embedded tags:              ", agg[0]?.totalTags);

    // Also check canonical document validation scheme: SO2YT5YLM
    const docReq = await db.collection('scheme_verified_data').findOne({ schemeCode: "SO2YT5YLM" });
    if (docReq) {
      console.log("SO2YT5YLM canonical doc count: ", docReq.documents?.length || docReq.canonicalDocuments?.length);
      console.log("SO2YT5YLM status:              ", docReq.status || docReq.verificationStatus);
    }
  } finally {
    await client.close();
  }
}

check().catch(console.error);
