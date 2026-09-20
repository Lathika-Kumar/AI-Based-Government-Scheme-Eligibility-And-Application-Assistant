/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useRef, useCallback } from "react";
import { DEFAULT_DOCUMENTS } from "../data/mockDocuments";
import { useAuth } from "./AuthContext";
import { useToast } from "../components/ui/ToastNotification";
import profileService from "../services/profileService";
import notificationService from "../services/notificationService";
import { getSchemes } from "../services/schemeService";
import * as grievanceService from "../services/grievanceService";
import * as feedbackService from "../services/feedbackService";
import { safeGetItem, safeSetItem, cleanupLegacySchemeCache } from "../utils/storage";
import applicationService from "../services/applicationService";
import adminService from "../services/adminService";
import * as documentService from "../services/documentService";
import { findApplicationForScheme } from "../utils/applicationStateMapping";

export const AppContext = createContext(null);

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error("useApp must be used within an AppProvider");
  }
  return context;
};

// Simple unique ID generator
const generateId = () => `APP-${  Math.floor(1000 + Math.random() * 9000)}`;

// ── Default initial documents ─────────────────────────────────────────────
// Loaded from mockDocuments.js
const DEFAULT_AUDIT_LOGS = [
  {
    id: "LOG-1001",
    timestamp: "2026-06-21T08:30:00Z",
    actionType: "Create",
    entityType: "Scheme",
    entityName: "Pradhan Mantri Ujjwala Yojana (PMUY)",
    actor: "Sanjay Kumar (Admin)",
    detail: "Scheme record added in Draft mode with max income ceiling ₹100,000.",
  },
  {
    id: "LOG-1002",
    timestamp: "2026-06-21T08:35:00Z",
    actionType: "Publish",
    entityType: "Scheme",
    entityName: "Pradhan Mantri Ujjwala Yojana (PMUY)",
    actor: "Sanjay Kumar (Admin)",
    detail: "Scheme status transitioned to Published after completing all quality checklist requirements.",
  },
  {
    id: "LOG-1003",
    timestamp: "2026-06-21T08:45:00Z",
    actionType: "Status Change",
    entityType: "Application",
    entityName: "Atal Pension Yojana (APY)",
    actor: "Sanjay Kumar (Admin)",
    detail: "Application APP-9023 transitioned from Submitted to Under Review.",
  },
];

const DEFAULT_GRIEVANCES = [
  {
    id: "GRV-7401",
    citizenName: "Rajesh Patel",
    citizenPhone: "9876543210",
    citizenEmail: "rajesh.patel@gmail.com",
    relatedScheme: "Pradhan Mantri Kisan Samman Nidhi (PM-KISAN)",
    category: "Payment Delayed",
    description: "Installment for April 2026 has not been credited to my bank account. The tracker status shows approved but bank transfer is pending.",
    supportingNote: "Bank branch manager says Aadhaar linking is perfect.",
    status: "In Review",
    date: "2026-06-18",
  },
  {
    id: "GRV-5219",
    citizenName: "Rajesh Patel",
    citizenPhone: "9876543210",
    citizenEmail: "rajesh.patel@gmail.com",
    relatedScheme: "Atal Pension Yojana (APY)",
    category: "Document Verification",
    description: "My auto-debit registration was rejected. Uploaded my bank passbook again.",
    supportingNote: "Please expedite approval.",
    status: "Resolved",
    date: "2026-06-16",
  }
];

const DEFAULT_FEEDBACK = [
  {
    id: "FB-1001",
    citizenName: "Rajesh Patel",
    citizenEmail: "rajesh.patel@gmail.com",
    type: "Portal Rating",
    rating: 5,
    comment: "Excellent portal! Very easy to use and AI recommendations are accurate.",
    date: "2026-06-20",
    status: "Acknowledged"
  },
  {
    id: "FB-1002",
    citizenName: "Sunita Sharma",
    citizenEmail: "sunita@demo.com",
    type: "Scheme Suggestion",
    rating: 4,
    comment: "Would love to see more educational schemes for girl students.",
    relatedScheme: "Education Schemes",
    date: "2026-06-19",
    status: "Under Review"
  }
];

// ── Default initial applications (empty for real citizen workflows) ─────────
const DEFAULT_APPLICATIONS = [];


const DEFAULT_RECENT_ACTIVITIES = [
  {
    id: "act-1",
    officerName: "Priya Sharma",
    module: "Applications",
    activityType: "Approved Application",
    description: "Application APP-1042 was verified and approved.",
    relatedEntityId: "APP-1042",
    timestamp: "5 mins ago",
    status: "Success"
  },
  {
    id: "act-2",
    officerName: "Amit Singh",
    module: "Documents",
    activityType: "Document Flagged",
    description: "Aadhaar Card uploaded by Rajesh Patel flagged for OCR warning.",
    relatedEntityId: "DOC-9023",
    timestamp: "45 mins ago",
    status: "Warning"
  },
  {
    id: "act-3",
    officerName: "Sanjay Kumar",
    module: "Schemes",
    activityType: "New Scheme Drafted",
    description: "Created new scheme draft for Pradhan Mantri Awas Yojana.",
    relatedEntityId: "pm-awas-yojana",
    timestamp: "2 hours ago",
    status: "Success"
  },
  {
    id: "act-4",
    officerName: "Priya Patel",
    module: "Grievances",
    activityType: "Grievance Resolved",
    description: "Ticket GRV-5219 marked as resolved.",
    relatedEntityId: "GRV-5219",
    timestamp: "4 hours ago",
    status: "Success"
  },
  {
    id: "act-5",
    officerName: "System",
    module: "Security",
    activityType: "MFA Check Triggered",
    description: "System-wide security policy enforced: Aadhaar OTP-based authentication.",
    relatedEntityId: "SEC-1002",
    timestamp: "Yesterday",
    status: "Info"
  }
];

