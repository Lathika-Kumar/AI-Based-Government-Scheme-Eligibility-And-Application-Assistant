/* src/admin/hooks/useDashboardData.js */
import { useState, useEffect, useCallback } from 'react';
import { getPriorityQueue, getAnalyticsData } from '../services/dashboardService';
import { useApp } from '../../context/AppContext';

/**
 * Custom hook to fetch all dashboard related data.
 * Returns loading, error and the data objects.
 */
export function useDashboardData(refreshTrigger) {
  const { applications, schemes, grievances, documents, auditLogs, notifications } = useApp();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [kpis, setKpis] = useState([]);
  const [priorityQueue, setPriorityQueue] = useState([]);
  const [analytics, setAnalytics] = useState({});
  const [recentActivities, setRecentActivities] = useState([]);
  const [notifList, setNotifList] = useState([]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const totalApps = applications?.length || 0;
      const pendingReviewsCount = applications?.filter(a => a.currentStage !== 'Approved' && a.currentStage !== 'Rejected')?.length || 0;
      const today = new Date().toISOString().split('T')[0];
      const approvedToday = applications?.filter(a => a.currentStage === 'Approved' && a.appliedDate === today)?.length || 0;
      const rejectedToday = applications?.filter(a => a.currentStage === 'Rejected' && a.appliedDate === today)?.length || 0;
      const activeSchemesCount = schemes?.filter(s => s.status === 'published')?.length || 0;
      const pendingDocumentsCount = documents?.filter(d => d.status !== 'verified' && d.status !== 'rejected')?.length || 0;
      const registeredCitizens = '14,802'; // static mock placeholder
      const averageProcessingTime = '4.2 Days'; // static mock placeholder

      const kpiList = [
        { title: 'Total Applications', value: totalApps, change: '+12.4%', isPositive: true, subtext: 'vs last month', icon: null, color: 'text-indigo-600 bg-indigo-50 border-indigo-100', sparklineData: [] },
        { title: 'Pending Review', value: pendingReviewsCount, change: '-5.1%', isPositive: true, subtext: 'vs last week', icon: null, color: 'text-amber-600 bg-amber-50 border-amber-100', sparklineData: [] },
        { title: 'Approved Today', value: approvedToday, change: '+15.0%', isPositive: true, subtext: 'vs daily average', icon: null, color: 'text-emerald-600 bg-emerald-50 border-emerald-100', sparklineData: [] },
        { title: 'Rejected Today', value: rejectedToday, change: '-20.0%', isPositive: false, subtext: 'vs daily average', icon: null, color: 'text-rose-600 bg-rose-50 border-rose-100', sparklineData: [] },
        { title: 'Pending Documents', value: pendingDocumentsCount, change: '-12.5%', isPositive: true, subtext: 'vs yesterday', icon: null, color: 'text-orange-600 bg-orange-50 border-orange-100', sparklineData: [] },
        { title: 'Active Schemes', value: activeSchemesCount, change: '+2', isPositive: true, subtext: 'this quarter', icon: null, color: 'text-sky-600 bg-sky-50 border-sky-100', sparklineData: [] },
        { title: 'Registered Citizens', value: registeredCitizens, change: '+18.2%', isPositive: true, subtext: 'vs last month', icon: null, color: 'text-violet-600 bg-violet-50 border-violet-100', sparklineData: [] },
        { title: 'Avg Processing Time', value: averageProcessingTime, change: '-1.5 Days', isPositive: true, subtext: 'SLA target: 7 Days', icon: null, color: 'text-teal-600 bg-teal-50 border-teal-100', sparklineData: [] },
      ];

      setKpis(kpiList);
      setPriorityQueue(getPriorityQueue(applications, grievances));
      setAnalytics(getAnalyticsData(applications, schemes));
      setRecentActivities(auditLogs?.slice(0, 5) || []);
      setNotifList(notifications?.slice(0, 5) || []);
    } catch (e) {
      console.error('Dashboard data fetch error', e);
      setError(e);
    } finally {
      setLoading(false);
    }
  }, [applications, schemes, grievances, documents, auditLogs, notifications]);

  useEffect(() => {
    fetchData();
  }, [fetchData, refreshTrigger]);

  return { loading, error, kpis, priorityQueue, analytics, recentActivities, notifList };
}
