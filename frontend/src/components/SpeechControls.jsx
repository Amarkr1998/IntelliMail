import { useEffect, useRef, useState } from 'react';
import { Stack, IconButton, Tooltip, Slider, Typography, Box, FormControlLabel, Switch } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import PauseIcon from '@mui/icons-material/Pause';
import StopIcon from '@mui/icons-material/Stop';
import VolumeUpIcon from '@mui/icons-material/VolumeUp';
import SpeedIcon from '@mui/icons-material/Speed';
import useTextToSpeech from '../hooks/useTextToSpeech';

/**
 * Full playback controls (play/pause/resume/stop, rate, volume, optional
 * auto-read toggle) for reading a piece of text aloud - built for the AI
 * Agent page, where ReplyCard/VoiceResponseCard's simpler single play/stop
 * button isn't enough.
 *
 * Rate/volume only take effect on the *next* utterance - the Web Speech API
 * has no way to change either mid-utterance, a real browser limitation, not
 * a gap in this component.
 */
export default function SpeechControls({ text, autoRead = false, onAutoReadChange }) {
  const { supported, speaking, paused, speak, pause, resume, stop } = useTextToSpeech();
  const [rate, setRate] = useState(1);
  const [volume, setVolume] = useState(1);
  const lastAutoReadTextRef = useRef(null);

  useEffect(() => {
    if (autoRead && supported && text && lastAutoReadTextRef.current !== text) {
      lastAutoReadTextRef.current = text;
      speak(text, { rate, volume });
    }
    // Only re-run when the text or the auto-read toggle changes - not on
    // every rate/volume slider tick, which would restart playback.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoRead, text, supported]);

  if (!supported) {
    return (
      <Typography variant="caption" color="text.secondary">
        Read-aloud isn&apos;t supported in this browser.
      </Typography>
    );
  }

  const handlePlayPause = () => {
    if (!speaking) {
      speak(text, { rate, volume });
      return;
    }
    if (paused) {
      resume();
    } else {
      pause();
    }
  };

  return (
    <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap>
      <Tooltip title={speaking && !paused ? 'Pause' : speaking && paused ? 'Resume' : 'Read aloud'}>
        <span>
          <IconButton
            size="small"
            onClick={handlePlayPause}
            disabled={!text}
            color={speaking ? 'primary' : 'default'}
            aria-label={speaking && !paused ? 'Pause reading' : speaking && paused ? 'Resume reading' : 'Read aloud'}
          >
            {speaking && !paused ? <PauseIcon fontSize="small" /> : <PlayArrowIcon fontSize="small" />}
          </IconButton>
        </span>
      </Tooltip>
      <Tooltip title="Stop">
        <span>
          <IconButton size="small" onClick={stop} disabled={!speaking} aria-label="Stop reading">
            <StopIcon fontSize="small" />
          </IconButton>
        </span>
      </Tooltip>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, width: 96 }}>
        <Tooltip title={`Speed: ${rate.toFixed(1)}x`}>
          <SpeedIcon fontSize="small" color="action" />
        </Tooltip>
        <Slider size="small" min={0.5} max={2} step={0.1} value={rate} onChange={(_, v) => setRate(v)} aria-label="Speech speed" />
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, width: 96 }}>
        <Tooltip title={`Volume: ${Math.round(volume * 100)}%`}>
          <VolumeUpIcon fontSize="small" color="action" />
        </Tooltip>
        <Slider size="small" min={0} max={1} step={0.1} value={volume} onChange={(_, v) => setVolume(v)} aria-label="Speech volume" />
      </Box>

      {onAutoReadChange && (
        <FormControlLabel
          sx={{ ml: 0 }}
          control={<Switch size="small" checked={autoRead} onChange={(e) => onAutoReadChange(e.target.checked)} />}
          label={<Typography variant="caption">Auto-read</Typography>}
        />
      )}
    </Stack>
  );
}
