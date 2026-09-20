/**
 * User dashboard service — fetches aggregated dashboard summary from Scheme Service backend
 */
import { schemeApi } from "@utils/apiClient";
import { DASHBOARD_ENDPOINTS } from "@config/api";

const dashboardService = {
  async getSummary() {
    try {
      const res = await schemeApi.get(DASHBOARD_ENDPOINTS.SUMMARY);
      if (res && !res.error && res.data) {
        return res.data;
      }
      if (res && !res.error && typeof res === "object" && res.eligibleSchemesCount !== undefined) {
        return res;
      }
      return null;
    } catch (err) {
      console.warn("dashboardService.getSummary error:", err);
      return null;
    }
  },
};

export default dashboardService;
