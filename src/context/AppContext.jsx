/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useRef } from "react";
import { mockSchemes as initialSchemes } from "../data/mockSchemes";
import { TRANSLATIONS } from "../data/translations";
import { DEFAULT_NOTIFICATIONS } from "../data/mockNotifications";
import { DEFAULT_DOCUMENTS } from "../data/mockDocuments";
import { useAuth } from "./AuthContext";

const AppContext = createContext(null);

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

// ── Default initial applications ───────────────────────────────────────────
const DEFAULT_APPLICATIONS = [
  {
    id: "APP-9023",
    schemeId: "atal-pension-yojana",
    schemeName: "Atal Pension Yojana (APY)",
    ministry: "Ministry of Finance / PFRDA",
    applicantName: "Rajesh Patel",
    applicantState: "Gujarat",
    applicantIncome: "₹1,80,000",
    applicantCaste: "OBC",
    appliedDate: "2026-06-15",
    currentStage: "Under Review",
    referenceNo: "APY/GJ/2026/009023",
    stageHistory: [
      { stage: "Saved", date: "2026-06-10", note: "Scheme saved for review." },
      { stage: "Preparing Documents", date: "2026-06-12", note: "Documents being collected." },
      { stage: "Ready to Apply", date: "2026-06-14", note: "All documents ready." },
      { stage: "Submitted", date: "2026-06-15", note: "Application submitted online." },
      { stage: "Under Review", date: "2026-06-17", note: "Aadhaar verification in progress." },
    ],
    nextAction: "Await verification from the Aadhaar-linked bank. Expected in 5–7 working days.",
  },
  {
    id: "APP-1042",
    schemeId: "pm-kisan",
    schemeName: "Pradhan Mantri Kisan Samman Nidhi (PM-KISAN)",
    ministry: "Ministry of Agriculture and Farmers Welfare",
    applicantName: "Rajesh Patel",
    applicantState: "Gujarat",
    applicantIncome: "₹1,80,000",
    applicantCaste: "OBC",
    appliedDate: "2026-05-10",
    currentStage: "Approved",
    referenceNo: "PMK/GJ/2026/001042",
    stageHistory: [
      { stage: "Saved", date: "2026-05-01", note: "Saved to profile." },
      { stage: "Preparing Documents", date: "2026-05-03", note: "Land Records retrieved via API." },
      { stage: "Ready to Apply", date: "2026-05-05", note: "E-sign verification complete." },
      { stage: "Submitted", date: "2026-05-10", note: "Direct bank mandate registered." },
      { stage: "Under Review", date: "2026-05-12", note: "Tehsildar approval pending." },
      { stage: "Approved", date: "2026-05-15", note: "Verification successful. DBT active." },
    ],
    nextAction: "None. Benefits active. First installment scheduled for next DBT cycle.",
  },
  {
    id: "APP-5120",
    schemeId: "pm-awas-yojana",
    schemeName: "Pradhan Mantri Awas Yojana (PMAY)",
    ministry: "Ministry of Housing and Urban Affairs",
    applicantName: "Rajesh Patel",
    applicantState: "Gujarat",
    applicantIncome: "₹1,80,000",
    applicantCaste: "OBC",
    appliedDate: "2026-04-02",
    currentStage: "Rejected",
    referenceNo: "PMAY/GJ/2026/005120",
    stageHistory: [
      { stage: "Saved", date: "2026-03-20", note: "Saved for review." },
      { stage: "Preparing Documents", date: "2026-03-22", note: "Affidavit missing." },
      { stage: "Ready to Apply", date: "2026-03-28", note: "Documents collected." },
      { stage: "Submitted", date: "2026-04-02", note: "Submitted via CSC." },
      { stage: "Under Review", date: "2026-04-05", note: "Asset screening in progress." },
      { stage: "Rejected", date: "2026-04-10", note: "Income mismatch with ITR registry." },
    ],
    nextAction: "Income mismatch. Please upload correct income certificate or file a grievance.",
  }
];

