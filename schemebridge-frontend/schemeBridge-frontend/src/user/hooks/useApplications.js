/**
 * @file useApplications.js
 * @description React hook for citizen application data from the real backend.
 * Provides loading, error, and data states with refresh capability.
 */
import { useState, useEffect, useCallback } from "react";
import applicationService from "@services/applicationService";

/**
 * Hook to fetch the authenticated citizen's applications.
 * @returns {{ applications, loading, error, refetch }}
 */
export function useMyApplications() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading]           = useState(true);
  const [error, setError]               = useState(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    const result = await applicationService.getMyApplications();
    if (result.error) {
      setError(result.message || "Unable to load applications.");
    } else {
      setApplications(Array.isArray(result.data) ? result.data : []);
    }
    setLoading(false);
  }, []);

  useEffect(() => { fetch(); }, [fetch]);

  return { applications, loading, error, refetch: fetch };
}

/**
 * Hook to fetch a single application's details plus its timeline.
 * @param {string} applicationId
 */
export function useApplicationDetails(applicationId) {
  const [application, setApplication] = useState(null);
  const [timeline, setTimeline]       = useState(null);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState(null);

  const fetch = useCallback(async () => {
    if (!applicationId) { setLoading(false); return; }
    setLoading(true);
    setError(null);

    const [appResult, timelineResult] = await Promise.all([
      applicationService.getApplicationDetails(applicationId),
      applicationService.getApplicationTimeline(applicationId),
    ]);

    if (appResult.error) {
      setError(appResult.message || "Unable to load application.");
    } else {
      setApplication(appResult.data);
    }
    if (!timelineResult.error) {
      setTimeline(timelineResult.data);
    }
    setLoading(false);
  }, [applicationId]);

  useEffect(() => { fetch(); }, [fetch]);

  return { application, timeline, loading, error, refetch: fetch };
}
