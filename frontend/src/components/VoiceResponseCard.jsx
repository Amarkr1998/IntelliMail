import { Card, CardContent, CardActions, IconButton, Typography, Tooltip, Stack, Chip } from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import MarkdownViewer from './MarkdownViewer';
import { useSnackbar } from '../context/SnackbarContext';

export default function VoiceResponseCard({ interaction }) {
  const { showSnackbar } = useSnackbar();

  const handleCopy = async () => {
    await navigator.clipboard.writeText(interaction.aiResponse);
    showSnackbar('Copied to clipboard', 'success');
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
      </CardActions>
    </Card>
  );
}