export const AppProvider = ({ children }) => {
  const { user } = useAuth();
  const lastSyncedEmail = useRef(user?.email || "");
  const lastSyncedDocsEmail = useRef(user?.email || "");

  // ── Citizen Profile ───────────────────────────────────────────────────────
  const [profile, setProfile] = useState(() => {
    const savedUser = localStorage.getItem("schemebridge_user");
    if (savedUser) {
      const parsedUser = JSON.parse(savedUser);
      const userKey = `schemebridge_profile_${parsedUser.email || "default"}`;
      const savedProfile = localStorage.getItem(userKey);
      if (savedProfile) {
return JSON.parse(savedProfile);
}

      return {
        name: parsedUser.name || "",
        age: parsedUser.age || 32,
        gender: parsedUser.gender || "Male",
        occupation: parsedUser.occupation || "Farmer",
        annualIncome: parsedUser.income || 180000,
        caste: parsedUser.caste || "OBC",
        state: parsedUser.state || "Gujarat",
        isComplete: false,
      };
    }

    const saved = localStorage.getItem("schemebridge_profile");
    return saved
      ? JSON.parse(saved)
      : {
          name: "Rajesh Patel",
          age: 32,
          gender: "Male",
          occupation: "Farmer",
          annualIncome: 180000,
          caste: "OBC",
          state: "Gujarat",
          isComplete: false,
        };
  });

  // ── Schemes Database ──────────────────────────────────────────────────────
  const [schemes, setSchemes] = useState(() => {
    const saved = localStorage.getItem("schemebridge_schemes");
    let loadedSchemes;

    if (saved) {
      try {
        loadedSchemes = JSON.parse(saved);
        if (import.meta.env.DEV) {
          console.warn("[AppContext] Loaded schemes from localStorage:", loadedSchemes?.length || 0);
        }

        // Check if saved data is corrupted (missing required fields)
        if (loadedSchemes && loadedSchemes.length > 0) {
          const firstScheme = loadedSchemes[0];
          if (!firstScheme.name || !firstScheme.ministry || !firstScheme.id || !firstScheme.category || !firstScheme.deadline || !firstScheme.approvalRate) {
            console.warn("localStorage schemes are corrupted or missing required fields. Resetting to initial schemes.");
            loadedSchemes = initialSchemes;
            localStorage.removeItem("schemebridge_schemes");
          }
        }
      } catch (e) {
        console.error("Failed to parse saved schemes from localStorage:", e);
        loadedSchemes = initialSchemes;
        localStorage.removeItem("schemebridge_schemes");
      }
    } else {
      loadedSchemes = initialSchemes;
      if (import.meta.env.DEV) {
        console.warn("[AppContext] No saved schemes found; using initial mock data:", initialSchemes?.length || 0);
      }
    }

    return loadedSchemes;
  });

  // ── Applications ──────────────────────────────────────────────────────────
  const [applications, setApplications] = useState(() => {
    const saved = localStorage.getItem("schemebridge_applications");
    return saved ? JSON.parse(saved) : DEFAULT_APPLICATIONS;
  });

  // ── Saved Schemes (tracker entries not yet applied) ───────────────────────
  const [savedSchemes, setSavedSchemes] = useState(() => {
    const saved = localStorage.getItem("schemebridge_saved");
    return saved ? JSON.parse(saved) : [];
  });

  // ── Documents Vault ───────────────────────────────────────────────────────
  const [documents, setDocuments] = useState(() => {
    const savedUser = localStorage.getItem("schemebridge_user");
    if (savedUser) {
      const parsedUser = JSON.parse(savedUser);
      const userKey = `schemebridge_documents_${parsedUser.email || "default"}`;
      const savedDocs = localStorage.getItem(userKey);
      if (savedDocs) {
return JSON.parse(savedDocs);
}
      if (parsedUser.email === "citizen@schemebridge.in") {
        return DEFAULT_DOCUMENTS;
      }
      return [];
    }
    return [];
  });

  // ── Notifications ─────────────────────────────────────────────────────────
  const [notifications, setNotifications] = useState(() => {
    const saved = localStorage.getItem("schemebridge_notifications");
    return saved ? JSON.parse(saved) : DEFAULT_NOTIFICATIONS;
  });

  // ── Audit Logs ────────────────────────────────────────────────────────────
  const [auditLogs, setAuditLogs] = useState(() => {
    const saved = localStorage.getItem("schemebridge_audit_logs");
    return saved ? JSON.parse(saved) : DEFAULT_AUDIT_LOGS;
  });

  // ── Grievances Database ───────────────────────────────────────────────────
  const [grievances, setGrievances] = useState(() => {
    const saved = localStorage.getItem("schemebridge_grievances");
    return saved ? JSON.parse(saved) : DEFAULT_GRIEVANCES;
  });

  // ── Language & Translations ───────────────────────────────────────────────
  const [language, setLanguage] = useState(() => {
    return localStorage.getItem("schemebridge_language") || "en";
  });

  const toggleLanguage = () => {
    const nextLang = language === "en" ? "hi" : "en";
    changeLanguage(nextLang);
  };

  const changeLanguage = (nextLang) => {
    setLanguage(nextLang);
    localStorage.setItem("schemebridge_language", nextLang);
  };

  const t = (key, params = {}) => {
    const dict = TRANSLATIONS[language] || TRANSLATIONS.en;
    let text = dict[key] || TRANSLATIONS.en[key] || key;

    // Replace parameters in the format {paramName}
    Object.keys(params).forEach(param => {
      text = text.replace(new RegExp(`{${param}}`, 'g'), params[param]);
    });

    return text;
  };

  // ── Sync profile when user changes ─────────────────────────────────────────
  useEffect(() => {
    if (user) {
      const userKey = `schemebridge_profile_${user.email || "default"}`;
      const savedProfile = localStorage.getItem(userKey);
      if (savedProfile) {
        const parsed = JSON.parse(savedProfile);
        // Merge user demographic details into profile, preferring user details if they changed/exist
        const merged = {
          ...parsed,
          name: user.name !== undefined ? user.name : parsed.name,
          age: user.age !== undefined ? user.age : parsed.age,
          gender: user.gender !== undefined ? user.gender : parsed.gender,
          occupation: user.occupation !== undefined ? user.occupation : parsed.occupation,
          annualIncome: user.annualIncome !== undefined ? user.annualIncome : (user.income !== undefined ? user.income : parsed.annualIncome),
          caste: user.caste !== undefined ? user.caste : parsed.caste,
          state: user.state !== undefined ? user.state : parsed.state,
        };
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setProfile(merged);
        localStorage.setItem(userKey, JSON.stringify(merged));
      } else {
        setProfile({
          name: user.name || "",
          age: user.age || 32,
          gender: user.gender || "Male",
          occupation: user.occupation || "Farmer",
          annualIncome: user.annualIncome || user.income || 180000,
          caste: user.caste || "OBC",
          state: user.state || "Gujarat",
          isComplete: false,
        });
      }
      lastSyncedEmail.current = user.email;
    } else {
      setProfile({
        name: "",
        age: "",
        gender: "Male",
        occupation: "Farmer",
        annualIncome: "",
        caste: "OBC",
        state: "Gujarat",
        isComplete: false,
      });
      lastSyncedEmail.current = "";
    }
  }, [user]);

  // ── Persist to localStorage on any change ─────────────────────────────────
  useEffect(() => {
    if (user && user.email === lastSyncedEmail.current) {
      const userKey = `schemebridge_profile_${user.email || "default"}`;
      localStorage.setItem(userKey, JSON.stringify(profile));
    }
    if (!user || user.email === lastSyncedEmail.current) {
      localStorage.setItem("schemebridge_profile", JSON.stringify(profile));
    }
  }, [profile, user]);

  // ── Sync documents when user changes ───────────────────────────────────────
  useEffect(() => {
    if (user) {
      const docKey = `schemebridge_documents_${user.email || "default"}`;
      const savedDocs = localStorage.getItem(docKey);
      if (savedDocs) {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setDocuments(JSON.parse(savedDocs));
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

  // ── Persist documents to localStorage on any change ───────────────────────
  useEffect(() => {
    if (user && user.email === lastSyncedDocsEmail.current) {
      const docKey = `schemebridge_documents_${user.email || "default"}`;
      localStorage.setItem(docKey, JSON.stringify(documents));
    }
    if (!user || user.email === lastSyncedDocsEmail.current) {
      localStorage.setItem("schemebridge_documents", JSON.stringify(documents));
    }
  }, [documents, user]);

  useEffect(() => {
    localStorage.setItem("schemebridge_schemes", JSON.stringify(schemes));
  }, [schemes]);

  useEffect(() => {
    localStorage.setItem("schemebridge_applications", JSON.stringify(applications));
  }, [applications]);

  useEffect(() => {
    localStorage.setItem("schemebridge_saved", JSON.stringify(savedSchemes));
  }, [savedSchemes]);

  // Handled by user-dependent sync effects above

  useEffect(() => {
    localStorage.setItem("schemebridge_notifications", JSON.stringify(notifications));
  }, [notifications]);

  useEffect(() => {
    localStorage.setItem("schemebridge_audit_logs", JSON.stringify(auditLogs));
  }, [auditLogs]);

  useEffect(() => {
    localStorage.setItem("schemebridge_grievances", JSON.stringify(grievances));
  }, [grievances]);

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
  const updateProfile = (updates) => {
    setProfile((prev) => ({ ...prev, ...updates }));
    // Log profile update if it completes wizard
    if (updates.isComplete) {
      addAuditLog("Edit", "Profile", updates.name || profile.name, `Citizen profile updated and socio-economic attributes locked.`, updates.name || profile.name);
    }
  };

  // ── Application Actions ───────────────────────────────────────────────────
  const applyToScheme = (scheme) => {
    const alreadyApplied = applications.some((a) => a.schemeId === scheme.id);
    if (alreadyApplied) {
return false;
}

    const now = new Date().toISOString().split("T")[0];
    const newApp = {
      id: generateId(),
      schemeId: scheme.id,
      schemeName: scheme.name,
      ministry: scheme.ministry,
      applicantName: profile.name,
      applicantState: profile.state,
      applicantIncome: `₹${Number(profile.annualIncome).toLocaleString("en-IN")}`,
      applicantCaste: profile.caste,
      appliedDate: now,
      currentStage: "Submitted",
      referenceNo: `${scheme.id.toUpperCase().slice(0, 3)}/${(profile.state || "IN").slice(0, 2).toUpperCase()}/2026/${generateId().split("-")[1]}`,
      stageHistory: [
        { stage: "Submitted", date: now, note: "Application submitted via SchemeBridge portal." },
      ],
      nextAction: "Your application has been received. Aadhaar verification will begin within 3 working days.",
    };

    // Remove from saved if it was saved before applying
    setSavedSchemes((prev) => prev.filter((s) => s.schemeId !== scheme.id));
    setApplications((prev) => [newApp, ...prev]);
    addAuditLog("Create", "Application", scheme.name, `Citizen ${profile.name} submitted application reference ${newApp.referenceNo}.`, profile.name);
    return true;
  };

  const hasApplied = (schemeId) => applications.some((a) => a.schemeId === schemeId);

  const updateApplicationStatus = (appId, newStage) => {
    const now = new Date().toISOString().split("T")[0];
    const nextActionMap = {
      "Submitted": "Application received. Aadhaar verification will begin within 3 working days.",
      "Under Review": "Your documents are being reviewed by the department. No action needed.",
      "Approved": "Congratulations! Disbursement will be initiated within 10 working days.",
      "Rejected": "Your application was rejected. Review the reason and reapply if eligible.",
    };

    let schemeName = "Scheme";
    let applicantName = "Citizen";

    setApplications((prev) =>
      prev.map((a) => {
        if (a.id === appId) {
          schemeName = a.schemeName;
          applicantName = a.applicantName;
          return {
            ...a,
            currentStage: newStage,
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
    addAuditLog("Status Change", "Application", schemeName, `Application ${appId} for ${applicantName} updated to status '${newStage}'.`, "Sanjay Kumar (Admin)");
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

  // ── Notification Actions ──────────────────────────────────────────────────
  const markNotificationRead = (id) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const markAllNotificationsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const dismissNotification = (id) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  const addNotification = (notif) => {
    const newNotif = {
      id: `NOTIF-${  Date.now()}`,
      timestamp: new Date().toISOString(),
      read: false,
      priority: "normal",
      ...notif,
    };
    setNotifications((prev) => [newNotif, ...prev]);
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  // ── Document Actions ──────────────────────────────────────────────────────
  const addDocument = (name, type, issuer = "Self-Uploaded", expiryDate = "No Expiration", status = "uploaded", source = "Manual") => {
    const newDoc = {
      id: Date.now(),
      name,
      type,
      status,
      issuer: issuer || "Self-Uploaded",
      expiryDate: expiryDate || "No Expiration",
      date: new Date().toISOString().split("T")[0],
      source: source || "Manual",
      linkedSchemes: [],
    };
    setDocuments((prev) => [...prev, newDoc]);
    addAuditLog("Create", "Document", name, `Document '${name}' uploaded to vault.`, profile.name);
  };

  // Import multiple documents (DigiLocker batch import)
  const importDocuments = (docs) => {
    const now = new Date().toISOString().split("T")[0];
    const imported = docs.map((doc) => ({
      id: Date.now() + Math.random(),
      name: doc.name,
      type: doc.type,
      status: doc.status || "verified",
      issuer: doc.issuer || "DigiLocker",
      expiryDate: doc.expiry || "No Expiration",
      date: now,
      source: "DigiLocker",
      linkedSchemes: [],
    }));
    setDocuments((prev) => {
      const existingNames = new Set(prev.map((d) => d.name.toLowerCase()));
      const fresh = imported.filter((d) => !existingNames.has(d.name.toLowerCase()));
      return [...prev, ...fresh];
    });
    if (imported.length > 0) {
      addAuditLog("Create", "Document", `${imported.length} documents`, `${imported.length} document(s) imported from DigiLocker.`, profile.name);
    }
  };

  const removeDocument = (docId) => {
    const doc = documents.find((d) => d.id === docId);
    const docName = doc ? doc.name : "Document";
    setDocuments((prev) => prev.filter((d) => d.id !== docId));
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
    const newGrievance = {
      id: `GRV-${  Math.floor(1000 + Math.random() * 9000)}`,
      citizenName: profile?.name || "Citizen",
      citizenPhone: grievance.phone,
      citizenEmail: grievance.email,
      relatedScheme: grievance.relatedScheme,
      category: grievance.category,
      description: grievance.description,
      supportingNote: grievance.supportingNote || "",
      status: "Received",
      date: new Date().toISOString().split("T")[0],
    };
    setGrievances((prev) => [newGrievance, ...prev]);
    addAuditLog("Create", "Grievance", grievance.category, `Grievance reference ${newGrievance.id} submitted for ${grievance.relatedScheme}.`, profile?.name || "Citizen");
    return newGrievance.id;
  };

  const updateGrievanceStatus = (id, newStatus) => {
    let category = "Grievance";
    let relatedScheme = "Scheme";
    setGrievances((prev) =>
      prev.map((g) => {
        if (g.id === id) {
          category = g.category;
          relatedScheme = g.relatedScheme;
          return { ...g, status: newStatus };
        }
        return g;
      })
    );
    addAuditLog("Status Change", "Grievance", category, `Grievance ${id} status updated to '${newStatus}' for ${relatedScheme}.`, "Sanjay Kumar (Admin)");
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
        updateApplicationStatus,
        // Saved / Tracker
        savedSchemes,
        saveScheme,
        isSaved,
        updateSavedStage,
        removeSaved,
        // Documents
        documents,
        addDocument,
        importDocuments,
        removeDocument,
        updateDocumentStatus,
        linkDocument,
        // Notifications
        notifications,
        unreadCount,
        markNotificationRead,
        markAllNotificationsRead,
        dismissNotification,
        addNotification,
        // Audit logs
        auditLogs,
        addAuditLog,
        // Language & i18n
        language,
        toggleLanguage,
        changeLanguage,
        t,
        // Grievances
        grievances,
        submitGrievance,
        updateGrievanceStatus,
        // Utilities
        resetData,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};
