import { BarChart } from '@mui/x-charts/BarChart';
import { useTheme } from '@mui/material/styles';

/**
 * Horizontal bar chart ranking request types by count. A single measure across
 * categories (not multiple simultaneous series), so category identity comes from
 * the axis labels rather than color — one hue for all bars is the correct,
 * simplest choice here (no categorical palette needed), per the dataviz skill's
 * form-selection guidance. Built-in hover tooltip ships by default; the caller is
 * expected to also render the same numbers as visible text alongside the chart
 * so the data isn't locked behind hover-only interaction.
 */
export default function AnalyticsBarChart({ items, height = 320 }) {
  const theme = useTheme();

  const labels = items.map((item) => item.label);
  const values = items.map((item) => item.value);

  return (
    <BarChart
      height={height}
      layout="horizontal"
      borderRadius={6}
      series={[{ data: values, color: theme.palette.primary.main, label: 'Requests', valueFormatter: (v) => `${v} requests` }]}
      yAxis={[{ data: labels, scaleType: 'band', width: 150 }]}
      xAxis={[
        {
          scaleType: 'linear',
          // Request counts are always integers; hide the fractional ticks the linear
          // scale's "nice number" algorithm otherwise picks when the max value is small
          // (e.g. 0, 0.2, 0.4 ... for a max of 1) rather than showing misleading decimals.
          valueFormatter: (value) => (Number.isInteger(value) ? String(value) : ''),
        },
      ]}
      grid={{ vertical: true }}
      hideLegend
      margin={{ left: 24, right: 24, top: 16, bottom: 32 }}
    />
  );
}
