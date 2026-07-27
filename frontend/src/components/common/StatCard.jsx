import { Paper, Typography, Box } from '@mui/material';
import { motion } from 'framer-motion';
import { useReducedMotionSafe } from '../../theme/motion';

const MotionPaper = motion.create(Paper);

/** A single dashboard/analytics stat tile: label, big value, optional icon accent, hover lift. */
export default function StatCard({ label, value, icon, accent = 'primary' }) {
  const { hoverLift } = useReducedMotionSafe();

  return (
    <MotionPaper variant="outlined" sx={{ p: 3, position: 'relative', overflow: 'hidden' }} {...hoverLift}>
      {icon && (
        <Box
          sx={{
            position: 'absolute',
            top: 12,
            right: 12,
            width: 36,
            height: 36,
            borderRadius: '30%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            bgcolor: (theme) =>
              theme.palette.mode === 'dark' ? `${theme.palette[accent].main}26` : `${theme.palette[accent].main}14`,
            color: `${accent}.main`,
          }}
        >
          {icon}
        </Box>
      )}
      <Typography variant="overline" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h4">{value}</Typography>
    </MotionPaper>
  );
}
