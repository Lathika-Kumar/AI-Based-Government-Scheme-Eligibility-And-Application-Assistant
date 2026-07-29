export const MOCK_NOTIFICATIONS = [
  {
    id: "notif-1",
    type: "deadline",
    title: "Upcoming Deadline Alert",
    text: "Atal Pension Yojana application window closes in 4 days. You have all documents verified.",
    time: "2 hours ago",
    urgency: "high",
    isRead: false
  },
  {
    id: "notif-2",
    type: "document",
    title: "DigiLocker Verification Success",
    text: "Your Land Ownership Record (Khatauni) has been successfully verified.",
    time: "5 hours ago",
    urgency: "medium",
    isRead: false
  },
  {
    id: "notif-3",
    type: "match",
    title: "New Matching Schemes Found",
    text: "Based on your updated OBC category status, 3 new schemes have been recommended for you.",
    time: "1 day ago",
    urgency: "low",
    isRead: true
  },
  {
    id: "notif-4",
    type: "system",
    title: "Portal Maintenance Notice",
    text: "SchemeBridge will undergo brief maintenance on June 30th at 12:00 AM IST.",
    time: "2 days ago",
    urgency: "low",
    isRead: true
  }
];

export const MOCK_RECENT_ACTIVITIES = [
  {
    id: "act-1",
    title: "Application Status Update",
    description: "Atal Pension Yojana (APY) application status changed to 'Under Review'",
    timestamp: "2026-06-26T15:30:00Z",
    type: "status_change",
    ip: "152.168.1.42"
  },
  {
    id: "act-2",
    title: "Document Verified",
    description: "Aadhaar Card successfully validated against national identity registry",
    timestamp: "2026-06-26T10:15:00Z",
    type: "doc_verification",
    ip: "152.168.1.42"
  },
  {
    id: "act-3",
    title: "Document Uploaded",
    description: "Land Ownership Record (Khatauni) uploaded to vault via DigiLocker",
    timestamp: "2026-06-25T18:45:00Z",
    type: "doc_upload",
    ip: "152.168.1.42"
  },
  {
    id: "act-4",
    title: "Profile Synchronized",
    description: "Socio-economic profile criteria updated",
    timestamp: "2026-06-25T11:20:00Z",
    type: "profile_update",
    ip: "152.168.1.42"
  },
  {
    id: "act-5",
    title: "Scheme Bookmarked",
    description: "Pradhan Mantri Kisan Samman Nidhi (PM-KISAN) added to saved schemes list",
    timestamp: "2026-06-24T09:05:00Z",
    type: "bookmark",
    ip: "152.168.1.33"
  }
];

export const MOCK_QUICK_ACTIONS = [
  {
    id: "qa-recommendations",
    label: "Explore Schemes",
    description: "Browse customized matches",
    path: "/recommendations",
    color: "from-indigo-500 to-indigo-650",
    hoverBg: "hover:bg-indigo-50"
  },
  {
    id: "qa-documents",
    label: "Document Vault",
    description: "Upload & verify documents",
    path: "/documents",
    color: "from-emerald-500 to-emerald-650",
    hoverBg: "hover:bg-emerald-50"
  },
  {
    id: "qa-profile",
    label: "Update Profile",
    description: "Improve match accuracy",
    path: "/profile",
    color: "from-amber-500 to-amber-650",
    hoverBg: "hover:bg-amber-50"
  },
  {
    id: "qa-tracker",
    label: "Track Progress",
    description: "Monitor active files",
    path: "/tracker",
    color: "from-violet-500 to-violet-650",
    hoverBg: "hover:bg-violet-50"
  }
];
