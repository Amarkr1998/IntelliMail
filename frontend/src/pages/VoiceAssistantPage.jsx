import { useCallback, useEffect, useRef, useState } from 'react';
import { Box, Paper, TextField, Button, Typography, Stack, MenuItem, Alert, Pagination } from '@mui/material';
import { AnimatePresence, motion } from 'framer-motion';
import SendIcon from '@mui/icons-material/Send';
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver';
import MicButton from '../components/MicButton';
import VoiceWaveform from '../components/VoiceWaveform';
import VoiceResponseCard from '../components/VoiceResponseCard';
import Loader from '../components/Loader';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import useSpeechRecognition from '../hooks/useSpeechRecognition';
import useAudioWaveform from '../hooks/useAudioWaveform';
import * as voiceApi from '../api/voiceApi';
import { useSnackbar } from '../context/SnackbarContext';
import { VOICE_LANGUAGES } from '../utils/voiceLanguages';
import { useReducedMotionSafe } from '../theme/motion';

export default function VoiceAssistantPage() {
  const { showSnackbar } = useSnackbar();
  const [languageCode, setLanguageCode] = useState(VOICE_LANGUAGES[0].code);
  const [promptText, setPromptText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [response, setResponse] = useState(null);
  const [history, setHistory] = useState(null);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyLoading, setHistoryLoading] = useState(true);

  // Text already in the field before the mic was pressed - live transcript is
  // appended after this rather than replacing it, so typing and dictation compose.
  const textBeforeListeningRef = useRef('');
  const { fadeInUp, staggerContainer } = useReducedMotionSafe();

  const {
    supported,
    listening,
    interimTranscript,
    finalTranscript,
    error: speechError,
    start,
    stop,
    resetTranscript,
    clearError,
  } = useSpeechRecognition({ language: languageCode });

  const { levels, permissionError: waveformError } = useAudioWaveform(listening);

  useEffect(() => {
    if (!listening && !finalTranscript && !interimTranscript) {
      return;
    }
    const base = textBeforeListeningRef.current;
    const combined = [base, finalTranscript].filter(Boolean).join(' ').trim();
    setPromptText(interimTranscript ? `${combined} ${interimTranscript}`.trim() : combined);
  }, [finalTranscript, interimTranscript, listening]);

  const loadHistory = useCallback(() => {
    setHistoryLoading(true);
    voiceApi
      .getVoiceHistory(historyPage, 5)
      .then(setHistory)
      .finally(() => setHistoryLoading(false));
  }, [historyPage]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  const handleMicToggle = () => {
    if (listening) {
      stop();
      return;
    }
    textBeforeListeningRef.current = promptText;
    resetTranscript();
    start();
  };

  const handleSubmit = async () => {
    if (!promptText.trim()) {
      return;
    }
    if (listening) {
      stop();
    }
    setSubmitting(true);
    try {
      const selectedLanguage = VOICE_LANGUAGES.find((l) => l.code === languageCode);
      const result = await voiceApi.submitVoicePrompt(promptText.trim(), selectedLanguage?.label);
      setResponse(result);
      setPromptText('');
      textBeforeListeningRef.current = '';
      resetTranscript();
      showSnackbar('AI response generated', 'success');
      setHistoryPage(0);
      loadHistory();
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Voice request failed', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Voice AI"
        subtitle="Speak your prompt instead of typing — dictate an email to draft, rewrite, or ask a general question, and the AI will respond."
      />

      {!supported && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Voice input isn&apos;t supported in this browser. Try Google Chrome or Microsoft Edge, or just type your
          prompt below.
        </Alert>
      )}
      {speechError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={clearError}>
          {speechError}
        </Alert>
      )}
      {waveformError && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {waveformError} (voice-to-text still works — only the waveform animation is affected.)
        </Alert>
      )}

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
        <Paper
          variant="outlined"
          sx={{
            p: 3,
            transition: 'border-color 0.2s ease',
            borderColor: listening ? 'error.main' : undefined,
          }}
        >
          <Stack spacing={2}>
            <TextField
              select
              label="Voice language"
              value={languageCode}
              onChange={(e) => setLanguageCode(e.target.value)}
              disabled={listening}
              size="small"
            >
              {VOICE_LANGUAGES.map((l) => (
                <MenuItem key={l.code} value={l.code}>
                  {l.label}
                </MenuItem>
              ))}
            </TextField>

            <Stack direction="row" spacing={1} alignItems="flex-start">
              <TextField
                label="Your prompt"
                placeholder="Tap the microphone and speak, or type here..."
                value={promptText}
                onChange={(e) => setPromptText(e.target.value)}
                multiline
                minRows={6}
                fullWidth
              />
              <MicButton listening={listening} supported={supported} onClick={handleMicToggle} disabled={submitting} />
            </Stack>

            <VoiceWaveform levels={levels} active={listening} />
            {listening && (
              <Typography variant="caption" color="text.secondary" textAlign="center">
                Listening… tap the microphone again to stop.
              </Typography>
            )}

            <Button
              variant="contained"
              size="large"
              endIcon={<SendIcon />}
              onClick={handleSubmit}
              disabled={submitting || !promptText.trim()}
            >
              {submitting ? 'Asking AI…' : 'Ask AI'}
            </Button>
          </Stack>
        </Paper>

        <Box>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            AI Response
          </Typography>
          {submitting && <Loader />}
          {!submitting && !response && (
            <Paper variant="outlined" sx={{ p: 3 }}>
              <Typography color="text.secondary">Your AI response will appear here.</Typography>
            </Paper>
          )}
          <AnimatePresence>
            {!submitting && response && (
              <motion.div key={response.id} initial="initial" animate="animate" exit="exit" variants={fadeInUp}>
                <VoiceResponseCard interaction={response} />
              </motion.div>
            )}
          </AnimatePresence>
        </Box>
      </Box>

      <Box sx={{ mt: 4 }}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          Recent Voice Prompts
        </Typography>
        {historyLoading && !history && <Loader />}
        {history?.content.length === 0 && (
          <EmptyState icon={<RecordVoiceOverIcon />} title="No voice prompts yet" description="Tap the microphone above to ask your first question." />
        )}
        <Box component={motion.div} initial="initial" animate="animate" variants={staggerContainer}>
          {history?.content.map((interaction) => (
            <motion.div key={interaction.id} variants={fadeInUp}>
              <VoiceResponseCard interaction={interaction} />
            </motion.div>
          ))}
        </Box>
        {history && history.totalPages > 1 && (
          <Stack alignItems="center" sx={{ mt: 2 }}>
            <Pagination count={history.totalPages} page={historyPage + 1} onChange={(_, p) => setHistoryPage(p - 1)} />
          </Stack>
        )}
      </Box>
    </Box>
  );
}
