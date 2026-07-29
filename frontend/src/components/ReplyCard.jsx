import { Card, CardContent, CardActions, IconButton, Typography, Chip, Tooltip, Stack } from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import FavoriteIcon from '@mui/icons-material/Favorite';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import RefreshIcon from '@mui/icons-material/Refresh';
import VolumeUpIcon from '@mui/icons-material/VolumeUp';
import VolumeOffIcon from '@mui/icons-material/VolumeOff';
import MarkdownViewer from './MarkdownViewer';
import useTextToSpeech from '../hooks/useTextToSpeech';
import { useSnackbar } from '../context/SnackbarContext';

export default function ReplyCard({ reply, onToggleFavorite, onRegenerate, regenerating }) {
  const { showSnackbar } = useSnackbar();
  const { supported: speechSupported, speaking, toggleSpeak } = useTextToSpeech();

  const handleCopy = async () => {
    await navigator.clipboard.writeText(reply.content);
    showSnackbar('Copied to clipboard', 'success');
  };

  const handleToggleSpeak = () => {
    if (!speechSupported) {
      showSnackbar('Text-to-speech is not supported in this browser', 'error');
      return;
    }
    toggleSpeak(reply.content);
  };

  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
          <Chip size="small" label={`Attempt #${reply.attemptNumber}`} />
          {reply.totalTokens != null && (
            <Typography variant="caption" color="text.secondary">
              {reply.totalTokens} tokens &middot; {reply.latencyMs} ms &middot; {reply.aiModel}
            </Typography>
          )}
        </Stack>
        <MarkdownViewer content={reply.content} />
      </CardContent>
      <CardActions disableSpacing>
        <Tooltip title="Copy">
          <IconButton onClick={handleCopy} aria-label="Copy reply">
            <ContentCopyIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title={speaking ? 'Stop reading' : 'Read aloud'}>
          <IconButton onClick={handleToggleSpeak} color={speaking ? 'primary' : 'default'} aria-label={speaking ? 'Stop reading reply aloud' : 'Read reply aloud'}>
            {speaking ? <VolumeOffIcon fontSize="small" /> : <VolumeUpIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
        <Tooltip title={reply.favorite ? 'Unfavorite' : 'Favorite'}>
          <IconButton
            onClick={() => onToggleFavorite(reply)}
            color={reply.favorite ? 'error' : 'default'}
            aria-label="Toggle favorite"
          >
            {reply.favorite ? <FavoriteIcon fontSize="small" /> : <FavoriteBorderIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
        {onRegenerate && (
          <Tooltip title="Regenerate">
            <span>
              <IconButton onClick={onRegenerate} disabled={regenerating} aria-label="Regenerate reply">
                <RefreshIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        )}
      </CardActions>
    </Card>
  );
}
