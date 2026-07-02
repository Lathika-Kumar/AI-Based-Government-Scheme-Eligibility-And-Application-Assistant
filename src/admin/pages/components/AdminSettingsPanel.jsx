import React, { useState } from "react";
import {
  Sliders,
  Shield,
  Save,
  Database,
  Cpu,
  RefreshCw,
  Server
} from "lucide-react";

export default function AdminSettingsPanel() {
  const [slaTarget, setSlaTarget] = useState(7);
  const [ocrConfidence, setOcrConfidence] = useState(85);
  const [enableAutoVerify, setEnableAutoVerify] = useState(true);

  const handleSave = (e) => {
    e.preventDefault();
    alert("Administrative settings saved to local system storage.");
  };

  return (
    <div className="max-w-2xl bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden animate-in fade-in duration-200">
      <div className="p-5 border-b border-slate-100 flex items-center gap-2">
        <Sliders className="h-4.5 w-4.5 text-indigo-650 text-indigo-600" />
        <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Console Configurations</h3>
      </div>

      <form onSubmit={handleSave} className="p-6 space-y-6 text-xs font-semibold text-slate-655 text-slate-700">
        
        {/* SLA boundaries */}
        <div className="space-y-3">
          <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
            <Server className="h-4 w-4 text-slate-455 text-slate-400" />
            <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">SLA & Workloads</h4>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-500 mb-1">Target Application SLA (Days)</label>
              <input
                type="number"
                value={slaTarget}
                onChange={(e) => setSlaTarget(Number(e.target.value))}
                className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-slate-500 mb-1">Max Queue Size per Nodal Officer</label>
              <input
                type="number"
                defaultValue={45}
                className="w-full px-3 py-2 border border-slate-200 bg-slate-50 rounded-xl focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
          </div>
        </div>

        {/* OCR automation */}
        <div className="space-y-3">
          <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
            <Cpu className="h-4 w-4 text-slate-400" />
            <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">OCR & AI Settings</h4>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
              <div>
                <span className="text-slate-800 font-bold block">Auto-Verify High Confidence Docs</span>
                <span className="text-[10px] text-slate-400 font-medium">Bypass manual review if OCR validation exceeds threshold.</span>
              </div>
              <button
                type="button"
                onClick={() => setEnableAutoVerify(!enableAutoVerify)}
                className={`w-12 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${
                  enableAutoVerify ? "bg-indigo-650 bg-indigo-600" : "bg-slate-300"
                }`}
              >
                <div
                  className={`w-4 h-4 rounded-full bg-white transition-transform duration-200 ${
                    enableAutoVerify ? "transform translate-x-6" : ""
                  }`}
                />
              </button>
            </div>
            <div>
              <label className="block text-slate-500 mb-1">Confidence Threshold (%)</label>
              <div className="flex items-center gap-3">
                <input
                  type="range"
                  min="50"
                  max="99"
                  value={ocrConfidence}
                  onChange={(e) => setOcrConfidence(Number(e.target.value))}
                  className="flex-1 accent-indigo-600 h-1 bg-slate-200 rounded-lg cursor-pointer"
                />
                <span className="font-bold text-slate-800 text-xs w-8">{ocrConfidence}%</span>
              </div>
            </div>
          </div>
        </div>

        {/* Backup Database */}
        <div className="space-y-3">
          <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
            <Database className="h-4 w-4 text-slate-400" />
            <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Database & Cache</h4>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => alert("Simulating cache optimization...")}
              className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-655 text-slate-600 rounded-xl font-bold transition flex items-center gap-1.5"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Optimize Indices</span>
            </button>
            <button
              type="button"
              onClick={() => alert("Simulating local storage database backup...")}
              className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl font-bold transition flex items-center gap-1.5"
            >
              <Shield className="h-3.5 w-3.5 text-indigo-650 text-indigo-600" />
              <span>Backup DB Configuration</span>
            </button>
          </div>
        </div>

        {/* Save button */}
        <div className="pt-4 border-t border-slate-100 flex justify-end">
          <button
            type="submit"
            className="px-4 py-2.5 bg-indigo-650 bg-indigo-600 hover:bg-indigo-705 hover:bg-indigo-700 text-white rounded-xl font-bold shadow-sm transition flex items-center gap-1.5"
          >
            <Save className="h-4.5 w-4.5" />
            <span>Save Configurations</span>
          </button>
        </div>
      </form>
    </div>
  );
}
