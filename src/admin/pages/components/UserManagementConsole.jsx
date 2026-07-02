import React, { useState, useMemo } from "react";
import {
  Users,
  Shield,
  UserCheck,
  CheckCircle,
  XCircle,
  Search,
  BookOpen,
  ClipboardList,
  AlertCircle
} from "lucide-react";

export default function UserManagementConsole() {
  const [activeTab, setActiveTab] = useState("citizens");
  const [search, setSearch] = useState("");

  const mockUsers = useMemo(() => {
    return {
      citizens: [
        { name: "Rajesh Patel", email: "citizen@demo.com", state: "Gujarat", occupation: "Farmer", status: "Active", lastLogin: "2 hours ago", workload: "3 applications submitted" },
        { name: "Aravind Swamy", email: "aravind@demo.com", state: "Tamil Nadu", occupation: "Self-Employed", status: "Active", lastLogin: "1 day ago", workload: "1 application in review" },
        { name: "Sunita Sharma", email: "sunita@demo.com", state: "Uttar Pradesh", occupation: "Student", status: "Active", lastLogin: "10 mins ago", workload: "2 applications approved" },
        { name: "Vikram Singh", email: "vikram@demo.com", state: "Maharashtra", occupation: "Unemployed", status: "Suspended", lastLogin: "3 days ago", workload: "No active applications" }
      ],
      officers: [
        { name: "Amit Singh (Verification)", email: "verify@schemebridge.gov.in", dept: "Document Verification Directorate", status: "Active", lastLogin: "5 mins ago", workload: "12 applications in queue" },
        { name: "Karan Johar", email: "karan@schemebridge.gov.in", dept: "Aadhaar Audit Division", status: "Active", lastLogin: "4 hours ago", workload: "8 applications in queue" }
      ],
      managers: [
        { name: "Neha Sharma (Schemes)", email: "schemes@schemebridge.gov.in", dept: "Ministry of Social Welfare", status: "Active", lastLogin: "12 mins ago", workload: "Managing 5 active schemes" },
        { name: "Suresh Prabhu", email: "suresh@schemebridge.gov.in", dept: "Ministry of Agriculture", status: "Active", lastLogin: "2 days ago", workload: "Managing 2 active schemes" }
      ],
      support: [
        { name: "Priya Patel", email: "priya@schemebridge.gov.in", dept: "Public Relations & Grievances", status: "Active", lastLogin: "1 hour ago", workload: "8 tickets assigned" },
        { name: "Ravi Shankar", email: "ravi@schemebridge.gov.in", dept: "Call Support Desk", status: "Active", lastLogin: "30 mins ago", workload: "14 tickets assigned" }
      ],
      admins: [
        { name: "Sanjay Kumar (Admin)", email: "admin@schemebridge.gov.in", dept: "Govt. Scheme Evaluation Board", status: "Active", lastLogin: "Just now", workload: "Super privileges" }
      ]
    };
  }, []);

  const tabs = [
    { id: "citizens", label: "Citizens" },
    { id: "officers", label: "Verification Officers" },
    { id: "managers", label: "Scheme Managers" },
    { id: "support", label: "Support Officers" },
    { id: "admins", label: "Super Admins" }
  ];

  const currentList = useMemo(() => {
    const list = mockUsers[activeTab] || [];
    if (!search) return list;
    return list.filter(
      (u) =>
        u.name.toLowerCase().includes(search.toLowerCase()) ||
        u.email.toLowerCase().includes(search.toLowerCase())
    );
  }, [mockUsers, activeTab, search]);

  const handleToggleStatus = (user) => {
    alert(`Status toggled for ${user.name}`);
  };

  return (
    <div className="space-y-4">
      {/* Search and Tabs Header */}
      <div className="bg-white border border-slate-200 p-4 rounded-2xl shadow-sm space-y-3">
        <div className="relative">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search users by name or email ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-50 hover:bg-slate-100/50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-700 transition"
          />
        </div>

        {/* Tab Controls */}
        <div className="flex border-b border-slate-100 overflow-x-auto select-none pt-1">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => {
                setActiveTab(tab.id);
                setSearch("");
              }}
              className={`pb-2.5 px-4 text-xs font-bold border-b-2 transition whitespace-nowrap focus:outline-none ${
                activeTab === tab.id
                  ? "border-indigo-650 border-indigo-600 text-indigo-700"
                  : "border-transparent text-slate-400 hover:text-slate-600"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Grid List */}
      {currentList.length === 0 ? (
        <div className="bg-white border border-slate-200 p-12 text-center rounded-2xl flex flex-col items-center justify-center space-y-3">
          <Users className="h-10 w-10 text-slate-450 text-slate-450 text-slate-400" />
          <div>
            <p className="text-sm font-bold text-slate-800">No Users Listed</p>
            <p className="text-xs text-slate-400 leading-normal max-w-sm mt-0.5">
              No registered user records match your search query for this role.
            </p>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {currentList.map((user, idx) => (
            <div
              key={idx}
              className="bg-white border border-slate-200 p-5 rounded-2xl shadow-sm hover:shadow-md transition duration-200 flex flex-col justify-between"
            >
              <div className="space-y-4">
                <div className="flex gap-3.5 items-start">
                  <div className="h-10 w-10 rounded-full bg-indigo-50 text-indigo-700 hover:bg-indigo-100 border border-indigo-100 flex items-center justify-center font-black text-xs shrink-0 select-none">
                    {user.name.split(" ").map((x) => x[0]).join("")}
                  </div>
                  <div className="space-y-0.5 min-w-0">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <h4 className="font-bold text-slate-800 text-xs truncate max-w-[130px]" title={user.name}>{user.name}</h4>
                      <span className={`px-1.5 py-0.5 rounded text-[8px] font-bold border ${
                        user.status === "Active"
                          ? "bg-emerald-50 text-emerald-700 border-emerald-100"
                          : "bg-rose-50 text-rose-700 border-rose-100"
                      }`}>
                        {user.status}
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-400 font-semibold block truncate" title={user.email}>{user.email}</span>
                  </div>
                </div>

                <div className="space-y-2 text-xs font-semibold text-slate-500 bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div className="flex justify-between">
                    <span className="text-slate-400 text-[9px] uppercase">Department / State:</span>
                    <span className="text-slate-700 text-right truncate max-w-[160px]" title={user.dept || user.state}>{user.dept || user.state}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400 text-[9px] uppercase">Assigned Workload:</span>
                    <span className="text-slate-700 text-right">{user.workload}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400 text-[9px] uppercase">Telemetry Last Login:</span>
                    <span className="text-slate-700 text-right">{user.lastLogin}</span>
                  </div>
                </div>
              </div>

              {/* Actions Footer */}
              <div className="border-t border-slate-100 pt-3.5 mt-4 flex gap-2">
                <button
                  onClick={() => handleToggleStatus(user)}
                  className={`flex-1 py-1.5 rounded-lg text-[10px] font-bold border transition ${
                    user.status === "Active"
                      ? "bg-white text-rose-600 border-rose-200 hover:bg-rose-50/50"
                      : "bg-emerald-600 hover:bg-emerald-700 text-white border-emerald-600"
                  }`}
                >
                  {user.status === "Active" ? "Suspend Account" : "Activate Account"}
                </button>
                <button
                  onClick={() => alert(`Modifying administrative permissions for ${user.name}`)}
                  className="px-3 py-1.5 border border-slate-200 text-slate-655 text-slate-600 hover:bg-slate-50 rounded-lg text-[10px] font-bold transition whitespace-nowrap"
                >
                  Permissions
                </button>
              </div>

            </div>
          ))}
        </div>
      )}
    </div>
  );
}
