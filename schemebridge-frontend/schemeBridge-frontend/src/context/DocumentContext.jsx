/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useRef } from "react";
import { DEFAULT_DOCUMENTS } from "../data/mockDocuments";
import { useAuth } from "./AuthContext";
import { safeGetItem, safeSetItem } from "../utils/storage";

const DocumentContext = createContext(null);

export const useDocuments = () => {
  const context = useContext(DocumentContext);
  if (!context) {
    throw new Error("useDocuments must be used within a DocumentProvider");
  }
  return context;
};

export const DocumentProvider = ({ children }) => {
  const { user } = useAuth();
  const lastSyncedDocsEmail = useRef(user?.email || "");

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

  // Sync documents when user changes
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

  // Persist documents to safe storage
  useEffect(() => {
    if (user && user.email === lastSyncedDocsEmail.current) {
      const docKey = `schemebridge_documents_${user.email || "default"}`;
      safeSetItem(docKey, documents);
    }
    if (!user || user.email === lastSyncedDocsEmail.current) {
      safeSetItem("schemebridge_documents", documents);
    }
  }, [documents, user]);

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
  };

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
  };

  const removeDocument = (docId) => {
    setDocuments((prev) => prev.filter((d) => d.id !== docId));
  };

  const updateDocumentStatus = (docId, status) => {
    setDocuments((prev) =>
      prev.map((d) => {
        if (d.id === docId) {
          return { ...d, status, date: new Date().toISOString().split("T")[0] };
        }
        return d;
      })
    );
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

  const getDocumentsByType = (type) => {
    return documents.filter((d) => d.type === type);
  };

  const getVerifiedDocuments = () => {
    return documents.filter((d) => d.status === "verified");
  };

  return (
    <DocumentContext.Provider
      value={{
        documents,
        addDocument,
        importDocuments,
        removeDocument,
        updateDocumentStatus,
        linkDocument,
        getDocumentsByType,
        getVerifiedDocuments,
      }}
    >
      {children}
    </DocumentContext.Provider>
  );
};
