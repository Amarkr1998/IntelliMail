import { Box, IconButton, Tooltip } from '@mui/material';
import { motion } from 'framer-motion';
import MicIcon from '@mui/icons-material/Mic';
import MicOffIcon from '@mui/icons-material/MicOff';
import { useReducedMotionSafe } from '../theme/motion';

export default function MicButton({ listening, supported, onClick, disabled }) {
  const { hoverLift } = useReducedMotionSafe();

  if (!supported) {
    return (
      <Tooltip title="Voice input isn't supported in this browser. Try Chrome or Edge.">
        <span>
          <IconButton disabled aria-label="Microphone unavailable" sx={{ minWidth: 44, minHeight: 44 }}>
            <MicOffIcon />
          </IconButton>
        </span>
      </Tooltip>
    );
  }

  return (
    <Tooltip title={listening ? 'Stop listening' : 'Start voice input'}>
      <Box sx={{ position: 'relative', display: 'inline-flex' }}>
        {listening && (
          <Box
            component={motion.span}
            animate={{ scale: [1, 1.8], opacity: [0.6, 0] }}
            transition={{ duration: 1.4, repeat: Infinity, ease: 'easeOut' }}
            sx={{
              position: 'absolute',
              inset: 0,
              borderRadius: '50%',
              border: '2px solid',
              borderColor: 'error.main',
              pointerEvents: 'none',
            }}
          />
        )}
        <span>
          <IconButton
            onClick={onClick}
            disabled={disabled}
            color={listening ? 'error' : 'primary'}
            aria-label={listening ? 'Stop voice input' : 'Start voice input'}
            component={motion.button}
            {...hoverLift}
            sx={{ minWidth: 44, minHeight: 44 }}
          >
            <MicIcon />
          </IconButton>
        </span>
      </Box>
    </Tooltip>
  );
}
