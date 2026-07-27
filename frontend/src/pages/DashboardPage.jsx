import { useEffect, useState } from 'react';
import { Paper, Typography, Box, Button, Stack } from '@mui/material';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import DescriptionIcon from '@mui/icons-material/Description';
import BoltIcon from '@mui/icons-material/Bolt';
import SpeedIcon from '@mui/icons-material/Speed';
import HistoryIcon from '@mui/icons-material/History';
import * as analyticsApi from '../api/analyticsApi';
import * as historyApi from '../api/historyApi';
import Loader from '../components/Loader';
import PageHeader from '../components/common/PageHeader';
import StatCard from '../components/common/StatCard';
import EmptyState from '../components/common/EmptyState';
import AnalyticsBarChart from '../components/charts/AnalyticsBarChart';
import { REQUEST_TYPE_LABELS } from '../utils/requestTypes';
import { useAuth } from '../context/AuthContext';
import { useReducedMotionSafe } from '../theme/motion';

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [analytics, setAnalytics] = useState(null);
  const [recent, setRecent] = useState([]);
  const [loading, setLoading] = useState(true);
  const { staggerContainer, fadeInUp } = useReducedMotionSafe();

  useEffect(() => {
    Promise.all([analyticsApi.getAnalytics(), historyApi.getHistory(0, 5)])
      .then(([analyticsData, historyData]) => {
        setAnalytics(analyticsData);
        setRecent(historyData.content);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <Loader fullHeight />;
  }

  const topBreakdown = [...(analytics?.breakdown || [])]
    .sort((a, b) => b.totalRequests - a.totalRequests)
    .slice(0, 5)
    .map((item) => ({ label: REQUEST_TYPE_LABELS[item.requestType] || item.requestType, value: item.totalRequests }));

  return (
    <Box>
      <PageHeader
        title={`Welcome back, ${user?.fullName?.split(' ')[0] || ''}`}
        subtitle="Here's what's happening with your AI email assistant."
        action={
          <Stack direction="row" spacing={2}>
            <Button variant="contained" onClick={() => navigate('/compose')}>
              New AI Draft
            </Button>
            <Button variant="outlined" onClick={() => navigate('/history')}>
              View History
            </Button>
          </Stack>
        }
      />

      <Box
        component={motion.div}
        initial="initial"
        animate="animate"
        variants={staggerContainer}
        sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2, mb: 4 }}
      >
        <motion.div variants={fadeInUp}>
          <StatCard label="Total Requests (30d)" value={analytics?.totalRequests ?? 0} icon={<DescriptionIcon fontSize="small" />} />
        </motion.div>
        <motion.div variants={fadeInUp}>
          <StatCard label="Tokens Used (30d)" value={analytics?.totalTokens ?? 0} icon={<BoltIcon fontSize="small" />} accent="secondary" />
        </motion.div>
        <motion.div variants={fadeInUp}>
          <StatCard label="Avg Latency" value={`${Math.round(analytics?.avgLatencyMs ?? 0)} ms`} icon={<SpeedIcon fontSize="small" />} />
        </motion.div>
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
        <Box>
          <Typography variant="h6" gutterBottom>
            Recent Activity
          </Typography>
          {recent.length === 0 ? (
            <EmptyState
              icon={<HistoryIcon />}
              title="No AI requests yet"
              description="Try the Compose Assistant to generate your first AI-powered reply."
              action={
                <Button variant="contained" size="small" onClick={() => navigate('/compose')}>
                  Open Compose Assistant
                </Button>
              }
            />
          ) : (
            <Paper
              variant="outlined"
              component={motion.div}
              initial="initial"
              animate="animate"
              variants={staggerContainer}
            >
              {recent.map((entry, index) => (
                <Box
                  key={entry.id}
                  component={motion.div}
                  variants={fadeInUp}
                  sx={{ p: 2, borderBottom: index === recent.length - 1 ? 0 : 1, borderColor: 'divider' }}
                >
                  <Typography variant="subtitle2">{REQUEST_TYPE_LABELS[entry.requestType] || entry.requestType}</Typography>
                  <Typography variant="body2" color="text.secondary" noWrap>
                    {entry.originalContent}
                  </Typography>
                </Box>
              ))}
            </Paper>
          )}
        </Box>

        {topBreakdown.length > 0 && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Top Request Types
            </Typography>
            <Paper variant="outlined" sx={{ p: 2 }}>
              <AnalyticsBarChart items={topBreakdown} height={Math.max(180, topBreakdown.length * 48)} />
            </Paper>
          </Box>
        )}
      </Box>
    </Box>
  );
}