const DEFAULT_USERS_REGISTRY = {
  citizens: [
    { name: "Rajesh Patel", email: "citizen@demo.com", phone: "9876543210", state: "Gujarat", occupation: "Farmer", status: "Active", lastLogin: "2 hours ago", workload: "3 applications submitted" },
    { name: "Aravind Swamy", email: "aravind@demo.com", phone: "9876543211", state: "Tamil Nadu", occupation: "Self-Employed", status: "Active", lastLogin: "1 day ago", workload: "1 application in review" },
    { name: "Sunita Sharma", email: "sunita@demo.com", phone: "9876543212", state: "Uttar Pradesh", occupation: "Student", status: "Active", lastLogin: "10 mins ago", workload: "2 applications approved" },
    { name: "Vikram Singh", email: "vikram@demo.com", phone: "9876543213", state: "Maharashtra", occupation: "Unemployed", status: "Suspended", lastLogin: "3 days ago", workload: "No active applications" }
  ],
  officers: [
    { name: "Amit Singh (Verification)", email: "verify@schemebridge.gov.in", phone: "9876543214", dept: "Document Verification Directorate", status: "Active", lastLogin: "5 mins ago", workload: "12 applications in queue" },
    { name: "Karan Johar", email: "karan@schemebridge.gov.in", phone: "9876543215", dept: "Aadhaar Audit Division", status: "Active", lastLogin: "4 hours ago", workload: "8 applications in queue" }
  ],
  managers: [
    { name: "Neha Sharma (Schemes)", email: "schemes@schemebridge.gov.in", phone: "9876543216", dept: "Ministry of Social Welfare", status: "Active", lastLogin: "12 mins ago", workload: "Managing 5 active schemes" },
    { name: "Suresh Prabhu", email: "suresh@schemebridge.gov.in", phone: "9876543217", dept: "Ministry of Agriculture", status: "Active", lastLogin: "2 days ago", workload: "Managing 2 active schemes" }
  ],
  support: [
    { name: "Priya Patel", email: "priya@schemebridge.gov.in", phone: "9876543218", dept: "Public Relations & Grievances", status: "Active", lastLogin: "1 hour ago", workload: "8 tickets assigned" },
    { name: "Ravi Shankar", email: "ravi@schemebridge.gov.in", phone: "9876543219", dept: "Call Support Desk", status: "Active", lastLogin: "30 mins ago", workload: "14 tickets assigned" }
  ],
  admins: [
    { name: "Sanjay Kumar (Admin)", email: "admin@schemebridge.gov.in", phone: "9876543220", dept: "Govt. Scheme Evaluation Board", status: "Active", lastLogin: "Just now", workload: "Super privileges" }
  ]
};

