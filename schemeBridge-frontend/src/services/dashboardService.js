/**
 * User dashboard service — fetches aggregated dashboard summary from backend
 */
import apiClient from "@utils/apiClient";
import { ENDPOINTS } from "@config/api";

const dashboardService = {
  async getSummary() {
    return apiClient.get(ENDPOINTS.DASHBOARD.SUMMARY);
  },
};

export default dashboardService;
