import React, { useState, useEffect, useRef } from "react";
import { Bot, X, Send } from "lucide-react";
import { useApp } from "../context/AppContext";

const AI_RESPONSES = {
  eligibility: "Based on your profile (Age: {age}, Income: ₹{income}/yr, Category: {caste}), you are eligible for {count} government schemes. Your top matches include PM-KISAN, PMUY, and APY.",
  documents: "Your Document Vault has {verified} verified documents. You're missing documents for {missing} saved schemes. Uploading your Khatauni will unlock 3 more schemes.",
  apply: "You have {ready} schemes ready to apply right now with your current documents! I recommend starting with Atal Pension Yojana — it has a June 30 deadline.",
  deadline: "⚠️ Deadline Alert: The application window for PM-KISAN closes in 8 days. You have all required documents. I recommend applying today!",
  kisan: "Pradhan Mantri Kisan Samman Nidhi (PM-KISAN) provides ₹6,000/year. Based on your profile, you match because you are a Farmer. You have uploaded your Land Records (Khatauni) which is currently pending review. Let's get it verified to unlock full eligibility!",
  awas: "PMAY-Urban provides interest subsidies for housing. Based on your income of ₹{income}, you qualify under the EWS category. You'll need to submit an Income Certificate and an affidavit stating you don't own a pucca house.",
  pension: "Atal Pension Yojana (APY) offers a guaranteed monthly pension. Since your age is {age}, you contribution window is active for 28 years until retirement. You have 100% document readiness! You can apply directly.",
  default: "I'm SchemeAI, your intelligent government scheme assistant. Ask me about your eligibility, required documents, deadlines, or how to apply for any scheme!",
  contextDocuments: "I see you're viewing your Document Vault. You can ask me which documents are missing, how to verify them, or what schemes they unlock.",
  contextTracker: "I see you're checking your Application Tracker. You can ask me about expected processing times or how to file a grievance for a delayed application.",
  contextRecommendations: "I see you're browsing Schemes. Ask me to find schemes matching specific criteria, or check your eligibility score for a particular scheme!"
};