export const AppProvider = ({ children }) => {
  const { user } = useAuth();
  const { showToast } = useToast();
  const lastSyncedEmail = useRef(user?.email || "");
  const lastSyncedDocsEmail = useRef(user?.email || "");

  // ── Citizen Profile ───────────────────────────────────────────────────────
  const [profile, setProfile] = useState(() => ({
    name: "",
    dob: "",
    age: "",
    gender: "",
    occupation: "",
    annualIncome: "",
    caste: "",
    state: "",
    education: "",
    disabilityStatus: false,
    isComplete: false,
  }));

  // ── Schemes Database ──────────────────────────────────────────────────────
  const [schemes, setSchemes] = useState([]);

  useEffect(() => {
    let isMounted = true;
    getSchemes()
      .then((res) => {
        if (isMounted && !res.error && res.data) {
          const raw = Array.isArray(res.data) ? res.data : (res.data?.content || []);
          const normalized = raw.map((s) => ({
            id: s.id || s.schemeCode,
            schemeCode: s.schemeCode,
            name: s.title?.english || s.title?.en || s.name || s.schemeCode || "Untitled Scheme",
            ministry: s.ministry || (s.schemeLevel === "CENTRAL" ? "Central Government" : "State Government"),
            department: s.department || "—",
            category: s.category?.name || s.categoryCode || s.category || "General",
            status: s.status ? String(s.status).toLowerCase() : "draft",
            description: s.shortDescription?.english || s.description?.english || s.description || "—",
            officialLink: s.source?.sourceUrl || s.applicationInfo?.applicationUrl || s.officialSourceUrl || s.applicationInfo?.officialPortalUrl || s.officialLink || "—",
            sourceType: s.schemeLevel === "STATE" ? "State" : "Central",
            benefits: Array.isArray(s.benefits) ? s.benefits.map((b) => typeof b === "string" ? b : b.description?.english || b.benefitType) : [],
            requiredDocuments: Array.isArray(s.requiredDocuments) ? s.requiredDocuments.map((d) => typeof d === "string" ? d : d.documentName?.english || d.documentCode) : [],
            deadline: s.applicationInfo?.closingDate || s.deadline || "—",
            approvalRate: typeof s.approvalRate === "number" ? s.approvalRate : null,
            lastUpdated: s.updatedAt ? new Date(s.updatedAt).toISOString().split("T")[0] : (s.createdAt ? new Date(s.createdAt).toISOString().split("T")[0] : "—")
          }));
          setSchemes(normalized);
        }
      })
      .catch(() => {});
    return () => { isMounted = false; };
  }, []);

  // ── Run storage migration once on AppContext mount ───────────────────────
  useEffect(() => {
    cleanupLegacySchemeCache();
  }, []);

  // ── Applications ──────────────────────────────────────────────────────────
  const [applications, setApplications] = useState(() => {
    return safeGetItem("schemebridge_applications", DEFAULT_APPLICATIONS);
  });

  const userRoleKey = `${user?.id || ""}_${user?.role || ""}_${user?.isAdmin ? "1" : "0"}_${(user?.roles || []).join(",")}`;

  const refreshApplications = useCallback(async () => {
    if (!user) return;
    try {
      const isStaffUser = Boolean(
        user.isAdmin ||
        ["admin", "scheme_manager", "verification_officer", "administrator"].includes(String(user.role || "").toLowerCase()) ||
        (Array.isArray(user.roles) && user.roles.some((r) =>
          [
            "ROLE_ADMIN", "ADMIN", "ROLE_ADMINISTRATOR", "ADMINISTRATOR",
            "ROLE_SUPER_ADMIN", "SUPER_ADMIN",
            "ROLE_SCHEME_MANAGER", "SCHEME_MANAGER",
            "ROLE_VERIFICATION_OFFICER", "VERIFICATION_OFFICER"
          ].includes(String(r).toUpperCase())
        ))
      );
      if (isStaffUser) {
        const res = await adminService.getApplicationsQueue({ page: 0, size: 500 });
        if (!res.error && res.data) {
          const list = Array.isArray(res.data) ? res.data : (res.data.content || []);
          setApplications(list);
          safeSetItem("schemebridge_applications", list);
          return list;
        }
      } else {
        const res = await applicationService.getMyApplications();
        if (!res.error && Array.isArray(res.data)) {
          setApplications(res.data);
          safeSetItem("schemebridge_applications", res.data);
          return res.data;
        }
      }
    } catch (err) {
      console.warn("Failed to load applications from backend:", err);
    }
  }, [userRoleKey]);

  useEffect(() => {
    if (user) {
      refreshApplications();
    } else {
      setApplications([]);
    }
  }, [user?.id, refreshApplications]);

  // ── Saved Schemes (tracker entries not yet applied) ───────────────────────
  const [savedSchemes, setSavedSchemes] = useState(() => {
    return safeGetItem("schemebridge_saved", []);
  });

  // ── Documents Vault ───────────────────────────────────────────────────────
  const [documents, setDocuments] = useState(() => {
    const savedUser = safeGetItem("schemebridge_user", null);
    if (savedUser) {
      const userKey = `schemebridge_documents_${savedUser.email || "default"}`;
      const savedDocs = safeGetItem(userKey, null);
      if (savedDocs) {
        return savedDocs;
      }
      if (savedUser.email === "citizen@schemebridge.in") {
        return DEFAULT_DOCUMENTS;
      }
      return [];
    }
    return [];
  });

  const refreshDocuments = useCallback(async () => {
    if (!user) return [];
    try {
      const list = await documentService.getDocuments();
      if (Array.isArray(list)) {
        setDocuments(list);
        if (user?.email) {
          safeSetItem(`schemebridge_documents_${user.email}`, list);
        }
        return list;
      }
      return [];
    } catch (err) {
      console.warn("Failed to refresh documents from backend vault:", err);
      return [];
    }
  }, [user?.id]);

  useEffect(() => {
    if (user?.id) {
      refreshDocuments();
    }
  }, [user?.id, refreshDocuments]);

  // ── Notifications ─────────────────────────────────────────────────────────
  const [notifications, setNotifications] = useState([]);

  // ── Audit Logs ────────────────────────────────────────────────────────────
  const [auditLogs, setAuditLogs] = useState(() => {
    return safeGetItem("schemebridge_audit_logs", DEFAULT_AUDIT_LOGS);
  });

  // ── Grievances Database ───────────────────────────────────────────────────
  const [grievances, setGrievances] = useState(() => {
    return safeGetItem("schemebridge_grievances", DEFAULT_GRIEVANCES);
  });

  // ── Feedback Database ────────────────────────────────────────────────────
  const [feedback, setFeedback] = useState(() => {
    return safeGetItem("schemebridge_feedback", DEFAULT_FEEDBACK);
  });

  // ── Recent Activities ─────────────────────────────────────────────────────
  const [recentActivities, setRecentActivities] = useState(() => {
    return safeGetItem("schemebridge_recent_activities", DEFAULT_RECENT_ACTIVITIES);
  });

  // ── Users Registry ────────────────────────────────────────────────────────
  const [usersRegistry, setUsersRegistry] = useState(() => {
    return safeGetItem("schemebridge_users_registry", DEFAULT_USERS_REGISTRY);
  });

  
  // ── Sync profile when user changes ─────────────────────────────────────────
  useEffect(() => {
    if (user) {
      setProfile({
        name: user.name || (user.firstName ? `${user.firstName} ${user.lastName || ""}`.trim() : ""),
        age: user.age !== undefined && user.age !== null ? user.age : "",
        gender: user.gender || "",
        occupation: user.occupation || "",
        annualIncome: user.annualIncome !== undefined && user.annualIncome !== null ? user.annualIncome : (user.income || ""),
        caste: user.caste || "",
        state: user.state || "",
        education: user.education || "",
        disabilityStatus: user.disabilityStatus || false,
        isComplete: Boolean(user.onboardingComplete),
        accessibilityPreferences: user.accessibilityPreferences || {},
      });
    } else {
      setProfile({
        name: "",
        age: "",
        gender: "",
        occupation: "",
        annualIncome: "",
        caste: "",
        state: "",
        education: "",
        disabilityStatus: false,
        isComplete: false,
      });
    }
  }, [user]);

  // ── Sync documents when user changes ───────────────────────────────────────
  useEffect(() => {
    if (user) {
      const docKey = `schemebridge_documents_${user.email || "default"}`;
      const savedDocs = safeGetItem(docKey, null);
      if (savedDocs) {
        setDocuments(savedDocs);
      } else {
        if (user.email === "citizen@schemebridge.in") {
          setDocuments(DEFAULT_DOCUMENTS);
        } else {
          setDocuments([]);
        }
      }
      lastSyncedDocsEmail.current = user.email;
    } else {
      setDocuments([]);
      lastSyncedDocsEmail.current = "";
    }
  }, [user]);

  // ── Persist documents to safe storage on any change ───────────────────────
  useEffect(() => {
    if (user && user.email === lastSyncedDocsEmail.current) {
      const docKey = `schemebridge_documents_${user.email || "default"}`;
      safeSetItem(docKey, documents);
    }
    if (!user || user.email === lastSyncedDocsEmail.current) {
      safeSetItem("schemebridge_documents", documents);
    }
  }, [documents, user]);

  // NOTE: Full schemes catalog is NEVER persisted into localStorage/sessionStorage.
  // It resides in memory and is backed authoritatively by backend APIs.

  useEffect(() => {
    safeSetItem("schemebridge_applications", applications);
  }, [applications]);

  useEffect(() => {
    safeSetItem("schemebridge_saved", savedSchemes);
  }, [savedSchemes]);

  useEffect(() => {
    safeSetItem("schemebridge_notifications", notifications);
  }, [notifications]);

  useEffect(() => {
    safeSetItem("schemebridge_audit_logs", auditLogs);
  }, [auditLogs]);

  useEffect(() => {
    safeSetItem("schemebridge_grievances", grievances);
  }, [grievances]);

  useEffect(() => {
    safeSetItem("schemebridge_feedback", feedback);
  }, [feedback]);

  useEffect(() => {
    safeSetItem("schemebridge_recent_activities", recentActivities);
  }, [recentActivities]);

  useEffect(() => {
    safeSetItem("schemebridge_users_registry", usersRegistry);
  }, [usersRegistry]);

  // ── Audit Log Action ──────────────────────────────────────────────────────
  const addAuditLog = (actionType, entityType, entityName, detail, actor = "Sanjay Kumar (Admin)") => {
    const now = new Date().toISOString();
    const newLog = {
      id: `LOG-${  Math.floor(100000 + Math.random() * 900000)}`,
      timestamp: now,
      actionType,
      entityType,
      entityName,
      actor,
      detail,
    };
    setAuditLogs((prev) => [newLog, ...prev]);
  };

  // ── Profile Actions ───────────────────────────────────────────────────────
  const updateProfile = async (updates) => {
    setProfile((prev) => ({ ...prev, ...updates }));
    try {
      await profileService.updateProfile({
        displayName: updates.name,
        dob: updates.dob,
        age: updates.age ? Number(updates.age) : undefined,
        gender: updates.gender,
        state: updates.state,
        district: updates.district,
        annualIncome: updates.annualIncome !== undefined ? (updates.annualIncome === "" ? undefined : Number(updates.annualIncome)) : (updates.income !== undefined ? (updates.income === "" ? undefined : Number(updates.income)) : undefined),
        occupation: updates.occupation,
        socialCategory: updates.caste,
        education: updates.education,
        disabilityStatus: updates.disabilityStatus,
        onboardingComplete: updates.isComplete !== undefined ? updates.isComplete : updates.onboardingComplete,
        onboardingStep: updates.onboardingStep,
        accessibilityPreferences: updates.accessibilityPreferences,
      });
    } catch (err) {
      console.warn("Failed to persist profile updates to backend:", err);
    }
    showToast("success", "Profile Saved", "Your eligibility profile has been updated successfully.");
    // Log profile update if it completes wizard
    if (updates.isComplete || updates.onboardingComplete) {
      addAuditLog("Edit", "Profile", updates.name || profile.name || "Citizen", `Citizen profile updated and socio-economic attributes locked.`, updates.name || profile.name || "Citizen");
    }
  };

  // ── Application Actions ───────────────────────────────────────────────────
  const applyToScheme = async (scheme) => {
    if (!scheme) return false;
    const effectiveCode = scheme.schemeCode || scheme.code || scheme.id;

    const alreadyApplied = hasApplied(effectiveCode) || hasApplied(scheme.id);
    if (alreadyApplied) {
      showToast("info", "Already Applied", "You have already applied for this scheme.");
      return false;
    }

    try {
      const res = await applicationService.createApplication({
        schemeCode: effectiveCode
      });

      if (res.error) {
        if (res.status === 409 || (res.message && res.message.toLowerCase().includes("already exists"))) {
          showToast("info", "Already Applied", "An active application already exists for this scheme.");
          await refreshApplications();
          return true;
        }
        showToast("error", "Application Failed", res.message || "Failed to submit application.");
        return false;
      }

      const backendApp = res.data;
      const schemeTitle = scheme.name || scheme.title?.english || (typeof scheme.title === "string" ? scheme.title : effectiveCode);

      const fullApp = {
        ...backendApp,
        schemeId: scheme.id || backendApp.schemeId,
        schemeCode: backendApp.schemeCode || effectiveCode,
        schemeName: schemeTitle,
        applicantName: profile.name || user?.name || "Citizen",
        currentStage: backendApp.status ? backendApp.status.replace(/_/g, " ") : "Submitted",
        referenceNo: backendApp.applicationNumber || backendApp.id,
        appliedDate: backendApp.createdAt ? new Date(backendApp.createdAt).toISOString().split("T")[0] : new Date().toISOString().split("T")[0],
      };

      setApplications((prev) => [fullApp, ...prev.filter((a) => a.id !== fullApp.id)]);
      safeSetItem("schemebridge_applications", [fullApp, ...applications.filter((a) => a.id !== fullApp.id)]);
      setSavedSchemes((prev) => prev.filter((s) => s.schemeId !== scheme.id && s.schemeCode !== effectiveCode));

      showToast("success", "Application Submitted", `Your application ${fullApp.referenceNo} for ${schemeTitle} has been submitted successfully.`);

      addAuditLog("Create", "Application", schemeTitle, `Citizen ${profile.name || "Citizen"} submitted application reference ${fullApp.referenceNo}.`, profile.name || "Citizen");

      addRecentActivity({
        officerName: profile.name || "Citizen",
        module: "Applications",
        activityType: "Submitted Application",
        description: `Citizen ${profile.name || "Citizen"} submitted application reference ${fullApp.referenceNo}.`,
        relatedEntityId: fullApp.id,
        status: "Success"
      });

      return true;
    } catch (err) {
      console.error("Failed to submit application to backend:", err);
      showToast("error", "Application Error", err.message || "An unexpected error occurred while submitting.");
      return false;
    }
  };

  const getApplicationForScheme = (schemeIdentifier) => {
    if (!schemeIdentifier) return null;
    return findApplicationForScheme(schemeIdentifier, applications);
  };

  const hasApplied = (schemeIdentifier) => {
    return Boolean(getApplicationForScheme(schemeIdentifier));
  };

  const updateApplicationStatus = (appId, newStage) => {
    const now = new Date().toISOString().split("T")[0];
    const nextActionMap = {
      "Submitted": "Application received. Aadhaar verification will begin within 3 working days.",
      "Under Review": "Your documents are being reviewed by the department. No action needed.",
      "Approved": "Congratulations! Disbursement will be initiated within 10 working days.",
      "Rejected": "Your application was rejected. Review the reason and reapply if eligible.",
    };

    const targetApp = applications.find(
      (a) => a.id === appId || a.applicationNumber === appId || a.applicationId === appId
    );
    const schemeName = targetApp?.schemeName || "Scheme";
    const applicantName = targetApp?.applicantName || "Citizen";

    setApplications((prev) =>
      prev.map((a) => {
        if (a.id === appId || a.applicationNumber === appId || a.applicationId === appId) {
          return {
            ...a,
            currentStage: newStage,
            status: newStage === "Approved" ? "APPROVED" : newStage === "Rejected" ? "REJECTED" : newStage === "Correction Required" ? "CORRECTION_REQUIRED" : newStage,
            nextAction: nextActionMap[newStage] || "Check back for updates.",
            stageHistory: [
              ...(a.stageHistory || []),
              { stage: newStage, date: now, note: `Status updated to "${newStage}" by administrator.` },
            ],
          };
        }
        return a;
      })
    );

    // Refresh from backend in background to ensure synchronized persistence
    if (typeof refreshApplications === "function") {
      refreshApplications().catch(() => {});
    }

    addAuditLog("Status Change", "Application", schemeName, `Application ${appId} for ${applicantName} updated to status '${newStage}'.`, "Sanjay Kumar (Admin)");
    addRecentActivity({
      officerName: "Sanjay Kumar (Admin)",
      module: "Applications",
      activityType: `${newStage} Application`,
      description: `Application ${appId} for ${applicantName} was updated to status '${newStage}'.`,
      relatedEntityId: appId,
      status: newStage === "Approved" ? "Success" : newStage === "Rejected" ? "Failure" : "Success"
    });
  };

  // ── Save to Tracker (without applying) ───────────────────────────────────
  const saveScheme = (scheme) => {
    const alreadySaved = savedSchemes.some((s) => s.schemeId === scheme.id);
    const alreadyApplied = applications.some((a) => a.schemeId === scheme.id);
    if (alreadySaved || alreadyApplied) {
return false;
}

    const now = new Date().toISOString().split("T")[0];
    setSavedSchemes((prev) => [
      {
        schemeId: scheme.id,
        schemeName: scheme.name,
        ministry: scheme.ministry,
        savedDate: now,
        stage: "Saved",
        stageHistory: [{ stage: "Saved", date: now, note: "Scheme saved to your tracker." }],
      },
      ...prev,
    ]);
    showToast("info", "Scheme Saved", `${scheme.name} has been added to your tracker.`);
    return true;
  };

  const isSaved = (schemeId) => savedSchemes.some((s) => s.schemeId === schemeId);

  const updateSavedStage = (schemeId, newStage) => {
    const now = new Date().toISOString().split("T")[0];
    setSavedSchemes((prev) =>
      prev.map((s) =>
        s.schemeId === schemeId
          ? {
              ...s,
              stage: newStage,
              stageHistory: [
                ...(s.stageHistory || []),
                { stage: newStage, date: now, note: `Stage updated to "${newStage}".` },
              ],
            }
          : s
      )
    );
  };

  const removeSaved = (schemeId) => {
    setSavedSchemes((prev) => prev.filter((s) => s.schemeId !== schemeId));
  };

  const inFlightNotificationReads = useRef(new Set());

  const resolveNotificationActionRoute = useCallback((n, isPrivileged) => {
    const typeStr = String(n.type || "").toUpperCase();
    const entityType = String(n.relatedEntityType || "").toUpperCase();
    const entityId = n.relatedEntityId || n.metadata?.applicationId || n.applicationId;

    if (isPrivileged) {
      if (entityType === "APPLICATION" || typeStr.includes("APP")) {
        return entityId ? `/admin/review/${entityId}` : "/admin/review";
      }
      if (entityType === "GRIEVANCE" || typeStr.includes("GRIEVANCE")) {
        return "/admin/grievances";
      }
      if (entityType === "FEEDBACK" || typeStr.includes("FEEDBACK")) {
        return "/admin/feedback";
      }
      if (entityType === "DOCUMENT" || typeStr.includes("DOC")) {
        return "/admin/documents";
      }
      if (entityType === "SCHEME" || typeStr.includes("SCHEME")) {
        return "/admin/schemes";
      }
      return "/admin/notifications";
    } else {
      if (entityType === "APPLICATION" || typeStr.includes("APP")) {
        return entityId ? `/applications/${entityId}` : "/applications";
      }
      if (entityType === "GRIEVANCE" || typeStr.includes("GRIEVANCE")) {
        return "/help";
      }
      if (entityType === "DOCUMENT" || typeStr.includes("DOC")) {
        return "/documents";
      }
      if (entityType === "SCHEME" || typeStr.includes("SCHEME")) {
        return entityId ? `/scheme/${entityId}` : "/recommendations";
      }
      if (entityType === "FEEDBACK" || typeStr.includes("FEEDBACK")) {
        return "/feedback";
      }
      return null;
    }
  }, []);

  const mapBackendNotification = useCallback((n) => {
    const roleStr = String(user?.role || "").toUpperCase();
    const isPrivileged = ["ADMIN", "SUPER_ADMIN", "SCHEME_MANAGER", "VERIFICATION_OFFICER"].includes(roleStr);
    const actionRoute = resolveNotificationActionRoute(n, isPrivileged);

    return {
      id: n.id,
      type: n.type,
      category: n.type
        ? n.type.toLowerCase().includes("doc")
          ? "document"
          : n.type.toLowerCase().includes("app")
          ? "application"
          : n.type.toLowerCase().includes("grievance")
          ? "grievance"
          : n.type.toLowerCase().includes("feedback")
          ? "feedback"
          : "system"
        : "system",
      title: n.title,
      body: n.message,
      message: n.message,
      timestamp: n.createdAt,
      createdAt: n.createdAt,
      read: n.read === true,
      isRead: n.read === true,
      schemeId: n.relatedEntityId,
      relatedEntityType: n.relatedEntityType,
      relatedEntityId: n.relatedEntityId,
      applicationId: n.relatedEntityId || n.metadata?.applicationId || n.applicationId,
      applicationNumber: n.metadata?.applicationNumber || n.applicationNumber,
      actionLabel: actionRoute ? "View Details" : null,
      actionRoute: actionRoute,
      icon: "Bell",
      priority: "normal",
    };
  }, [user?.role, resolveNotificationActionRoute]);

  const refreshNotifications = useCallback(async () => {
    if (!user) {
      setNotifications([]);
      return;
    }
    try {
      const res = await notificationService.getNotifications({ page: 0, size: 50 });
      if (!res.error && res.data?.content) {
        setNotifications(res.data.content.map(mapBackendNotification));
      }
    } catch (err) {
      console.warn("Failed to refresh notifications:", err);
    }
  }, [user, mapBackendNotification]);

  // ── Sync notifications from backend when user changes + SSE subscription ──
  useEffect(() => {
    let isMounted = true;
    if (user) {
      refreshNotifications();

      const roleStr = String(user?.role || "").toUpperCase();
      const isPrivileged = ["ADMIN", "SUPER_ADMIN", "SCHEME_MANAGER", "VERIFICATION_OFFICER"].includes(roleStr);

      if (isPrivileged) {
        const unsubscribe = notificationService.subscribeToNotificationStream(
          (newAlert) => {
            if (!isMounted || !newAlert || !newAlert.id) return;
            setNotifications((prev) => {
              // Stable ID deduplication
              if (prev.some((item) => item.id === newAlert.id)) {
                return prev;
              }
              const mapped = mapBackendNotification(newAlert);
              return [mapped, ...prev];
            });
          },
          (err) => {
            console.warn("AppContext SSE notification stream notice:", err);
          }
        );
        return () => {
          isMounted = false;
          unsubscribe();
        };
      }
    } else {
      setNotifications([]);
    }
    return () => { isMounted = false; };
  }, [user, refreshNotifications, mapBackendNotification]);

  // ── Sync grievances & feedback from backend ──────────────────────────────
  useEffect(() => {
    let isMounted = true;
    if (user?.email) {
      const roleStr = String(user?.role || "").toUpperCase();
      const isPrivileged = ["ADMIN", "SUPER_ADMIN", "SCHEME_MANAGER", "VERIFICATION_OFFICER"].includes(roleStr);
      if (isPrivileged) {
        grievanceService.getAdminGrievances({ size: 100 })
          .then((res) => {
            if (isMounted && !res.error && res.data) {
              const items = Array.isArray(res.data) ? res.data : (res.data.content || []);
              if (items.length > 0) setGrievances(items);
            }
          })
          .catch(() => {});

        feedbackService.getAdminFeedback({ size: 100 })
          .then((res) => {
            if (isMounted && !res.error && res.data) {
              const items = Array.isArray(res.data) ? res.data : (res.data.content || []);
              if (items.length > 0) setFeedback(items);
            }
          })
          .catch(() => {});
      } else {
        grievanceService.getCitizenGrievances({ size: 50 })
          .then((res) => {
            if (isMounted && !res.error && res.data) {
              const items = Array.isArray(res.data) ? res.data : (res.data.content || []);
              if (items.length > 0) setGrievances(items);
            }
          })
          .catch(() => {});

        feedbackService.getCitizenFeedback()
          .then((res) => {
            if (isMounted && !res.error && res.data) {
              const items = Array.isArray(res.data) ? res.data : (res.data.content || []);
              if (items.length > 0) setFeedback(items);
            }
          })
          .catch(() => {});
      }
    }
    return () => { isMounted = false; };
  }, [user?.email, user?.role]);

  // ── Notification Actions ──────────────────────────────────────────────────
  const markNotificationRead = useCallback(async (id) => {
    if (!id || inFlightNotificationReads.current.has(id)) return;

    let wasUnread = false;
    setNotifications((prev) => {
      const target = prev.find((n) => n.id === id);
      if (!target || target.read || target.isRead) {
        return prev;
      }
      wasUnread = true;
      return prev.map((n) => (n.id === id ? { ...n, read: true, isRead: true } : n));
    });

    if (!wasUnread) return;

    if (typeof id === "string" && !id.startsWith("NOTIF-")) {
      inFlightNotificationReads.current.add(id);
      try {
        const res = await notificationService.markNotificationRead(id);
        if (res?.error) {
          // Revert optimistic update if backend failed
          setNotifications((prev) =>
            prev.map((n) => (n.id === id ? { ...n, read: false, isRead: false } : n))
          );
        }
      } catch (err) {
        console.warn("Failed to mark notification read on backend:", err);
        // Revert optimistic update
        setNotifications((prev) =>
          prev.map((n) => (n.id === id ? { ...n, read: false, isRead: false } : n))
        );
      } finally {
        inFlightNotificationReads.current.delete(id);
      }
    }
  }, []);

  const markAllNotificationsRead = useCallback(async () => {
    let hadUnread = false;
    setNotifications((prev) => {
      hadUnread = prev.some((n) => !n.read && !n.isRead);
      return prev.map((n) => ({ ...n, read: true, isRead: true }));
    });
    if (hadUnread) {
      try {
        await notificationService.markAllNotificationsRead();
      } catch (err) {
        console.warn("Failed to mark all notifications read on backend:", err);
      }
    }
  }, []);

  const dismissNotification = (id) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  const addNotification = (notif) => {
    const newNotif = {
      id: `NOTIF-${Date.now()}`,
      timestamp: new Date().toISOString(),
      read: false,
      isRead: false,
      priority: "normal",
      ...notif,
    };
    setNotifications((prev) => [newNotif, ...prev]);
  };

  const unreadCount = notifications.filter((n) => !n.read && !n.isRead).length;

  const addDocument = async (nameOrPayload, type, issuer = "Self-Uploaded", expiryDate = "No Expiration", status = "pending_review", source = "Manual Upload") => {
    let newDoc;
    const now = new Date().toISOString().split("T")[0];

    if (typeof nameOrPayload === "object" && nameOrPayload !== null) {
      const payload = nameOrPayload;
      const genIdNumber = Math.floor(1000 + Math.random() * 9000);
      newDoc = {
        id: payload.id || `DOC-${genIdNumber}`,
        name: payload.name || payload.documentName || "Document",
        documentName: payload.name || payload.documentName || "Document",
        filename: payload.filename || payload.fileName || `${(payload.name || "document").toLowerCase().replace(/\s+/g, "_")}.pdf`,
        filesize: payload.filesize || payload.fileSize || "1.4 MB",
        type: payload.type || payload.category || "Identity Proof",
        status: payload.status || payload.verificationStatus || "pending_review",
        issuer: payload.issuer || "Self-Uploaded",
        expiryDate: payload.expiryDate || payload.expiry || "No Expiration",
        holderName: payload.holderName ?? "",
        docNumber: payload.docNumber || payload.documentNumber || "",
        ocrStatus: payload.ocrStatus || "Success",
        aiConfidence: payload.aiConfidence || 98,
        date: payload.date || now,
        uploadDate: payload.uploadDate || now,
        source: payload.source || "Manual Upload",
        linkedSchemes: payload.linkedSchemes || [],
        file: payload.file,
      };
    } else {
      const genIdNumber = Math.floor(1000 + Math.random() * 9000);
      newDoc = {
        id: `DOC-${genIdNumber}`,
        name: nameOrPayload,
        documentName: nameOrPayload,
        filename: `${nameOrPayload.toLowerCase().replace(/\s+/g, "_")}.pdf`,
        filesize: "1.4 MB",
        type: type || "Identity Proof",
        status: status || "pending_review",
        issuer: issuer || "Self-Uploaded",
        expiryDate: expiryDate || "No Expiration",
        holderName: "",
        docNumber: "",
        ocrStatus: "Success",
        aiConfidence: 98,
        date: now,
        uploadDate: now,
        source: source || "Manual Upload",
        linkedSchemes: [],
      };
    }

    try {
      const backendRes = await documentService.uploadDocument(typeof nameOrPayload === "object" ? nameOrPayload : newDoc);
      if (backendRes && backendRes.id) {
        newDoc = { ...newDoc, ...backendRes };
      }
    } catch (err) {
      console.warn("Backend vault upload error, using local fallback:", err);
    }

    setDocuments((prev) => {
      const filtered = prev.filter((d) => d.id !== newDoc.id && (d.name || "").toLowerCase() !== (newDoc.name || "").toLowerCase());
      const updated = [newDoc, ...filtered];
      if (user?.email) {
        safeSetItem(`schemebridge_documents_${user.email}`, updated);
      }
      return updated;
    });

    showToast("success", "Saved to Digital Locker", `"${newDoc.name}" has been saved into your vault.`);
    addAuditLog("Create", "Document", newDoc.name, `Document '${newDoc.name}' (${newDoc.id}) uploaded to vault.`, profile?.name || "Citizen");
    return newDoc;
  };

  // Import multiple documents (batch import)
  const importDocuments = (docs) => {
    const now = new Date().toISOString().split("T")[0];
    const imported = docs.map((doc) => ({
      id: Date.now() + Math.random(),
      name: doc.name,
      type: doc.type,
      status: doc.status || "verified",
      issuer: doc.issuer || "Official Authority",
      expiryDate: doc.expiry || "No Expiration",
      date: now,
      source: doc.source || "Manual Upload",
      linkedSchemes: [],
    }));
    setDocuments((prev) => {
      const existingNames = new Set(prev.map((d) => d.name.toLowerCase()));
      const fresh = imported.filter((d) => !existingNames.has(d.name.toLowerCase()));
      return [...prev, ...fresh];
    });
    if (imported.length > 0) {
      addAuditLog("Create", "Document", `${imported.length} documents`, `${imported.length} document(s) imported into vault.`, profile.name);
    }
  };

  const removeDocument = async (docId) => {
    const doc = documents.find((d) => d.id === docId);
    const docName = doc ? doc.name : "Document";
    setDocuments((prev) => prev.filter((d) => d.id !== docId));
    try {
      await documentService.deleteDocument(docId);
    } catch (err) {
      console.warn("Failed to delete document from backend:", err);
    }
    addAuditLog("Delete", "Document", docName, `Document '${docName}' deleted from vault.`, profile.name);
  };

  const updateDocumentStatus = (docId, status) => {
    let docName = "Document";
    setDocuments((prev) =>
      prev.map((d) => {
        if (d.id === docId) {
          docName = d.name;
          return { ...d, status, date: new Date().toISOString().split("T")[0] };
        }
        return d;
      })
    );
    addAuditLog("Status Change", "Document", docName, `Document status updated to '${status}'.`, "System Vault Registry");
  };

  const linkDocument = (docId) => {
    setDocuments((prev) =>
      prev.map((d) =>
        d.id === docId
          ? { ...d, status: "verified", date: new Date().toISOString().split("T")[0] }
          : d
      )
    );
  };

  // ── Scheme Admin Actions ──────────────────────────────────────────────────
  const addScheme = (scheme) => {
    const now = new Date().toISOString().split("T")[0];
    const baseId = scheme.name.toLowerCase().replace(/[^a-z0-9]+/g, "-").slice(0, 25);
    const existingIds = new Set(schemes.map((s) => s.id));
    let finalId = baseId;
    let counter = 1;
    while (existingIds.has(finalId)) {
      finalId = `${baseId}-${counter}`;
      counter++;
    }
    const newScheme = {
      ...scheme,
      id: finalId,
      status: scheme.status || "draft",
      lastUpdated: now,
      tags: scheme.tags || [],
    };
    setSchemes((prev) => [...prev, newScheme]);
    addAuditLog("Create", "Scheme", scheme.name, `New scheme administrative record created in '${newScheme.status}' mode.`);
    addRecentActivity({
      officerName: "Sanjay Kumar (Admin)",
      module: "Schemes",
      activityType: "Create Scheme",
      description: `New scheme administrative record '${scheme.name}' created in '${newScheme.status}' mode.`,
      relatedEntityId: finalId,
      status: "Success"
    });
  };

  const editScheme = (updatedScheme) => {
    const now = new Date().toISOString().split("T")[0];
    const oldScheme = schemes.find((s) => s.id === updatedScheme.id);
    setSchemes((prev) =>
      prev.map((s) =>
        s.id === updatedScheme.id ? { ...updatedScheme, lastUpdated: now } : s
      )
    );
    if (oldScheme && oldScheme.status !== updatedScheme.status) {
      const action = updatedScheme.status === "published" ? "Publish" : updatedScheme.status === "archived" ? "Archive" : "Edit";
      addAuditLog(action, "Scheme", updatedScheme.name, `Scheme '${updatedScheme.name}' status transitioned from '${oldScheme.status}' to '${updatedScheme.status}'.`);
    } else {
      addAuditLog("Edit", "Scheme", updatedScheme.name, `Scheme administrative configurations updated.`);
    }
  };

  const deleteScheme = (schemeId) => {
    const sc = schemes.find((s) => s.id === schemeId);
    const scName = sc ? sc.name : schemeId;
    setSchemes((prev) => prev.filter((s) => s.id !== schemeId));
    addAuditLog("Delete", "Scheme", scName, `Scheme '${scName}' deleted from administrative directory.`);
  };

  // ── Grievance Submission Actions ─────────────────────────────────────────
  const submitGrievance = (grievance) => {
    const tempId = `GRV-${Math.floor(1000 + Math.random() * 9000)}`;
    const newGrievance = {
      id: tempId,
      grievanceNumber: tempId,
      citizenName: profile?.name || user?.name || "Citizen",
      citizenPhone: grievance.phone,
      citizenEmail: grievance.email || user?.email,
      relatedScheme: grievance.relatedScheme,
      category: grievance.category,
      subject: grievance.subject || grievance.category,
      description: grievance.description,
      supportingNote: grievance.supportingNote || "",
      status: "Received",
      date: new Date().toISOString().split("T")[0],
    };

    setGrievances((prev) => [newGrievance, ...prev]);
    addAuditLog("Create", "Grievance", grievance.category, `Grievance reference ${tempId} submitted for ${grievance.relatedScheme}.`, profile?.name || user?.name || "Citizen");

    // Call backend API asynchronously
    grievanceService.createGrievance({
      category: grievance.category,
      subject: grievance.subject || grievance.category,
      description: grievance.description,
      schemeCode: grievance.relatedScheme,
      priority: grievance.priority || "MEDIUM"
    }).then((res) => {
      if (res && !res.error && res.data) {
        const savedId = res.data.grievanceNumber || res.data.id;
        setGrievances((prev) =>
          prev.map((g) => (g.id === tempId ? { ...g, ...res.data, id: savedId, grievanceNumber: savedId } : g))
        );
      }
    }).catch((err) => {
      console.warn("Grievance saved locally, backend sync note:", err);
    });

    return tempId;
  };

  // ── Feedback Submission Actions ─────────────────────────────────────────
  const submitFeedback = (data) => {
    const tempId = `FB-${Math.floor(1000 + Math.random() * 9000)}`;
    const newFeedback = {
      id: tempId,
      feedbackNumber: tempId,
      citizenName: profile?.name || user?.name || "Citizen",
      citizenEmail: user?.email || "",
      type: data.type,
      rating: data.rating,
      comment: data.comment,
      relatedScheme: data.relatedScheme || null,
      status: "Received",
      date: new Date().toISOString().split("T")[0],
    };

    setFeedback((prev) => [newFeedback, ...prev]);
    addAuditLog("Create", "Feedback", data.type, `Feedback reference ${tempId} submitted.`, profile?.name || user?.name || "Citizen");

    // Call backend API asynchronously
    feedbackService.submitPortalFeedback({
      type: data.type,
      rating: data.rating,
      comment: data.comment,
      relatedScheme: data.relatedScheme || null,
      citizenEmail: user?.email || "",
      citizenName: profile?.name || user?.name || "Citizen"
    }).then((res) => {
      if (res && !res.error && res.data) {
        const savedId = res.data.feedbackNumber || res.data.id;
        setFeedback((prev) =>
          prev.map((f) => (f.id === tempId ? { ...f, ...res.data, id: savedId, feedbackNumber: savedId } : f))
        );
      }
    }).catch((err) => {
      console.warn("Feedback saved locally, backend sync note:", err);
    });

    return tempId;
  };

  const updateGrievanceStatus = (id, newStatus) => {
    let category = "Grievance";
    let relatedScheme = "Scheme";
    setGrievances((prev) =>
      prev.map((g) => {
        if (g.id === id || g.grievanceNumber === id) {
          category = g.category;
          relatedScheme = g.relatedScheme;
          return { ...g, status: newStatus };
        }
        return g;
      })
    );
    addAuditLog("Status Change", "Grievance", category, `Grievance ${id} status updated to '${newStatus}' for ${relatedScheme}.`, "Sanjay Kumar (Admin)");

    // Call backend resolve/assign if applicable
    if (newStatus === "Resolved") {
      grievanceService.resolveGrievance(id, "Resolved by admin desk", "Administrative review completed").catch(() => {});
    }
  };

  const addRecentActivity = (activity) => {
    const newActivity = {
      id: `act-${Date.now()}`,
      timestamp: "Just Now",
      ...activity
    };
    setRecentActivities((prev) => [newActivity, ...prev].slice(0, 20));
  };

  const toggleRegistryUserStatus = (roleKey, email) => {
    setUsersRegistry((prev) => {
      const list = prev[roleKey] || [];
      const updatedList = list.map((u) =>
        u.email === email ? { ...u, status: u.status === "Active" ? "Suspended" : "Active" } : u
      );
      return { ...prev, [roleKey]: updatedList };
    });
  };

  const updateRegistryUser = (roleKey, email, updates) => {
    setUsersRegistry((prev) => {
      const list = prev[roleKey] || [];
      const updatedList = list.map((u) =>
        u.email === email ? { ...u, ...updates } : u
      );
      return { ...prev, [roleKey]: updatedList };
    });
  };

  const resetUserPassword = (email) => {
    // Mock implementation
    addAuditLog("Password Reset", "User", email, "Password reset initiated for user.");
  };

  const assignUserRole = (email, oldRole, newRole) => {
    setUsersRegistry((prev) => {
      const oldList = prev[oldRole] || [];
      const userToMove = oldList.find((u) => u.email === email);
      if (!userToMove) return prev;

      const newOldList = oldList.filter((u) => u.email !== email);
      const newNewList = [...(prev[newRole] || []), userToMove];

      return { ...prev, [oldRole]: newOldList, [newRole]: newNewList };
    });
    addAuditLog("Role Change", "User", email, `User role changed from ${oldRole} to ${newRole}.`);
  };

  const updateApplicationPriority = (appId, manualPriority) => {
    let schemeName = "Scheme";
    let applicantName = "Citizen";
    setApplications((prev) =>
      prev.map((a) => {
        if (a.id === appId) {
          schemeName = a.schemeName;
          applicantName = a.applicantName;
          return {
            ...a,
            manualPriority,
          };
        }
        return a;
      })
    );
    addAuditLog("Edit", "Application", schemeName, `Application ${appId} priority manually set to '${manualPriority}'.`, "Sanjay Kumar (Admin)");
    addRecentActivity({
      officerName: "Sanjay Kumar (Admin)",
      module: "Applications",
      activityType: "Set Priority",
      description: `Application ${appId} priority manually set to '${manualPriority}'.`,
      relatedEntityId: appId,
      status: "Success"
    });
  };

  const resetData = () => {
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const key = localStorage.key(i);
      if (key && key.startsWith("schemebridge_")) {
        localStorage.removeItem(key);
      }
    }
    window.location.reload();
  };

  return (
    <AppContext.Provider
      value={{
        // Profile
        profile,
        updateProfile,
        // Schemes
        schemes,
        addScheme,
        editScheme,
        deleteScheme,
        // Applications
        applications,
        applyToScheme,
        hasApplied,
        getApplicationForScheme,
        refreshApplications,
        updateApplicationStatus,
        // Saved / Tracker
        savedSchemes,
        saveScheme,
        isSaved,
        updateSavedStage,
        removeSaved,
        // Documents
        documents,
        refreshDocuments,
        addDocument,
        importDocuments,
        removeDocument,
        updateDocumentStatus,
        linkDocument,
        // Notifications
        notifications,
        unreadCount,
        refreshNotifications,
        markNotificationRead,
        markAllNotificationsRead,
        dismissNotification,
        addNotification,
        // Audit logs
        auditLogs,
        addAuditLog,
        // Grievances
        grievances,
        submitGrievance,
        updateGrievanceStatus,
        // Feedback
        feedback,
        submitFeedback,
        // Recent Activities
        recentActivities,
        addRecentActivity,
        // Users Registry
        usersRegistry,
        toggleRegistryUserStatus,
        updateRegistryUser,
        resetUserPassword,
        assignUserRole,
        updateApplicationPriority,
        // Utilities
        resetData,
        // Safe i18n fallbacks (fixed English)
        t: (key) => key,
        language: "en",
        currentLanguage: "en",
        setLanguage: () => {},
        toggleLanguage: () => {},
      }}
    >
      {children}
    </AppContext.Provider>
  );
};
