import { useEffect, useState } from 'react';
import { Paper, Typography, Box, Button, Stack } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import * as analyticsApi from '../api/analyticsApi';
import * as historyApi from '../api/historyApi';
import Loader from '../components/Loader';
import { REQUEST_TYPE_LABELS } from '../utils/requestTypes';
import { useAuth } from '../context/AuthContext';

function StatCard({ label, value }) {
  return (
    <Paper variant="outlined" sx={{ p: 3 }}>
      <Typography variant="overline" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h4" fontWeight={700}>
        {value}
      </Typography>
    </Paper>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [analytics, setAnalytics] = useState(null);
  const [recent, setRecent] = useState([]);
  const [loading, setLoading] = useState(true);

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

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Welcome back, {user?.fullName?.split(' ')[0]}
      </Typography>
      <Stack direction="row" spacing={2} sx={{ mb: 4 }}>
        <Button variant="contained" onClick={() => navigate('/compose')}>
          New AI Draft
        </Button>
        <Button variant="outlined" onClick={() => navigate('/history')}>
          View History
        </Button>
      </Stack>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2, mb: 4 }}>
        <StatCard label="Total Requests (30d)" value={analytics?.totalRequests ?? 0} />
        <StatCard label="Tokens Used (30d)" value={analytics?.totalTokens ?? 0} />
        <StatCard label="Avg Latency" value={`${Math.round(analytics?.avgLatencyMs ?? 0)} ms`} />
      </Box>

      <Typography variant="h6" gutterBottom>
        Recent Activity
      </Typography>
      <Paper variant="outlined">
        {recent.length === 0 && (
          <Box sx={{ p: 3 }}>
            <Typography color="text.secondary">No AI requests yet — try the Compose Assistant.</Typography>
          </Box>
        )}
        {recent.map((entry) => (
          <Box key={entry.id} sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
            <Typography variant="subtitle2">{REQUEST_TYPE_LABELS[entry.requestType] || entry.requestType}</Typography>
            <Typography variant="body2" color="text.secondary" noWrap>
              {entry.originalContent}
            </Typography>
          </Box>
        ))}
      </Paper>
    </Box>
  );
}
