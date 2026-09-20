import React, { useState, useEffect } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Building2 } from "lucide-react";
import ErrorBoundary from "@components/ErrorBoundary";

function OfficialBanner() {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <div className="bg-slate-900 text-slate-400 border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 py-2 flex flex-col sm:flex-row sm:items-center justify-between text-[11px] font-medium tracking-wide gap-2">
        <div className="flex items-center space-x-2">
          <span>🇮🇳</span>
          <span className="uppercase text-slate-400 font-bold tracking-wider text-[9px] bg-slate-800 px-1.5 py-0.5 rounded mr-1">Official Website Simulation</span>
          <span>This is a simulated government portal for demonstration purposes only.</span>
        </div>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="text-indigo-400 hover:text-indigo-300 underline font-semibold flex items-center space-x-1 focus:outline-none focus:ring-1 focus:ring-indigo-400 px-1 rounded transition self-start sm:self-auto"
          aria-expanded={isOpen}
        >
          {isOpen ? "Hide Details" : "Verify Authenticity"}
        </button>
      </div>
      {isOpen && (
        <div className="bg-slate-950 border-t border-slate-800 px-4 py-3 text-[11px] text-slate-400 leading-relaxed">
          <div className="max-w-7xl mx-auto grid sm:grid-cols-2 gap-4">
            <div>
              <p className="font-bold text-slate-200">🔒 schemebridge.gov.in</p>
              <p className="mt-1">Official government domain for scheme information and services.</p>
            </div>
            <div>
              <p className="font-bold text-slate-200">⚙️ Sandbox Environment Notice</p>
              <p className="mt-1">All data entered here is stored locally in your browser and is not transmitted to any government servers.</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function PublicLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  const handleScrollToSection = (e, id) => {
    e.preventDefault();
    if (location.pathname === "/") {
      const element = document.getElementById(id);
      if (element) {
        element.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    } else {
      navigate(`/#${id}`);
    }
  };

  useEffect(() => {
    // Handle hash on initial load or navigation
    if (location.hash) {
      const id = location.hash.slice(1);
      setTimeout(() => {
        const element = document.getElementById(id);
        if (element) {
          element.scrollIntoView({ behavior: "smooth", block: "start" });
        }
      }, 150);
    }
  }, [location.pathname, location.hash]);

  return (
    <div className="flex flex-col min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors">
      {/* Accessible skip link */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:p-2 focus:bg-indigo-600 focus:text-white focus:font-bold focus:rounded-md"
      >
        Skip to main content
      </a>

      {/* Top Banner indicating Official Website Simulation */}
      <OfficialBanner />

      {/* Main Header */}
      <header className="sticky top-0 z-50 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 shadow-sm transition-colors">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center space-x-3 group">
            <div className="bg-indigo-600 text-white p-2 rounded-lg group-hover:bg-indigo-700 transition">
              <Building2 className="h-6 w-6" />
            </div>
            <div>
              <span className="font-bold text-lg text-slate-900 dark:text-white tracking-tight block">SchemeBridge</span>
              <span className="text-[10px] text-slate-500 dark:text-slate-400 uppercase tracking-widest block font-semibold -mt-1">Digital India Initiative</span>
            </div>
          </Link>

          <nav className="flex space-x-4 sm:space-x-8 text-xs sm:text-sm font-medium text-slate-600 dark:text-slate-300 items-center">
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                `hover:text-indigo-600 dark:hover:text-indigo-400 transition ${isActive ? "text-indigo-600 dark:text-indigo-400 font-bold" : ""}`
              }
            >
              Home
            </NavLink>
            <a
              href="#how-it-works"
              onClick={(e) => handleScrollToSection(e, "how-it-works")}
              className="hover:text-indigo-600 dark:hover:text-indigo-400 transition cursor-pointer"
            >
              How It Works
            </a>
            <a
              href="#benefits"
              onClick={(e) => handleScrollToSection(e, "benefits")}
              className="hover:text-indigo-600 dark:hover:text-indigo-400 transition cursor-pointer"
            >
              Benefits
            </a>
            <NavLink
              to="/help-support"
              className={({ isActive }) =>
                `hover:text-indigo-600 dark:hover:text-indigo-400 transition ${isActive ? "text-indigo-600 dark:text-indigo-400 font-bold" : ""}`
              }
            >
              Help
            </NavLink>
          </nav>
        </div>
      </header>

      {/* Main Content Area */}
      <main id="main-content" tabIndex="-1" className="flex-grow focus:outline-none">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>

      {/* Official styled Footer */}
      <footer className="bg-slate-900 text-slate-400 border-t border-slate-800 text-xs py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center space-x-2">
            <Building2 className="h-5 w-5 text-indigo-400" />
            <span className="font-semibold text-white">SchemeBridge Gateway</span>
          </div>
          <div className="flex flex-wrap justify-center gap-6">
            <Link to="/about" className="hover:text-white transition">About</Link>
            <Link to="/help-support" className="hover:text-white transition">Help</Link>
            <Link to="/terms" className="hover:text-white transition">Terms & Privacy</Link>
            <Link to="/disclaimer" className="hover:text-white transition">Disclaimer</Link>
          </div>
          <p className="text-center md:text-right text-slate-500">
            © 2026 SchemeBridge. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
