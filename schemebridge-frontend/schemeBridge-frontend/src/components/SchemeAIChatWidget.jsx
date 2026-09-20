import React, { useState, useEffect, useRef } from "react";
import { Bot, X, Send, ArrowRight, ExternalLink } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useApp } from "../context/AppContext";
import aiService from "../services/aiService";

export default function SchemeAIChatWidget({ isOpen, onClose, initialQuery = "", pageContext = "", contextData = null }) {
  const { profile, schemes, documents } = useApp();
  const navigate = useNavigate();

  // Build a dynamic welcome message from contextData
  const buildWelcomeMessage = () => {
    if (contextData?.page === "documents") {
      return `👋 I can see your Document Vault. You have ${contextData.totalDocs || documents.length} documents, ${contextData.verifiedDocs || 0} verified. Your vault readiness is ${contextData.vaultScore || 80}%. How can I assist your document verification today?`;
    }
    if (contextData?.page === "schemeDetails") {
      return `👋 I'm looking at **${contextData.schemeName || "this scheme"}**. Your document readiness for this scheme is ${contextData.readinessScore || 100}%. Ask me anything about eligibility criteria, required certificates, or how to apply!`;
    }
    return "👋 Hello! I'm SchemeAI, your intelligent e-governance assistant. I have analyzed your profile. Ask me about matching government schemes, required documents, or application status!";
  };

  const [messages, setMessages] = useState([
    { from: "ai", text: buildWelcomeMessage() }
  ]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [conversationId, setConversationId] = useState(null);
  const [aiSuggestions, setAiSuggestions] = useState([
    "Which schemes match my profile?",
    "Which documents do I need to upload?",
    "How to apply for PM-KISAN?"
  ]);
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
      const timer = setTimeout(() => {
        handleSend(initialQuery);
      }, 300);
      return () => clearTimeout(timer);
    }
  }, [isOpen, initialQuery]);

  const handleSend = async (textToSend = input) => {
    const text = typeof textToSend === "string" ? textToSend : input;
    if (!text.trim()) return;

    const userMsg = { from: "user", text };
    setMessages(prev => [...prev, userMsg]);
    setInput("");
    setIsTyping(true);

    try {
      const result = await aiService.sendChatMessage({
        message: text,
        conversationId,
        context: {
          profile,
          documents,
          page: pageContext,
          contextData,
        },
      });

      if (result.conversationId) {
        setConversationId(result.conversationId);
      }

      if (Array.isArray(result.suggestions) && result.suggestions.length > 0) {
        setAiSuggestions(result.suggestions);
      }

      setIsTyping(false);
      setMessages(prev => [
        ...prev,
        {
          from: "ai",
          text: result.response,
          actionLink: result.actionLink,
          relatedSchemes: result.relatedSchemes,
        },
      ]);
    } catch (err) {
      console.warn("SchemeAI service fallback:", err);
      setIsTyping(false);
      setMessages(prev => [
        ...prev,
        {
          from: "ai",
          text: `Based on your profile, I recommend exploring verified central and state welfare programs. You can review your Document Vault to ensure all certificates are up to date.`,
          actionLink: { label: "Explore Schemes", path: "/schemes" },
        },
      ]);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 sm:inset-auto sm:bottom-20 sm:right-6 z-50 w-full sm:w-[400px] h-[100dvh] sm:h-[520px] bg-white dark:bg-slate-900 sm:rounded-2xl shadow-2xl border-t sm:border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col transition-all duration-300 transform scale-100" aria-label="AI Assistant Chat">
      {/* Chat Widget Header */}
      <div className="bg-gradient-to-r from-government-blue to-indigo-900 px-4 py-3.5 flex items-center justify-between shrink-0 shadow-sm">
        <div className="flex items-center gap-2.5">
          <div className="bg-white/10 p-2 rounded-lg border border-white/20">
            <Bot className="h-4 w-4 text-white" />
          </div>
          <div>
            <p className="text-white font-bold text-xs tracking-wide">SchemeAI Assistant</p>
            <p className="text-indigo-200 text-[10px] uppercase tracking-wider font-medium">Grounded E-Gov Intelligence</p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="text-white/70 hover:text-white transition p-1.5 bg-black/20 rounded-full hover:bg-black/40"
          aria-label="Close Assistant"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Suggested Quick Prompts */}
      <div className="px-3 py-2 bg-slate-50 dark:bg-slate-800/60 border-b border-slate-200/80 dark:border-slate-700/60 shrink-0">
        <p className="text-[9px] text-slate-500 dark:text-slate-400 uppercase font-bold tracking-wider mb-1.5">Suggested Queries</p>
        <div className="flex flex-wrap gap-1.5 max-h-16 overflow-y-auto">
          {aiSuggestions.map((suggestion, idx) => (
            <button
              key={`ai-${idx}`}
              onClick={() => handleSend(suggestion)}
              className="text-[10px] px-2.5 py-1 bg-white dark:bg-slate-800 hover:bg-indigo-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-full border border-slate-200 dark:border-slate-700 font-medium shadow-2xs transition truncate max-w-full"
            >
              {suggestion}
            </button>
          ))}
        </div>
      </div>

      {/* Messages Window */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-slate-50/40 dark:bg-slate-900/50" role="log" aria-live="polite">
        {messages.map((msg, i) => (
          <div key={i} className={`flex ${msg.from === "user" ? "justify-end" : "justify-start"}`}>
            {msg.from === "ai" && (
              <div className="bg-government-blue/10 text-government-blue dark:bg-indigo-900/40 dark:text-indigo-300 border border-government-blue/20 dark:border-indigo-800 p-1.5 rounded-xl h-7 w-7 flex items-center justify-center mr-2 shrink-0 mt-0.5 shadow-2xs">
                <Bot className="h-3.5 w-3.5" />
              </div>
            )}
            <div className={`max-w-[82%] px-3.5 py-2.5 rounded-2xl text-xs leading-relaxed shadow-2xs ${
              msg.from === "user"
                ? "bg-government-blue text-white rounded-tr-none font-medium"
                : "bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 border border-slate-200 dark:border-slate-700 rounded-tl-none font-normal"
            }`}>
              <div className="whitespace-pre-line space-y-1">
                {msg.text}
              </div>

              {/* Related Schemes preview */}
              {msg.relatedSchemes && msg.relatedSchemes.length > 0 && (
                <div className="mt-2.5 pt-2 border-t border-slate-100 dark:border-slate-700 space-y-1.5">
                  <p className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase">Related Schemes:</p>
                  {msg.relatedSchemes.map((s, idx) => (
                    <button
                      key={idx}
                      onClick={() => {
                        onClose();
                        navigate(`/schemes/${s.slug || s.schemeCode}`);
                      }}
                      className="w-full text-left p-1.5 rounded-lg bg-indigo-50/50 dark:bg-slate-700/50 hover:bg-indigo-100 dark:hover:bg-slate-700 border border-indigo-100 dark:border-slate-600 flex items-center justify-between text-[11px] font-medium text-government-blue dark:text-indigo-300 transition"
                    >
                      <span className="truncate">{s.title || s.schemeCode}</span>
                      <ExternalLink className="h-3 w-3 shrink-0 ml-1 opacity-70" />
                    </button>
                  ))}
                </div>
              )}

              {/* Action link button */}
              {msg.actionLink && msg.actionLink.path && (
                <div className="mt-2.5 pt-1.5">
                  <button
                    onClick={() => {
                      onClose();
                      navigate(msg.actionLink.path);
                    }}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-government-blue text-white hover:bg-government-blue-dark rounded-lg text-[11px] font-semibold transition shadow-2xs"
                  >
                    <span>{msg.actionLink.label || "View Details"}</span>
                    <ArrowRight className="h-3 w-3" />
                  </button>
                </div>
              )}
            </div>
          </div>
        ))}
        {isTyping && (
          <div className="flex justify-start">
            <div className="bg-government-blue/10 text-government-blue dark:bg-indigo-900/40 dark:text-indigo-300 border border-government-blue/20 dark:border-indigo-800 p-1.5 rounded-xl h-7 w-7 flex items-center justify-center mr-2 shrink-0">
              <Bot className="h-3.5 w-3.5 animate-bounce" />
            </div>
            <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 px-3.5 py-2.5 rounded-2xl rounded-tl-none shadow-2xs">
              <div className="flex gap-1 items-center h-3">
                <span className="h-1.5 w-1.5 bg-slate-400 dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: "0ms" }} />
                <span className="h-1.5 w-1.5 bg-slate-400 dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                <span className="h-1.5 w-1.5 bg-slate-400 dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Form Footer */}
      <div className="p-3 border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shrink-0">
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            placeholder="Ask about schemes, eligibility, documents..."
            className="flex-1 text-xs px-3.5 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-government-blue bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:bg-white dark:focus:bg-slate-800 transition"
          />
          <button
            onClick={() => handleSend()}
            disabled={!input.trim()}
            className="bg-government-blue hover:bg-government-blue-dark disabled:opacity-40 text-white p-2.5 rounded-xl transition flex items-center justify-center shrink-0 shadow-2xs"
            aria-label="Send Message"
          >
            <Send className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
