import React, { useState, useEffect } from "react";
import {
  MessageSquare,
  AlertTriangle,
  Clock,
  Send,
  User,
  Paperclip,
  CheckCircle,
  TrendingUp,
  Search,
  ChevronDown,
  ChevronUp,
  Users,
  UserPlus,
  FileText
} from "lucide-react";

// List of available officers for assignment
const AVAILABLE_OFFICERS = [
  "Priya Patel",
  "Amit Singh",
  "Sanjay Kumar",
  "Neha Sharma",
  "Ravi Shankar"
];

export default function GrievanceManagementDesk({ grievances, updateGrievanceStatus }) {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("all");
  const [expandedId, setExpandedId] = useState(null);
  const [replyText, setReplyText] = useState("");
  const [internalNoteText, setInternalNoteText] = useState("");

  // Ticket discussions state mapped by ticket ID
  const [discussions, setDiscussions] = useState({
    "GRV-7401": [
      { sender: "citizen", date: "2026-06-18", text: "Installment for April 2026 has not been credited to my bank account. The tracker status shows approved but bank transfer is pending." },
      { sender: "officer", date: "2026-06-19", text: "We have requested transaction tracking records from State Bank of India. Response pending." }
    ],
    "GRV-5219": [
      { sender: "citizen", date: "2026-06-16", text: "My auto-debit registration was rejected. Uploaded my bank passbook again." },
      { sender: "officer", date: "2026-06-17", text: "Verified passbook details. Routing code updated. Auto-debit successfully verified." }
    ]
  });

  // Internal notes per ticket
  const [internalNotes, setInternalNotes] = useState({
    "GRV-7401": [
      { author: "Amit Singh", date: "2026-06-18", text: "Follow up with SBI nodal officer." }
    ]
  });

  // Assigned officers per ticket
  const [assignedOfficers, setAssignedOfficers] = useState({
    "GRV-7401": "Sanjay Kumar",
    "GRV-5219": "Priya Patel"
  });

  // Timelines per ticket
  const [timelines, setTimelines] = useState({
    "GRV-7401": [
      { date: "2026-06-18", event: "Grievance received", actor: "System" },
      { date: "2026-06-18", event: "Assigned to Sanjay Kumar", actor: "Admin" },
      { date: "2026-06-19", event: "First response sent", actor: "Sanjay Kumar" }
    ],
    "GRV-5219": [
      { date: "2026-06-16", event: "Grievance received", actor: "System" },
      { date: "2026-06-16", event: "Assigned to Priya Patel", actor: "Admin" },
      { date: "2026-06-17", event: "Verified documents", actor: "Priya Patel" },
      { date: "2026-06-17", event: "Resolved", actor: "Priya Patel" }
    ]
  });

  // Simulated countdown timers state
  const [slaTimes, setSlaTimes] = useState({
    "GRV-7401": 7200 + 45 * 60, // 2h 45m in seconds
    "GRV-5219": 0 // Resolved/Closed
  });

  // Tick timers once per second
  useEffect(() => {
    const timer = setInterval(() => {
      setSlaTimes((prev) => {
        const next = { ...prev };
        Object.keys(next).forEach((key) => {
          if (next[key] > 0) {
            next[key] = next[key] - 1;
          }
        });
        return next;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatSla = (seconds) => {
    if (seconds <= 0) return "SLA Met";
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h}h ${m}m ${s}s`;
  };

  const handleReplySubmit = (ticketId) => {
    if (!replyText.trim()) return;
    const nowStr = new Date().toISOString().split("T")[0];
    
    // Update local thread discussion
    const existing = discussions[ticketId] || [];
    setDiscussions({
      ...discussions,
      [ticketId]: [...existing, { sender: "officer", date: nowStr, text: replyText }]
    });

    setReplyText("");
  };

  const handleAddInternalNote = (ticketId) => {
    if (!internalNoteText.trim()) return;
    const nowStr = new Date().toISOString().split("T")[0];
    const existing = internalNotes[ticketId] || [];
    setInternalNotes({
      ...internalNotes,
      [ticketId]: [...existing, { author: "Current Officer", date: nowStr, text: internalNoteText }]
    });
    setInternalNoteText("");
  };

  const handleResolve = (ticketId) => {
    updateGrievanceStatus(ticketId, "Resolved");
    setSlaTimes((prev) => ({ ...prev, [ticketId]: 0 }));
    // Update timeline
    const nowStr = new Date().toISOString().split("T")[0];
    setTimelines({
      ...timelines,
      [ticketId]: [...(timelines[ticketId] || []), { date: nowStr, event: "Resolved", actor: "Current Officer" }]
    });
  };

  const handleEscalate = (ticketId) => {
    updateGrievanceStatus(ticketId, "In Review");
    // Extend SLA timer on escalation
    setSlaTimes((prev) => ({ ...prev, [ticketId]: (prev[ticketId] || 0) + 14400 }));
    // Update timeline
    const nowStr = new Date().toISOString().split("T")[0];
    setTimelines({
      ...timelines,
      [ticketId]: [...(timelines[ticketId] || []), { date: nowStr, event: "Escalated to Nodal Head", actor: "Current Officer" }]
    });
  };

  const handleAssignOfficer = (ticketId, officerName) => {
    setAssignedOfficers({
      ...assignedOfficers,
      [ticketId]: officerName
    });
    // Update timeline
    const nowStr = new Date().toISOString().split("T")[0];
    setTimelines({
      ...timelines,
      [ticketId]: [...(timelines[ticketId] || []), { date: nowStr, event: `Assigned to ${officerName}`, actor: "Admin" }]
    });
  };

  // Map ticket category to priorities
  const enrichedGrievances = React.useMemo(() => {
    return (grievances || []).map((g) => {
      const isResolved = g.status === "Resolved" || g.status === "Closed";
      let priority = "Medium";

      if (g.category.includes("Delay") || g.category.includes("Payment")) {
        priority = "Critical";
      } else if (g.category.includes("Verify") || g.category.includes("Document")) {
        priority = "High";
      }

      return {
        ...g,
        priority,
        assignedOfficer: assignedOfficers[g.id] || "Priya Patel",
        sla: isResolved ? 0 : slaTimes[g.id] ?? (priority === "Critical" ? 3600 : 7200)
      };
    });
  }, [grievances, slaTimes, assignedOfficers]);

  const filteredTickets = React.useMemo(() => {
    return enrichedGrievances
      .filter((g) => filter === "all" || g.status.toLowerCase() === filter.toLowerCase())
      .filter(
        (g) =>
          g.citizenName.toLowerCase().includes(search.toLowerCase()) ||
          g.id.toLowerCase().includes(search.toLowerCase()) ||
          g.category.toLowerCase().includes(search.toLowerCase())
      );
  }, [enrichedGrievances, search, filter]);

  const priorityStyles = {
    Critical: "bg-rose-50 text-rose-700 border-rose-200 animate-pulse",
    High: "bg-amber-50 text-amber-700 border-amber-200",
    Medium: "bg-blue-50 text-blue-700 border-blue-200",
    Low: "bg-slate-100 text-slate-600 border-slate-200"
  };

  const statusStyles = {
    Received: "bg-indigo-50 text-indigo-700 border-indigo-200",
    "In Review": "bg-purple-50 text-purple-700 border-purple-200",
    Resolved: "bg-emerald-50 text-emerald-700 border-emerald-200",
    Closed: "bg-slate-50 text-slate-600 border-slate-200"
  };

  return (
    <div className="space-y-4">
      {/* ── Search & Filter Panel ── */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm flex flex-col md:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search tickets by ID, category, or citizen..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-50 hover:bg-slate-100/50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 transition"
          />
        </div>
        <div className="flex bg-slate-100 p-1 rounded-xl select-none text-[11px] font-bold shrink-0">
          {["all", "received", "in review", "resolved"].map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-3 py-1.5 rounded-lg capitalize transition ${
                filter === f ? "bg-white text-slate-900 shadow-sm" : "text-slate-500 hover:text-slate-700"
              }`}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      {/* ── Ticket Listing Rows ── */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        {filteredTickets.length === 0 ? (
          <div className="p-16 flex flex-col items-center justify-center text-center space-y-3">
            <MessageSquare className="h-10 w-10 text-slate-400" />
            <div>
              <p className="text-sm font-bold text-slate-800">No Grievances Found</p>
              <p className="text-xs text-slate-400 leading-normal max-w-sm mt-0.5">
                All citizen grievance requests are sorted. No pending tickets found matching filters.
              </p>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[9px] select-none">
                <tr>
                  <th className="px-5 py-3.5">Ticket ID</th>
                  <th className="px-5 py-3.5">Priority</th>
                  <th className="px-5 py-3.5">Citizen Details</th>
                  <th className="px-5 py-3.5">Issue Category</th>
                  <th className="px-5 py-3.5 hidden md:table-cell">Scheme Target</th>
                  <th className="px-5 py-3.5">SLA Countdown</th>
                  <th className="px-5 py-3.5">Officer Assigned</th>
                  <th className="px-5 py-3.5">Status</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredTickets.map((ticket) => {
                  const isExpanded = expandedId === ticket.id;
                  const currentThread = discussions[ticket.id] || [];
                  const currentTimeline = timelines[ticket.id] || [];
                  const currentInternalNotes = internalNotes[ticket.id] || [];

                  return (
                    <React.Fragment key={ticket.id}>
                      <tr
                        className={`hover:bg-slate-50 transition cursor-pointer font-semibold ${
                          isExpanded ? "bg-slate-50/50" : ""
                        }`}
                        onClick={() => setExpandedId(isExpanded ? null : ticket.id)}
                      >
                        <td className="px-5 py-3.5 font-mono text-slate-500 text-[10px] font-bold">
                          {ticket.id}
                        </td>
                        <td className="px-5 py-3.5">
                          <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${priorityStyles[ticket.priority] || ""}`}>
                            {ticket.priority}
                          </span>
                        </td>
                        <td className="px-5 py-3.5 font-bold text-slate-800">
                          <div className="flex flex-col">
                            <span>{ticket.citizenName}</span>
                            <span className="text-[9px] text-slate-400 font-sans font-medium">
                              Ph: {ticket.citizenPhone}
                            </span>
                          </div>
                        </td>
                        <td className="px-5 py-3.5 text-slate-700">
                          {ticket.category}
                        </td>
                        <td className="px-5 py-3.5 hidden md:table-cell text-slate-500 font-medium truncate max-w-[150px]" title={ticket.relatedScheme}>
                          {ticket.relatedScheme}
                        </td>
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-1.5 font-mono text-slate-600 font-bold">
                            <Clock className={`h-3.5 w-3.5 ${ticket.sla > 0 ? "text-indigo-600" : "text-slate-400"}`} />
                            <span>{ticket.sla > 0 ? formatSla(ticket.sla) : "SLA Met"}</span>
                          </div>
                        </td>
                        <td className="px-5 py-3.5 text-slate-500 font-medium">
                          {ticket.assignedOfficer}
                        </td>
                        <td className="px-5 py-3.5">
                          <span className={`px-2 py-0.5 rounded-full border text-[9px] font-bold ${statusStyles[ticket.status] || ""}`}>
                            {ticket.status}
                          </span>
                        </td>
                        <td className="px-5 py-3.5 text-right" onClick={(e) => e.stopPropagation()}>
                          <button
                            onClick={() => setExpandedId(isExpanded ? null : ticket.id)}
                            className="p-1 hover:bg-slate-200 rounded-lg text-slate-500 transition"
                          >
                            {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                          </button>
                        </td>
                      </tr>

                      {/* Ticket expanded conversation and actions */}
                      {isExpanded && (
                        <tr>
                          <td colSpan="9" className="bg-slate-50/50 px-6 py-5 border-y border-slate-100 text-xs">
                            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                              
                              {/* Left column: Citizen profile, complaint details, attachments, timeline */}
                              <div className="lg:col-span-5 space-y-4">
                                <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
                                  <div className="flex items-center gap-3 mb-3">
                                    <div className="h-10 w-10 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-100 flex items-center justify-center font-black text-xs">
                                      {ticket.citizenName.split(" ").map(x => x[0]).join("")}
                                    </div>
                                    <div>
                                      <p className="font-bold text-slate-800 text-sm">{ticket.citizenName}</p>
                                      <p className="text-[10px] text-slate-500">Email: {ticket.citizenEmail} | Phone: {ticket.citizenPhone}</p>
                                    </div>
                                  </div>
                                </div>

                                <div className="space-y-1">
                                  <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block">Grievance Description</span>
                                  <div className="bg-white border border-slate-200 p-4 rounded-xl text-[11px] font-medium leading-relaxed text-slate-700 shadow-sm">
                                    {ticket.description}
                                  </div>
                                </div>

                                {ticket.supportingNote && (
                                  <div>
                                    <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block">Citizen Remarks</span>
                                    <p className="text-slate-500 mt-0.5 italic font-medium">"{ticket.supportingNote}"</p>
                                  </div>
                                )}

                                <div className="space-y-1.5">
                                  <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block">Attached files</span>
                                  <div className="flex gap-2">
                                    <button className="px-3 py-1.5 bg-white hover:bg-slate-100 border border-slate-200 rounded-lg text-[10px] font-bold text-slate-600 flex items-center gap-1 transition">
                                      <Paperclip className="h-3.5 w-3.5 text-slate-400" />
                                      <span>transaction_receipt.pdf</span>
                                    </button>
                                  </div>
                                </div>

                                {/* Timeline */}
                                <div className="space-y-2">
                                  <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block">Grievance Timeline</span>
                                  <div className="bg-white border border-slate-200 p-4 rounded-xl shadow-sm">
                                    <div className="space-y-3">
                                      {currentTimeline.map((entry, idx) => (
                                        <div key={idx} className="flex gap-3">
                                          <div className="flex flex-col items-center">
                                            <div className="w-2 h-2 rounded-full bg-indigo-600 mt-1" />
                                            {idx < currentTimeline.length - 1 && <div className="w-0.5 h-full bg-slate-200" />}
                                          </div>
                                          <div className="pb-3">
                                            <div className="flex justify-between items-start">
                                              <p className="text-xs font-bold text-slate-800">{entry.event}</p>
                                              <span className="text-[9px] text-slate-400">{entry.date}</span>
                                            </div>
                                            <p className="text-[10px] text-slate-500 mt-0.5">By: {entry.actor}</p>
                                          </div>
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                </div>
                              </div>

                              {/* Right column: Assign Officer, Discussion, Internal Notes, Actions */}
                              <div className="lg:col-span-7 space-y-4 flex flex-col">
                                {/* Assign Officer */}
                                <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm">
                                  <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block mb-2">Assign Officer</span>
                                  <div className="flex items-center gap-2">
                                    <Users className="h-4 w-4 text-slate-400" />
                                    <select
                                      value={ticket.assignedOfficer}
                                      onChange={(e) => handleAssignOfficer(ticket.id, e.target.value)}
                                      className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500"
                                    >
                                      {AVAILABLE_OFFICERS.map(officer => (
                                        <option key={officer} value={officer}>{officer}</option>
                                      ))}
                                    </select>
                                  </div>
                                </div>

                                {/* Discussion Feed */}
                                <div className="space-y-2 flex-1">
                                  <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block">Communication Thread</span>
                                  <div className="bg-white border border-slate-200 rounded-xl p-4 min-h-[120px] max-h-48 overflow-y-auto space-y-3 shadow-sm">
                                    {currentThread.length === 0 ? (
                                      <p className="text-center text-slate-400 py-8 text-[11px] font-semibold">No discussions on this ticket yet.</p>
                                    ) : (
                                      currentThread.map((msg, index) => (
                                        <div
                                          key={index}
                                          className={`flex flex-col max-w-[85%] ${
                                            msg.sender === "officer" ? "ml-auto items-end" : "items-start"
                                          }`}
                                        >
                                          <div
                                            className={`p-3 rounded-2xl text-[11px] font-medium leading-relaxed shadow-sm ${
                                              msg.sender === "officer"
                                                ? "bg-indigo-600 text-white rounded-tr-none"
                                                : "bg-slate-50 text-slate-700 border border-slate-200 rounded-tl-none"
                                            }`}
                                          >
                                            {msg.text}
                                          </div>
                                          <span className="text-[8px] text-slate-400 font-semibold mt-1">
                                            {msg.sender === "officer" ? "Officer" : "Citizen"} • {msg.date}
                                          </span>
                                        </div>
                                      ))
                                    )}
                                  </div>

                                  <div className="flex gap-2">
                                    <input
                                      type="text"
                                      placeholder="Write response message..."
                                      value={replyText}
                                      onChange={(e) => setReplyText(e.target.value)}
                                      className="flex-1 px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-indigo-500 font-medium"
                                    />
                                    <button
                                      onClick={() => handleReplySubmit(ticket.id)}
                                      className="px-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl transition shadow-sm flex items-center justify-center"
                                    >
                                      <Send className="h-4 w-4" />
                                    </button>
                                  </div>
                                </div>

                                {/* Internal Notes */}
                                <div className="space-y-2">
                                  <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block">Internal Notes</span>
                                  <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-2 max-h-32 overflow-y-auto shadow-sm">
                                    {currentInternalNotes.length === 0 ? (
                                      <p className="text-center text-amber-600/60 py-4 text-[11px] font-semibold">No internal notes yet.</p>
                                    ) : (
                                      currentInternalNotes.map((note, idx) => (
                                        <div key={idx} className="bg-white border border-amber-100 p-2 rounded-lg text-[11px]">
                                          <div className="flex justify-between items-center mb-1">
                                            <span className="font-bold text-amber-800">{note.author}</span>
                                            <span className="text-[9px] text-amber-600">{note.date}</span>
                                          </div>
                                          <p className="text-slate-700">{note.text}</p>
                                        </div>
                                      ))
                                    )}
                                  </div>
                                  <div className="flex gap-2">
                                    <input
                                      type="text"
                                      placeholder="Add internal note..."
                                      value={internalNoteText}
                                      onChange={(e) => setInternalNoteText(e.target.value)}
                                      className="flex-1 px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-amber-500 font-medium"
                                    />
                                    <button
                                      onClick={() => handleAddInternalNote(ticket.id)}
                                      className="px-3 bg-amber-600 hover:bg-amber-700 text-white rounded-xl transition shadow-sm flex items-center justify-center"
                                    >
                                      <FileText className="h-4 w-4" />
                                    </button>
                                  </div>
                                </div>

                                {/* Actions Bar */}
                                <div className="pt-2 border-t border-slate-100 flex gap-2">
                                  <button
                                    onClick={() => handleResolve(ticket.id)}
                                    disabled={ticket.status === "Resolved"}
                                    className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-100 disabled:text-slate-400 text-white rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 shadow-sm"
                                  >
                                    <CheckCircle className="h-4 w-4" />
                                    <span>Resolve Ticket</span>
                                  </button>
                                  <button
                                    onClick={() => handleEscalate(ticket.id)}
                                    disabled={ticket.status === "Resolved"}
                                    className="flex-1 py-2 bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 shadow-sm"
                                  >
                                    <AlertTriangle className="h-4 w-4 text-rose-500" />
                                    <span>Escalate Ticket</span>
                                  </button>
                                </div>
                              </div>

                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
