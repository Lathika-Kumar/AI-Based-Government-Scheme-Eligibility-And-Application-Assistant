import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Building2, Mail, ArrowRight, ArrowLeft, CheckCircle, AlertCircle } from "lucide-react";
import { isValidEmail } from "@utils/security";
import { forgotPassword } from "@services/authService";
import { useToast } from "@components/ui/ToastNotification";

export default function ForgotPassword() {
  const { showToast } = useToast();
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const targetEmail = email.trim().toLowerCase();
    if (!targetEmail) {
      setError("Please enter your email address.");
      return;
    }

    if (!isValidEmail(targetEmail)) {
      setError("Please enter a valid email address.");
      return;
    }

    setLoading(true);
    try {
      const res = await forgotPassword({ email: targetEmail });
      setSubmitted(true);
      setSuccessMessage(res.message || `Password reset instructions sent to ${targetEmail}`);
      showToast("Reset Link Sent", "Reset Link Sent", "Check your email inbox for further instructions.");
    } catch (err) {
      setError(err.message || "Failed to process password reset. Please try again.");
      showToast("Request Failed", "Request Failed", err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-government-blue via-government-blue-light to-government-blue flex items-center justify-center px-4 py-12 relative overflow-hidden">
      <div className="absolute top-1/4 left-1/4 w-80 h-80 bg-saffron/10 rounded-full blur-3xl"></div>
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-india-green/10 rounded-full blur-3xl"></div>

      <div className="w-full max-w-md relative z-10 space-y-6">
        <div className="h-2 bg-gradient-to-r from-saffron via-white-official to-india-green rounded-full"></div>

        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center bg-white p-3 rounded-xl shadow-lg">
            <Building2 className="h-8 w-8 text-government-blue" />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">SchemeBridge</h1>
          <p className="text-white/80 text-sm font-medium">National Public Welfare & Scheme Eligibility Platform</p>
        </div>

        <div className="bg-white rounded-xl shadow-2xl overflow-hidden border border-gray-200">
          <div className="bg-gray-50 px-8 py-6 text-center border-b border-gray-200">
            <h2 className="text-xl font-bold text-gray-900">Reset Password</h2>
            <p className="text-gray-600 text-sm mt-1 leading-relaxed">
              Enter your registered email to receive a password reset link
            </p>
          </div>

          <div className="px-8 py-7 space-y-6">
            {submitted ? (
              <div className="text-center space-y-4 py-4">
                <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100 text-green-600">
                  <CheckCircle className="h-6 w-6" />
                </div>
                <h3 className="text-base font-bold text-gray-900">Check Your Inbox</h3>
                <p className="text-xs text-gray-600 leading-relaxed bg-gray-50 p-4 rounded-xl border border-gray-200">
                  {successMessage}
                </p>
                <div className="pt-2">
                  <Link
                    to="/login"
                    className="inline-flex items-center justify-center gap-2 w-full py-3 bg-government-blue hover:bg-government-blue-dark text-white rounded-lg text-sm font-bold shadow-md transition"
                  >
                    <ArrowLeft className="h-4 w-4" /> Return to Login
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                  <div className="flex items-start gap-2.5 bg-red-50 border border-red-200 text-red-700 text-sm p-3.5 rounded-lg">
                    <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                    <span className="font-medium text-xs leading-normal">{error}</span>
                  </div>
                )}

                <div>
                  <label htmlFor="forgot-email" className="block text-sm font-medium text-gray-700 mb-1.5">
                    Registered Email Address
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                      id="forgot-email"
                      type="email"
                      placeholder="citizen@schemebridge.in"
                      value={email}
                      onChange={(e) => {
                        setEmail(e.target.value);
                        setError("");
                      }}
                      disabled={loading}
                      className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-government-blue focus:bg-white transition"
                      autoComplete="email"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-3 bg-government-blue hover:bg-government-blue-dark disabled:opacity-50 text-white rounded-lg font-bold text-sm shadow-md transition flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <span>Sending Reset Link...</span>
                  ) : (
                    <>
                      <span>Send Reset Link</span>
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>

                <div className="text-center pt-2">
                  <Link
                    to="/login"
                    className="inline-flex items-center text-xs font-semibold text-gray-500 hover:text-gray-800 transition gap-1"
                  >
                    <ArrowLeft className="h-3.5 w-3.5" /> Back to Login
                  </Link>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
