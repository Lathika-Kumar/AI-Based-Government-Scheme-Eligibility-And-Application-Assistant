export const EDUCATION_OPTIONS = [
  "Illiterate / No Formal Education",
  "Primary Education (Class 1-5)",
  "Middle School (Class 6-8)",
  "Secondary Education (Class 9-10)",
  "Higher Secondary (Class 11-12)",
  "Diploma / Vocational Training",
  "Graduate / Bachelor's Degree",
  "Postgraduate / Master's Degree",
  "Doctorate / Professional Degree"
];

export const OCCUPATION_OPTIONS = [
  "Farmer",
  "Agricultural Laborer",
  "Student",
  "Self-Employed / Shopkeeper",
  "Unemployed / Homemaker",
  "Salaried Employee (Private Sector)",
  "Salaried Employee (Public/Govt)",
  "Daily Wage Artisan / Worker",
  "Retired / Pensioner"
];

export const CASTE_OPTIONS = [
  "General",
  "OBC (Other Backward Class)",
  "SC (Scheduled Caste)",
  "ST (Scheduled Tribe)"
];

export const ACCESSIBILITY_OPTIONS = {
  fontSize: [
    { label: "Normal", value: "normal", class: "text-base" },
    { label: "Large", value: "large", class: "text-lg" },
    { label: "Extra Large", value: "xlarge", class: "text-xl" }
  ],
  highContrast: [
    { label: "Standard Colors", value: "standard" },
    { label: "High Contrast (Dark/Yellow)", value: "high-contrast" }
  ],
  audioGuidance: [
    { label: "Disabled", value: "disabled" },
    { label: "Enabled (Narrator Assistance)", value: "enabled" }
  ]
};

export const PROFILE_COMPLETION_WEIGHTS = {
  name: 15,
  age: 15,
  gender: 10,
  occupation: 15,
  annualIncome: 15,
  caste: 15,
  state: 10,
  education: 5
};

export function calculateCompletion(profile) {
  if (!profile) {
return 0;
}
  let totalScore = 0;

  if (profile.name && profile.name.trim() !== "") {
totalScore += PROFILE_COMPLETION_WEIGHTS.name;
}
  if (profile.age && Number(profile.age) > 0) {
totalScore += PROFILE_COMPLETION_WEIGHTS.age;
}
  if (profile.gender && profile.gender.trim() !== "") {
totalScore += PROFILE_COMPLETION_WEIGHTS.gender;
}
  if (profile.occupation && profile.occupation.trim() !== "") {
totalScore += PROFILE_COMPLETION_WEIGHTS.occupation;
}

  // Income can be 0 (e.g. students or unemployed), so we check if defined and not null/NaN
  const income = profile.annualIncome !== undefined ? profile.annualIncome : profile.income;
  if (income !== undefined && income !== null && !isNaN(Number(income))) {
totalScore += PROFILE_COMPLETION_WEIGHTS.annualIncome;
}

  if (profile.caste && profile.caste.trim() !== "") {
totalScore += PROFILE_COMPLETION_WEIGHTS.caste;
}
  if (profile.state && profile.state.trim() !== "") {
totalScore += PROFILE_COMPLETION_WEIGHTS.state;
}
  if (profile.education && profile.education.trim() !== "") {
totalScore += PROFILE_COMPLETION_WEIGHTS.education;
}

  return totalScore;
}

export const INDIAN_STATES_AND_UTS = [
  "Andhra Pradesh",
  "Arunachal Pradesh",
  "Assam",
  "Bihar",
  "Chhattisgarh",
  "Goa",
  "Gujarat",
  "Haryana",
  "Himachal Pradesh",
  "Jharkhand",
  "Karnataka",
  "Kerala",
  "Madhya Pradesh",
  "Maharashtra",
  "Manipur",
  "Meghalaya",
  "Mizoram",
  "Nagaland",
  "Odisha",
  "Punjab",
  "Rajasthan",
  "Sikkim",
  "Tamil Nadu",
  "Telangana",
  "Tripura",
  "Uttar Pradesh",
  "Uttarakhand",
  "West Bengal",
  "Andaman and Nicobar Islands",
  "Chandigarh",
  "Dadra and Nagar Haveli and Daman and Diu",
  "Delhi",
  "Jammu and Kashmir",
  "Ladakh",
  "Lakshadweep",
  "Puducherry"
].sort();
