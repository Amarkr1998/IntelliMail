import { Card, CardContent, CardActions, IconButton, Typography, Tooltip, Stack, Chip } from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import VolumeUpIcon from '@mui/icons-material/VolumeUp';
import VolumeOffIcon from '@mui/icons-material/VolumeOff';
import MarkdownViewer from './MarkdownViewer';
import useTextToSpeech from '../hooks/useTextToSpeech';
import { useSnackbar } from '../context/SnackbarContext';

export default function VoiceResponseCard({ interaction }) {
  const { showSnackbar } = useSnackbar();
  const { supported: speechSupported, speaking, toggleSpeak } = useTextToSpeech();

  const handleCopy = async () => {
    await navigator.clipboard.writeText(interaction.aiResponse);
    showSnackbar('Copied to clipboard', 'success');
  };

  const handleToggleSpeak = () => {
    if (!speechSupported) {
      showSnackbar('Text-to-speech is not supported in this browser', 'error');
      return;
    }
    toggleSpeak(interaction.aiResponse);
  };

  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
          <Chip size="small" color="secondary" label="Voice AI" />
          {interaction.totalTokens != null && (
            <Typography variant="caption" color="text.secondary">
              {interaction.totalTokens} tokens &middot; {interaction.latencyMs} ms &middot; {interaction.aiModel}
            </Typography>
          )}
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1, fontStyle: 'italic' }}>
          You said: &ldquo;{interaction.transcript}&rdquo;
        </Typography>
        <MarkdownViewer content={interaction.aiResponse} />
      </CardContent>
      <CardActions disableSpacing>
        <Tooltip title="Copy">
          <IconButton onClick={handleCopy} aria-label="Copy AI response">
            <ContentCopyIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title={speaking ? 'Stop reading' : 'Read aloud'}>
          <IconButton onClick={handleToggleSpeak} color={speaking ? 'primary' : 'default'} aria-label={speaking ? 'Stop reading AI response aloud' : 'Read AI response aloud'}>
            {speaking ? <VolumeOffIcon fontSize="small" /> : <VolumeUpIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
      </CardActions>
    </Card>
  );
}
