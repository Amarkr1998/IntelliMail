import { Box } from '@mui/material';

/** Renders `levels` (an array of bar heights in px) as a live waveform; fades out and idles flat when `active` is false. */
export default function VoiceWaveform({ levels, active }) {
  return (
    <Box
      aria-hidden="true"
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '3px',
        height: 40,
        opacity: active ? 1 : 0.3,
        transition: 'opacity 0.2s',
      }}
    >
      {levels.map((level, index) => (
        <Box
          // Bars are positional and never reorder, so index is a stable, appropriate key here.
          key={index}
          sx={{
            width: 3,
            borderRadius: 1,
            height: `${Math.max(4, level)}px`,
            bgcolor: active ? 'secondary.main' : 'action.disabled',
            transition: 'height 0.08s ease-out',
          }}
        />
      ))}
    </Box>
  );
}
