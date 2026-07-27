import { Box, Stack, Typography } from '@mui/material';
import { motion } from 'framer-motion';
import PsychologyIcon from '@mui/icons-material/Psychology';
import GlassCard from './GlassCard';
import { useReducedMotionSafe } from '../../theme/motion';

/** Shared illustrated-card shell for Login/Register — they duplicate 100% of this outer structure otherwise. */
export default function AuthLayout({ illustration, title, subtitle, children, footer }) {
  const { fadeInUp } = useReducedMotionSafe();

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: { xs: 2, sm: 4 },
        background: (theme) => theme.palette.custom.gradient,
      }}
    >
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        sx={{
          width: '100%',
          maxWidth: 960,
          borderRadius: 4,
          overflow: 'hidden',
          boxShadow: '0 30px 60px -20px rgba(15,23,42,0.25)',
        }}
      >
        <Box
          component={motion.div}
          initial="initial"
          animate="animate"
          variants={fadeInUp}
          sx={{
            display: { xs: 'none', md: 'flex' },
            flex: '0 0 45%',
            alignItems: 'center',
            justifyContent: 'center',
            p: 5,
            bgcolor: (theme) => (theme.palette.mode === 'dark' ? 'rgba(79,70,229,0.14)' : 'rgba(79,70,229,0.06)'),
          }}
        >
          {illustration}
        </Box>
        <Box component={motion.div} initial="initial" animate="animate" variants={fadeInUp} sx={{ flex: 1, display: 'flex' }}>
          <GlassCard sx={{ p: { xs: 3, sm: 5 }, borderRadius: 0, width: '100%' }}>
            <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 3 }}>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 36,
                  height: 36,
                  borderRadius: '26%',
                  bgcolor: 'primary.main',
                  color: 'primary.contrastText',
                }}
              >
                <PsychologyIcon fontSize="small" />
              </Box>
              <Typography variant="h6" fontWeight={700}>
                IntelliMail
              </Typography>
            </Stack>
            <Typography variant="h5" fontWeight={700} gutterBottom>
              {title}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              {subtitle}
            </Typography>
            {children}
            {footer && (
              <Typography variant="body2" sx={{ mt: 3, textAlign: 'center' }}>
                {footer}
              </Typography>
            )}
          </GlassCard>
        </Box>
      </Stack>
    </Box>
  );
}
