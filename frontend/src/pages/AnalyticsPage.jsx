import { useEffect, useState } from 'react';
import { Box, Typography, Paper, Stack } from '@mui/material';
import { motion } from 'framer-motion';
import DescriptionIcon from '@mui/icons-material/Description';
import BoltIcon from '@mui/icons-material/Bolt';
import SpeedIcon from '@mui/icons-material/Speed';
import BarChartIcon from '@mui/icons-material/BarChart';
import * as analyticsApi from '../api/analyticsApi';
import Loader from '../components/Loader';
import PageHeader from '../components/common/PageHeader';
import StatCard from '../components/common/StatCard';
import EmptyState from '../components/common/EmptyState';
import AnalyticsBarChart from '../components/charts/AnalyticsBarChart';
import { REQUEST_TYPE_LABELS } from '../utils/requestTypes';
import { useReducedMotionSafe } from '../theme/motion';

export default function AnalyticsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const { staggerContainer, fadeInUp } = useReducedMotionSafe();

  useEffect(() => {
    analyticsApi
      .getAnalytics()
      .then(setData)
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <Loader fullHeight />;
  }

  const chartItems = data.breakdown.map((item) => ({
    label: REQUEST_TYPE_LABELS[item.requestType] || item.requestType,
    value: item.totalRequests,
  }));

  return (
    <Box>
      <PageHeader
        title="Usage Analytics"
        subtitle={`${new Date(data.from).toLocaleDateString()} – ${new Date(data.to).toLocaleDateString()}`}
      />

      <Box
        component={motion.div}
        initial="initial"
        animate="animate"
        variants={staggerContainer}
        sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2, mb: 4 }}
      >
        <motion.div variants={fadeInUp}>
          <StatCard label="Total Requests" value={data.totalRequests} icon={<DescriptionIcon fontSize="small" />} />
        </motion.div>
        <motion.div variants={fadeInUp}>
          <StatCard label="Total Tokens" value={data.totalTokens} icon={<BoltIcon fontSize="small" />} accent="secondary" />
        </motion.div>
        <motion.div variants={fadeInUp}>
          <StatCard label="Avg Latency" value={`${Math.round(data.avgLatencyMs)} ms`} icon={<SpeedIcon fontSize="small" />} />
        </motion.div>
      </Box>

      <Typography variant="h6" gutterBottom>
        Breakdown by Feature
      </Typography>

      {data.breakdown.length === 0 ? (
        <EmptyState icon={<BarChartIcon />} title="No usage recorded in this period yet" />
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1.1fr 0.9fr' }, gap: 3 }}>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <AnalyticsBarChart items={chartItems} height={Math.max(280, chartItems.length * 44)} />
          </Paper>

          {/* Kept visible (not hidden behind hover-only tooltips) so the data stays
              accessible without pointer interaction — the chart is a visual complement. */}
          <Paper variant="outlined" sx={{ p: 3 }}>
            <Stack spacing={2}>
              {data.breakdown.map((item) => (
                <Stack key={item.requestType} direction="row" justifyContent="space-between" alignItems="center">
                  <Typography variant="body2">{REQUEST_TYPE_LABELS[item.requestType] || item.requestType}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {item.totalRequests} req &middot; {item.totalTokens} tok &middot; {Math.round(item.avgLatencyMs)} ms
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </Paper>
        </Box>
      )}
    </Box>
  );
}
