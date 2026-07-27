import { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Typography,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  IconButton,
  Chip,
  Stack,
  Pagination,
} from '@mui/material';
import { motion } from 'framer-motion';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import DeleteIcon from '@mui/icons-material/Delete';
import HistoryIcon from '@mui/icons-material/History';
import * as historyApi from '../api/historyApi';
import ReplyCard from '../components/ReplyCard';
import Loader from '../components/Loader';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import { useSnackbar } from '../context/SnackbarContext';
import { REQUEST_TYPE_LABELS } from '../utils/requestTypes';
import { useReducedMotionSafe } from '../theme/motion';

export default function HistoryPage() {
  const { showSnackbar } = useSnackbar();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [regeneratingId, setRegeneratingId] = useState(null);
  const { staggerContainer, fadeInUp } = useReducedMotionSafe();

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
      <PageHeader title="History" subtitle="Every AI request you've made, with all regeneration attempts." />

      {data?.content.length === 0 && (
        <EmptyState icon={<HistoryIcon />} title="No requests yet" description="Your AI request history will show up here." />
      )}

      {/* AccordionSummary renders as a <button>; nesting the delete IconButton (also a
          <button>) inside it is invalid HTML and silently breaks click handling in real
          browsers (confirmed via a live browser run, not caught by any build/unit test).
          Rendering it as a normal flexbox sibling next to the Accordion, rather than
          absolutely-positioned on top of it, avoids both the invalid nesting and any
          z-index/stacking guesswork. */}
      <Box component={motion.div} initial="initial" animate="animate" variants={staggerContainer}>
        {data?.content.map((entry) => (
          <Stack
            key={entry.id}
            component={motion.div}
            variants={fadeInUp}
            direction="row"
            alignItems="flex-start"
            spacing={1}
            sx={{ mb: 1 }}
          >
            <Accordion
              variant="outlined"
              disableGutters
              sx={{
                flexGrow: 1,
                transition: 'box-shadow 0.2s ease',
                '&:hover': { boxShadow: (theme) => (theme.palette.mode === 'dark' ? '0 4px 16px -6px rgba(0,0,0,0.5)' : '0 4px 16px -6px rgba(15,23,42,0.15)') },
              }}
            >
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
      </Box>

      {data && data.totalPages > 1 && (
        <Stack alignItems="center" sx={{ mt: 2 }}>
          <Pagination count={data.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
        </Stack>
      )}
    </Box>
  );
}
