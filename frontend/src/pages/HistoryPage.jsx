import { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Typography,
  Paper,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  IconButton,
  Chip,
  Stack,
  Pagination,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import DeleteIcon from '@mui/icons-material/Delete';
import * as historyApi from '../api/historyApi';
import ReplyCard from '../components/ReplyCard';
import Loader from '../components/Loader';
import { useSnackbar } from '../context/SnackbarContext';
import { REQUEST_TYPE_LABELS } from '../utils/requestTypes';

export default function HistoryPage() {
  const { showSnackbar } = useSnackbar();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [regeneratingId, setRegeneratingId] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    historyApi
      .getHistory(page, 10)
      .then(setData)
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const handleDelete = async (id, event) => {
    event.stopPropagation();
    try {
      await historyApi.deleteHistoryEntry(id);
      showSnackbar('Deleted', 'success');
      load();
    } catch {
      showSnackbar('Could not delete', 'error');
    }
  };

  const handleRegenerate = async (entryId) => {
    setRegeneratingId(entryId);
    try {
      await historyApi.regenerateReply(entryId);
      showSnackbar('Regenerated', 'success');
      load();
    } catch {
      showSnackbar('Regeneration failed', 'error');
    } finally {
      setRegeneratingId(null);
    }
  };

  const handleToggleFavorite = async (reply) => {
    try {
      await historyApi.setFavorite(reply.id, !reply.favorite);
      load();
    } catch {
      showSnackbar('Could not update favorite', 'error');
    }
  };

  if (loading && !data) {
    return <Loader fullHeight />;
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        History
      </Typography>

      {data?.content.length === 0 && (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography color="text.secondary">No requests yet.</Typography>
        </Paper>
      )}

      {/* AccordionSummary renders as a <button>; nesting the delete IconButton (also a
          <button>) inside it is invalid HTML and silently breaks click handling in real
          browsers (confirmed via a live browser run, not caught by any build/unit test).
          Rendering it as a normal flexbox sibling next to the Accordion, rather than
          absolutely-positioned on top of it, avoids both the invalid nesting and any
          z-index/stacking guesswork. */}
      {data?.content.map((entry) => (
        <Stack key={entry.id} direction="row" alignItems="flex-start" spacing={1} sx={{ mb: 1 }}>
          <Accordion variant="outlined" disableGutters sx={{ flexGrow: 1 }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Stack direction="row" spacing={2} alignItems="center" sx={{ flexGrow: 1, pr: 1 }}>
                <Chip size="small" label={REQUEST_TYPE_LABELS[entry.requestType] || entry.requestType} />
                <Typography noWrap sx={{ flexGrow: 1, maxWidth: 400 }} color="text.secondary" variant="body2">
                  {entry.originalContent}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {new Date(entry.createdAt).toLocaleString()}
                </Typography>
              </Stack>
            </AccordionSummary>
            <AccordionDetails>
              {entry.replies.map((reply) => (
                <ReplyCard
                  key={reply.id}
                  reply={reply}
                  onToggleFavorite={handleToggleFavorite}
                  onRegenerate={() => handleRegenerate(entry.id)}
                  regenerating={regeneratingId === entry.id}
                />
              ))}
            </AccordionDetails>
          </Accordion>
          <IconButton
            size="small"
            onClick={(e) => handleDelete(entry.id, e)}
            aria-label="Delete history entry"
            sx={{ mt: 1 }}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      ))}

      {data && data.totalPages > 1 && (
        <Stack alignItems="center" sx={{ mt: 2 }}>
          <Pagination count={data.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
        </Stack>
      )}
    </Box>
  );
}
