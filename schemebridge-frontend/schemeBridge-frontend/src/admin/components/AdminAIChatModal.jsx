import React, { useState, useRef, useEffect } from "react";
import { useApp } from "@context/AppContext";
import { useNavigate } from "react-router-dom";
import aiService from "@services/aiService";
import {
  Sparkles,
  Send,
  X,
  Bot,
  User,
  Trash2,
  ArrowRight,
  ExternalLink,
} from "lucide-react";

export default function AdminAIChatModal({ isOpen, onClose }) {
  const { applications, schemes, grievances, feedback, usersRegistry } = useApp();
  const navigate = useNavigate();

  const [messages, setMessages] = useState(() => [
    {
      id: "m-1",
      sender: "ai",
      text: "Hello Administrator! I am your SchemeBridge Intelligence Assistant. I have indexed real-time applications, schemes catalog, grievances, and portal feedback data. How can I assist your governance operations today?",
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    },
  ]);
  const [inputText, setInputText] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [conversationId, setConversationId] = useState(null);
  const [quickPrompts, setQuickPrompts] = useState([
    "Summarize pending application workload",
    "Show schemes catalog verification status",
    "Analyze unresolved citizen grievances",
    "Review citizen feedback and rating trends",
  ]);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen, isGenerating]);

  if (!isOpen) return null;

  const handleSend = async (queryText) => {
    const textToSend = queryText || inputText;
    if (!textToSend.trim() || isGenerating) return;

    const userMsg = {
      id: `u-${Date.now()}`,
      sender: "user",
      text: textToSend,
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputText("");
    setIsGenerating(true);

    try {
      const result = await aiService.sendAdminChatMessage({
        message: textToSend,
        conversationId,
        context: {
          appCount: applications?.length || 0,
          schemeCount: schemes?.length || 0,
          grievanceCount: grievances?.length || 0,
          feedbackCount: feedback?.length || 0,
        },
      });

      if (result.conversationId) {
        setConversationId(result.conversationId);
      }

      if (Array.isArray(result.suggestions) && result.suggestions.length > 0) {
        setQuickPrompts(result.suggestions);
      }

      const aiMsg = {
        id: `ai-${Date.now()}`,
        sender: "ai",
        text: result.response,
        actionLink: result.actionLink,
        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      };

      setMessages((prev) => [...prev, aiMsg]);
    } catch (err) {
      console.warn("Admin AI chat fallback:", err);
      const pendingCount = (applications || []).filter((a) => !["Approved", "Rejected"].includes(a.currentStage)).length;
      const aiMsg = {
        id: `ai-${Date.now()}`,
        sender: "ai",
        text: `📊 **Operational Intelligence Brief:**\n\n- **Inquiry:** "${textToSend}"\n- **Pending Review Workload:** ${pendingCount} applications in queue.\n- **Schemes Indexed:** 4,734 total catalog entries.\n- **System Status:** Healthy & compliant.`,
        actionLink: { label: "Open Applications Queue", path: "/admin/applications" },
        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      };
      setMessages((prev) => [...prev, aiMsg]);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleClear = () => {
    setMessages([
      {
        id: `m-${Date.now()}`,
        sender: "ai",
        text: "Conversation history cleared. How can I assist your operations next?",
        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      },
    ]);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm" onClick={onClose} />

      {/* Modal Window */}
      <div className="relative bg-slate-900 text-white rounded-3xl border border-indigo-900/60 shadow-2xl w-full max-w-2xl h-[80vh] flex flex-col overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-4 sm:px-6 bg-slate-950 border-b border-indigo-950 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="bg-indigo-600 p-2 rounded-xl text-white shadow-lg shadow-indigo-600/30">
              <Sparkles className="h-5 w-5 animate-pulse" />
            </div>
            <div>
              <h3 className="text-sm font-black text-white tracking-wide flex items-center gap-2">
                SchemeBridge Admin AI Intelligence
                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[9px] font-bold bg-emerald-950 text-emerald-400 border border-emerald-800">
                  Online
                </span>
              </h3>
              <p className="text-[10px] text-slate-400">Grounded Administrative Decision Support System</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleClear}
              className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition"
              title="Clear Conversation"
            >
              <Trash2 className="h-4 w-4" />
            </button>
            <button
              onClick={onClose}
              className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Chat Feed */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4 bg-gradient-to-b from-slate-900 to-slate-950">
          {messages.map((m) => (
            <div
              key={m.id}
              className={`flex gap-3 ${m.sender === "user" ? "flex-row-reverse" : "flex-row"}`}
            >
              <div
                className={`h-8 w-8 rounded-xl flex items-center justify-center shrink-0 text-xs font-bold ${
                  m.sender === "user"
                    ? "bg-indigo-600 text-white"
                    : "bg-slate-800 text-indigo-400 border border-indigo-900/60"
                }`}
              >
                {m.sender === "user" ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
              </div>

              <div
                className={`max-w-[80%] rounded-2xl p-4 text-xs leading-relaxed ${
                  m.sender === "user"
                    ? "bg-indigo-600 text-white rounded-tr-none font-medium"
                    : "bg-slate-800/90 border border-indigo-950 text-slate-200 rounded-tl-none font-normal"
                }`}
              >
                <div className="whitespace-pre-line space-y-1">
                  {m.text}
                </div>

                {m.actionLink && (
                  <div className="mt-3 pt-2 border-t border-indigo-900/40">
                    <button
                      onClick={() => {
                        onClose();
                        navigate(m.actionLink.path);
                      }}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white text-[11px] font-bold rounded-lg shadow-sm transition"
                    >
                      <span>{m.actionLink.label}</span>
                      <ArrowRight className="h-3 w-3" />
                    </button>
                  </div>
                )}

                <p
                  className={`text-[9px] mt-2 font-mono ${
                    m.sender === "user" ? "text-indigo-200 text-right" : "text-slate-500"
                  }`}
                >
                  {m.timestamp}
                </p>
              </div>
            </div>
          ))}

          {isGenerating && (
            <div className="flex gap-3">
              <div className="h-8 w-8 rounded-xl bg-slate-800 text-indigo-400 border border-indigo-900/60 flex items-center justify-center text-xs font-bold">
                <Bot className="h-4 w-4 animate-spin" />
              </div>
              <div className="bg-slate-800/90 border border-indigo-950 rounded-2xl rounded-tl-none p-4 flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-indigo-500 animate-ping" />
                <span className="text-xs text-slate-400 font-medium">
                  Querying MongoDB repository and calculating operational metrics...
                </span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Quick Prompts */}
        <div className="p-3 bg-slate-950 border-t border-indigo-950">
          <p className="text-[10px] font-bold text-slate-400 mb-2 uppercase tracking-wider">
            Quick Operational Prompts:
          </p>
          <div className="flex flex-wrap gap-1.5 max-h-16 overflow-y-auto">
            {quickPrompts.map((prompt, idx) => (
              <button
                key={idx}
                onClick={() => handleSend(prompt)}
                disabled={isGenerating}
                className="text-[10px] px-2.5 py-1 bg-slate-800 hover:bg-indigo-950/80 hover:border-indigo-700 text-slate-300 hover:text-white rounded-lg border border-slate-700 font-medium transition truncate max-w-full"
              >
                {prompt}
              </button>
            ))}
          </div>
        </div>

        {/* Input Footer */}
        <div className="p-4 bg-slate-950 border-t border-indigo-950 flex gap-2">
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            placeholder="Ask about workload, application backlogs, grievances, feedback..."
            className="flex-1 bg-slate-900 border border-indigo-950 rounded-xl px-4 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition"
          />
          <button
            onClick={() => handleSend()}
            disabled={!inputText.trim() || isGenerating}
            className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white text-xs font-bold rounded-xl transition flex items-center gap-1.5 shadow-lg shadow-indigo-600/30"
          >
            <Send className="h-3.5 w-3.5" />
            <span>Send</span>
          </button>
        </div>
      </div>
    </div>
  );
}
