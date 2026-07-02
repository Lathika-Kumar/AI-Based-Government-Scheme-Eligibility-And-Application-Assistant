import React, { useState, useEffect } from "react";
import { User, Building, Calendar, Clock, Shield } from "lucide-react";

export default function WelcomeCard({ user }) {
  const [greeting, setGreeting] = useState("");
  const [currentDate, setCurrentDate] = useState("");
  const [currentTime, setCurrentTime] = useState("");

  useEffect(() => {
    const updateGreeting = () => {
      const hour = new Date().getHours();
      if (hour < 12) setGreeting("Good Morning");
      else if (hour < 17) setGreeting("Good Afternoon");
      else setGreeting("Good Evening");
    };

    const updateDateTime = () => {
      const now = new Date();
      setCurrentDate(
        now.toLocaleDateString("en-IN", {
          weekday: "long",
          year: "numeric",
          month: "long",
          day: "numeric"
        })
      );
      setCurrentTime(
        now.toLocaleTimeString("en-IN", {
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
          hour12: true
        })
      );
    };

    updateGreeting();
    updateDateTime();
    const interval = setInterval(updateDateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  const adminName = user?.name || "Sanjay Kumar";
  const adminRole = user?.role || "Super Admin";
  const adminDepartment = user?.department || "Govt. Scheme Evaluation Board";

  return (
    <div className="bg-gradient-to-br from-indigo-600 via-indigo-700 to-indigo-800 rounded-2xl p-6 text-white shadow-lg border border-indigo-500/30 relative overflow-hidden">
      {/* Decorative elements */}
      <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full blur-3xl -mr-16 -mt-16 pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-48 h-48 bg-emerald-500/10 rounded-full blur-3xl -ml-12 -mb-12 pointer-events-none" />

      <div className="relative z-10">
        {/* Government of India branding */}
        <div className="flex items-center gap-2 mb-4">
          <div className="flex items-center gap-2 bg-white/10 backdrop-blur-sm px-3 py-1.5 rounded-xl border border-white/20">
            <span className="text-lg">🇮🇳</span>
            <span className="text-[10px] font-black tracking-wider uppercase text-white/90">
              Government of India
            </span>
          </div>
        </div>

        {/* Greeting */}
        <h1 className="text-2xl font-black tracking-tight mb-1">
          {greeting}, {adminName}
        </h1>

        {/* Admin details grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-6">
          {/* Role */}
          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-3 border border-white/10">
            <div className="flex items-center gap-2 mb-1">
              <Shield className="h-4 w-4 text-indigo-200" />
              <span className="text-[9px] font-black uppercase tracking-wider text-indigo-200">
                Role
              </span>
            </div>
            <p className="text-sm font-bold text-white">{adminRole}</p>
          </div>

          {/* Department */}
          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-3 border border-white/10">
            <div className="flex items-center gap-2 mb-1">
              <Building className="h-4 w-4 text-indigo-200" />
              <span className="text-[9px] font-black uppercase tracking-wider text-indigo-200">
                Department
              </span>
            </div>
            <p className="text-sm font-bold text-white truncate">{adminDepartment}</p>
          </div>

          {/* Date */}
          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-3 border border-white/10">
            <div className="flex items-center gap-2 mb-1">
              <Calendar className="h-4 w-4 text-indigo-200" />
              <span className="text-[9px] font-black uppercase tracking-wider text-indigo-200">
                Date
              </span>
            </div>
            <p className="text-sm font-bold text-white">{currentDate}</p>
          </div>

          {/* Time */}
          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-3 border border-white/10">
            <div className="flex items-center gap-2 mb-1">
              <Clock className="h-4 w-4 text-indigo-200" />
              <span className="text-[9px] font-black uppercase tracking-wider text-indigo-200">
                Time
              </span>
            </div>
            <p className="text-sm font-bold text-white font-mono">{currentTime}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
