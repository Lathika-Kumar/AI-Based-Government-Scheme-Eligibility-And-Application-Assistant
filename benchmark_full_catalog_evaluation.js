const totalSchemes = db.schemes.countDocuments();
const centralSchemes = db.schemes.countDocuments({ schemeLevel: 'CENTRAL' });
const stateSchemes = db.schemes.countDocuments({ schemeLevel: 'STATE' });

console.log('=== Performance & Catalog Invariant Verification ===');
console.log('  Total Catalog Schemes:', totalSchemes);
console.log('  Central Schemes:      ', centralSchemes);
console.log('  State Schemes:        ', stateSchemes);

// Test citizen profiles
const testProfiles = [
  {
    name: 'Profile A: Farmer in Maharashtra (User 65)',
    state: 'Maharashtra',
    age: 34,
    annualIncome: 160000,
    socialCategory: 'OBC',
    occupation: 'Farmer',
    isFarmer: true
  },
  {
    name: 'Profile B: Student in Tamil Nadu',
    state: 'Tamil Nadu',
    age: 21,
    annualIncome: 120000,
    socialCategory: 'SC',
    occupation: 'Student',
    isStudent: true
  },
  {
    name: 'Profile C: Low-Income Artisan in Gujarat',
    state: 'Gujarat',
    age: 45,
    annualIncome: 90000,
    socialCategory: 'General',
    occupation: 'Artisan',
    bplStatus: true
  }
];

testProfiles.forEach(p => {
  const start = new Date();
  
  // Indexed candidate pre-filtering query:
  const candidates = db.schemes.find({
    status: 'ACTIVE',
    $or: [
      { schemeLevel: 'CENTRAL' },
      { stateOrUt: new RegExp(`^${p.state.trim()}$`, 'i') },
      { stateOrUt: null },
      { stateOrUt: 'ALL' }
    ]
  }).toArray();
  
  const queryDuration = new Date() - start;
  
  console.log(`\nCitizen: ${p.name}`);
  console.log(`  Candidate Schemes Filtered: ${candidates.length} / ${totalSchemes}`);
  console.log(`  MongoDB Query Latency:      ${queryDuration} ms`);
});
