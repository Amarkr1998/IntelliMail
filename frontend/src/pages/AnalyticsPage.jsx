import { useEffect, useState } from 'react';
import { Box, Typography, Paper, LinearProgress, Stack } from '@mui/material';
import * as analyticsApi from '../api/analyticsApi';
import Loader from '../components/Loader';
import { REQUEST_TYPE_LABELS } from '../utils/requestTypes';

export default function AnalyticsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    analyticsApi
      .getAnalytics()
      .then(setData)
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <Loader fullHeight />;
  }

  const maxRequests = Math.max(1, ...(data?.breakdown.map((b) => b.totalRequests) || [1]));

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Usage Analytics
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {new Date(data.from).toLocaleDateString()} – {new Date(data.to).toLocaleDateString()}
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2, mb: 4 }}>
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography variant="overline" color="text.secondary">
            Total Requests
          </Typography>
          <Typography variant="h4" fontWeight={700}>
            {data.totalRequests}
          </Typography>
        </Paper>
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography variant="overline" color="text.secondary">
            Total Tokens
          </Typography>
          <Typography variant="h4" fontWeight={700}>
            {data.totalTokens}
          </Typography>
        </Paper>
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography variant="overline" color="text.secondary">
            Avg Latency
          </Typography>
          <Typography variant="h4" fontWeight={700}>
            {Math.round(data.avgLatencyMs)} ms
          </Typography>
        </Paper>
      </Box>

      <Typography variant="h6" gutterBottom>
        Breakdown by Feature
      </Typography>
      <Paper variant="outlined" sx={{ p: 3 }}>
        {data.breakdown.length === 0 && (
          <Typography color="text.secondary">No usage recorded in this period yet.</Typography>
        )}
        <Stack spacing={2}>
          {data.breakdown.map((item) => (
            <Box key={item.requestType}>
              <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
                <Typography variant="body2">{REQUEST_TYPE_LABELS[item.requestType] || item.requestType}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {item.totalRequests} req &middot; {item.totalTokens} tok &middot; {Math.round(item.avgLatencyMs)} ms
                </Typography>
              </Stack>
              <LinearProgress
                variant="determinate"
                value={(item.totalRequests / maxRequests) * 100}
                sx={{ height: 8, borderRadius: 4 }}
              />
            </Box>
          ))}
        </Stack>
      </Paper>
    </Box>
  );
}
