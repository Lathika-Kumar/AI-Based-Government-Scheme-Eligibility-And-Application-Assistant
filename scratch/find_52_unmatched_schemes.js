const { MongoClient } = require('mongodb');
const fs = require('fs');
const path = require('path');

const URI = "mongodb://schemebridge_user:31Rfp1OuAafIzBhm1Xu6G5p7@localhost:27017/schemebridge_scheme_db?authSource=admin";

async function run() {
  const client = new MongoClient(URI);
  try {
    await client.connect();
    const db = client.db('schemebridge_scheme_db');

    // 1. Get all canonical scheme codes
    const canonicalCodes = new Set(await db.collection('scheme_verified_data').distinct('schemeCode'));
    console.log(`Total canonical scheme codes: ${canonicalCodes.size}`);

    // 2. Find all schemes in schemes collection not in canonicalCodes
    const allSchemes = await db.collection('schemes').find({}).toArray();
    console.log(`Total schemes collection count: ${allSchemes.length}`);

    const unmatched = allSchemes.filter(s => !canonicalCodes.has(s.schemeCode));
    console.log(`Total unmatched schemes: ${unmatched.length}`);

    const formattedList = unmatched.map(s => ({
      _id: s._id.toString(),
      schemeCode: s.schemeCode,
      slug: s.slug,
      name: s.title?.english || s.title?.hindi || s.name || s.schemeCode,
      categoryCode: s.categoryCode,
      ministry: s.ministry,
      department: s.department,
      schemeLevel: s.schemeLevel,
      stateOrUt: s.stateOrUt,
      beneficiaryType: s.beneficiaryType,
      schemeType: s.schemeType,
      status: s.status,
      benefitsCount: Array.isArray(s.benefits) ? s.benefits.length : 0,
      tagsCount: Array.isArray(s.tags) ? s.tags.length : 0,
      createdAt: s.createdAt,
      updatedAt: s.updatedAt,
      source: s.source,
      version: s.version
    }));

    fs.writeFileSync(
      path.join(__dirname, '52_unmatched_schemes.json'),
      JSON.stringify(formattedList, null, 2),
      'utf-8'
    );
    console.log(`✓ Saved 52 unmatched schemes to scratch/52_unmatched_schemes.json`);
  } finally {
    await client.close();
  }
}

run().catch(console.error);
