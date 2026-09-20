import { describe, it, expect, vi, beforeEach } from 'vitest';
import dashboardService from './dashboardService';
import { schemeApi } from '@utils/apiClient';
import { DASHBOARD_ENDPOINTS } from '@config/api';

vi.mock('@utils/apiClient', () => ({
  schemeApi: {
    get: vi.fn(),
  },
}));

describe('dashboardService — Aggregated Citizen Dashboard & Action Brief', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('successfully fetches and returns dashboard summary data', async () => {
    const mockData = {
      userId: '65',
      eligibleSchemesCount: 4,
      completedApplicationsCount: 1,
      pendingApplicationsCount: 2,
      documentCompletionPercentage: 100,
      profileCompletionPercentage: 100,
      actionItems: ['You qualify for 4 active schemes.'],
      aiSummary: 'You qualify for 4 welfare schemes.',
    };

    schemeApi.get.mockResolvedValueOnce({ data: mockData, error: false });

    const result = await dashboardService.getSummary();
    expect(schemeApi.get).toHaveBeenCalledWith(DASHBOARD_ENDPOINTS.SUMMARY);
    expect(result).toEqual(mockData);
  });

  it('handles backend response without nested data property', async () => {
    const directData = {
      userId: '64',
      eligibleSchemesCount: 2,
      completedApplicationsCount: 0,
      pendingApplicationsCount: 0,
      documentCompletionPercentage: 33,
      profileCompletionPercentage: 0,
      actionItems: [],
      aiSummary: 'You qualify for 2 welfare schemes.',
    };

    schemeApi.get.mockResolvedValueOnce(directData);

    const result = await dashboardService.getSummary();
    expect(result).toEqual(directData);
  });

  it('returns null when backend responds with error', async () => {
    schemeApi.get.mockResolvedValueOnce({ error: true, message: 'Internal Server Error' });

    const result = await dashboardService.getSummary();
    expect(result).toBeNull();
  });

  it('returns null and does not throw when network call fails', async () => {
    schemeApi.get.mockRejectedValueOnce(new Error('Network offline'));

    const result = await dashboardService.getSummary();
    expect(result).toBeNull();
  });
});