export default function SchemeAIChatWidget({ isOpen, onClose, initialQuery = "", pageContext = "", contextData = null }) {
  const { profile, schemes, documents, language } = useApp();

  // Build a dynamic welcome message from contextData
  const buildWelcomeMessage = () => {
    if (contextData?.page === "documents") {
      return `👋 I can see your Document Vault. You have ${contextData.totalDocs} documents, ${contextData.verifiedDocs} verified${
        contextData.expiredDocs > 0 ? `, and ${contextData.expiredDocs} expired` : ""
      }. Your vault readiness is ${contextData.vaultScore}%.${
        contextData.missingSummary ? ` Missing: ${contextData.missingSummary}.` : ""
      } How can I help you today?`;
    }
    if (contextData?.page === "schemeDetails") {
      const missingStr = contextData.missingDocs?.length > 0
        ? `Missing documents: ${contextData.missingDocs.slice(0, 2).join(", ")}${
            contextData.missingDocs.length > 2 ? " and more" : ""
          }.`
        : "All required documents are present in your vault.";
      return `👋 I'm looking at **${contextData.schemeName}**. Your document readiness for this scheme is ${contextData.readinessScore}%. ${missingStr} Ask me anything about this scheme!`;
    }
    return "👋 Hello! I'm SchemeAI, your personalized e-governance assistant. I have analyzed your profile. Ask me anything about matching schemes, missing documents, or upcoming deadlines!";
  };

  const [messages, setMessages] = useState([
    { from: "ai", text: buildWelcomeMessage() }
  ]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef(null);

  // Auto-scroll to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

  // Handle prefilled/initial query trigger
  useEffect(() => {
    if (isOpen && initialQuery) {
      // Small timeout to let opening animation finish
      const timer = setTimeout(() => {
        handleSend(initialQuery);
      }, 300);
      return () => clearTimeout(timer);
    }
  }, [isOpen, initialQuery]);

  const getResponse = (query) => {
    const q = query.toLowerCase();
    const age = profile?.age || 32;
    const incomeVal = profile?.annualIncome || profile?.income || 180000;
    const income = Number(incomeVal).toLocaleString("en-IN");
    const caste = profile?.caste || "OBC";

    // Simple filter to match count
    const matchingCount = schemes.filter(s => {
      const minAge = s.eligibility?.minAge ?? 0;
      const maxAge = s.eligibility?.maxAge ?? 100;
      const maxInc = s.eligibility?.maxIncome ?? Infinity;
      return age >= minAge && age <= maxAge && incomeVal <= maxInc;
    }).length;

    const verifiedCount = documents.filter(d => d.status === "verified").length;

    if (q.includes("pm-kisan") || q.includes("kisan") || q.includes("farmer")) {
      return AI_RESPONSES.kisan;
    }
    if (q.includes("awas") || q.includes("housing") || q.includes("house") || q.includes("pmay")) {
      return AI_RESPONSES.awas.replace("{income}", income);
    }
    if (q.includes("pension") || q.includes("atal") || q.includes("apy")) {
      return AI_RESPONSES.pension.replace("{age}", age);
    }
    if (q.includes("elig") || q.includes("qualify") || q.includes("match")) {
      return AI_RESPONSES.eligibility
        .replace("{age}", age)
        .replace("{income}", income)
        .replace("{caste}", caste)
        .replace("{count}", matchingCount);
    }
    if (q.includes("doc") || q.includes("upload") || q.includes("vault")) {
      return AI_RESPONSES.documents
        .replace("{verified}", verifiedCount)
        .replace("{missing}", "2");
    }
    if (q.includes("apply") || q.includes("start") || q.includes("ready")) {
      return AI_RESPONSES.apply.replace("{ready}", Math.max(0, matchingCount - 1));
    }
    if (q.includes("deadline") || q.includes("when") || q.includes("date") || q.includes("expir")) {
      return AI_RESPONSES.deadline;
    }

    // Provide context-aware generic response if no specific keyword matches
    if (contextData?.page === "documents") {
      if (q.includes("miss") || q.includes("upload") || q.includes("need")) {
        return contextData.missingSummary
          ? `Based on your current vault, you are missing: ${contextData.missingSummary}. Uploading these documents will improve your scheme eligibility significantly.`
          : "Your vault looks complete! All commonly required documents are present.";
      }
      if (q.includes("expir") || q.includes("renew")) {
        return contextData.expiredDocs > 0
          ? `You have ${contextData.expiredDocs} expired document(s). Please renew them via the respective issuing authority to avoid eligibility disruptions.`
          : "None of your documents are currently expired. Keep checking back!";
      }
      return AI_RESPONSES.contextDocuments;
    }
    if (contextData?.page === "schemeDetails") {
      if (q.includes("miss") || q.includes("upload") || q.includes("need") || q.includes("document")) {
        const missingStr = contextData.missingDocs?.length > 0
          ? `For ${contextData.schemeName}, you still need: ${contextData.missingDocs.join(", ")}. Upload these in your Document Vault.`
          : `Great news! All required documents for ${contextData.schemeName} are already in your vault.`;
        return missingStr;
      }
      return `For ${contextData.schemeName}, your readiness is ${contextData.readinessScore}%. ${contextData.readinessScore === 100 ? "You are ready to apply!" : "Upload the missing documents to qualify fully."}`;
    }
    if (pageContext.includes("/documents")) {
return AI_RESPONSES.contextDocuments;
}
    if (pageContext.includes("/tracker")) {
return AI_RESPONSES.contextTracker;
}
    if (pageContext.includes("/recommendations")) {
return AI_RESPONSES.contextRecommendations;
}

    return AI_RESPONSES.default;
  };

  const handleSend = (textToSend = input) => {
    const text = typeof textToSend === "string" ? textToSend : input;
    if (!text.trim()) {
return;
}

    // Add user message
    const userMsg = { from: "user", text: text };
    setMessages(prev => [...prev, userMsg]);
    setInput("");
    setIsTyping(true);

    setTimeout(() => {
      setIsTyping(false);
      setMessages(prev => [
        ...prev,
        { from: "ai", text: getResponse(text) }
      ]);
    }, 1000);
  };

  if (!isOpen) {
return null;
}

  return (
    <div className="fixed bottom-24 right-6 z-50 w-[340px] sm:w-[380px] bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col transition-all duration-300 transform scale-100" style={{ height: "480px" }}>
      {/* Chat Widget Header */}
      <div className="bg-gradient-to-r from-slate-900 to-indigo-900 px-4 py-3.5 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2">
          <div className="bg-indigo-500/20 p-2 rounded-lg border border-indigo-400/20 animate-pulse">
            <Bot className="h-4 w-4 text-indigo-400" />
          </div>
          <div>
            <p className="text-white font-bold text-xs tracking-wide">SchemeAI Assistant</p>
            <p className="text-indigo-300 text-[9px] uppercase tracking-wider font-semibold">Active E-Gov Consult</p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="text-slate-400 hover:text-white transition p-1 bg-slate-800/40 rounded-full hover:bg-slate-800"
          aria-label="Close Assistant"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Suggested Quick Prompts */}
      <div className="px-3 py-2.5 bg-slate-50 border-b border-slate-100 shrink-0">
        <p className="text-[8px] text-slate-400 uppercase font-bold tracking-wider mb-1.5">Suggested Queries</p>
        <div className="flex flex-wrap gap-1.5">
          {[
            { label: "Am I eligible?", query: "Am I eligible for any schemes?" },
            { label: "Missing docs?", query: "Which documents are missing in my vault?" },
            { label: "APY details", query: "Tell me about Atal Pension Yojana" }
          ].map((item, idx) => (
            <button
              key={idx}
              onClick={() => handleSend(item.query)}
              className="text-[9px] px-2.5 py-1 bg-white hover:bg-slate-100 text-slate-700 rounded-full border border-slate-200 font-bold shadow-xs transition"
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {/* Messages Window */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-slate-50/50">
        {messages.map((msg, i) => (
          <div key={i} className={`flex ${msg.from === "user" ? "justify-end" : "justify-start"}`}>
            {msg.from === "ai" && (
              <div className="bg-indigo-600/10 text-indigo-600 border border-indigo-100 p-1.5 rounded-xl h-7 w-7 flex items-center justify-center mr-2 shrink-0 mt-0.5 shadow-sm">
                <Bot className="h-3.5 w-3.5" />
              </div>
            )}
            <div className={`max-w-[78%] px-3.5 py-2.5 rounded-2xl text-xs leading-relaxed shadow-xs ${
              msg.from === "user"
                ? "bg-slate-900 text-white rounded-tr-none font-medium"
                : "bg-white text-slate-700 border border-slate-200/60 rounded-tl-none font-medium"
            }`}>
              {msg.text}
            </div>
          </div>
        ))}
        {isTyping && (
          <div className="flex justify-start">
            <div className="bg-indigo-600/10 text-indigo-600 border border-indigo-100 p-1.5 rounded-xl h-7 w-7 flex items-center justify-center mr-2 shrink-0">
              <Bot className="h-3.5 w-3.5 animate-bounce" />
            </div>
            <div className="bg-white border border-slate-200/60 px-3.5 py-2.5 rounded-2xl rounded-tl-none shadow-xs">
              <div className="flex gap-1 items-center h-3">
                <span className="h-1.5 w-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "0ms" }} />
                <span className="h-1.5 w-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                <span className="h-1.5 w-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Form Footer */}
      <div className="p-3 border-t border-slate-100 bg-white shrink-0">
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === "Enter" && handleSend()}
            placeholder="Type a question about schemes..."
            className="flex-1 text-xs px-3.5 py-2.5 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-slate-950 focus:border-transparent bg-slate-50 focus:bg-white transition"
          />
          <button
            onClick={() => handleSend()}
            disabled={!input.trim()}
            className="bg-slate-900 hover:bg-slate-800 disabled:opacity-40 disabled:hover:bg-slate-900 text-white p-2.5 rounded-xl transition flex items-center justify-center shrink-0"
            aria-label="Send Message"
          >
            <Send className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
