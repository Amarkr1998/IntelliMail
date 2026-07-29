import { useCallback, useEffect, useState } from 'react';
import {
  Box,
  Paper,
  Tabs,
  Tab,
  Button,
  Chip,
  Typography,
  Stack,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Pagination,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import EmailEditor from '../components/EmailEditor';
import MarkdownViewer from '../components/MarkdownViewer';
import AgentStepsTimeline from '../components/AgentStepsTimeline';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Loader from '../components/Loader';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import * as agentApi from '../api/agentApi';
import { useSnackbar } from '../context/SnackbarContext';

const STATUS_COLORS = {
  COMPLETED: 'success',
  AWAITING_CONFIRMATION: 'warning',
  FAILED: 'error',
  REJECTED: 'default',
  IN_PROGRESS: 'info',
};

function pendingActionDescription(pendingAction) {
  if (!pendingAction) {
    return '';
  }
  const { payload } = pendingAction;
  return `Save a template named "${payload.name}" in category ${payload.category}?\n\n${payload.promptText}`;
}

function NewTaskTab() {
  const { showSnackbar } = useSnackbar();
  const [goal, setGoal] = useState('');
  const [context, setContext] = useState('');
  const [conversationId, setConversationId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState(null);
  const [actioning, setActioning] = useState(false);
  const [attemptedSubmit, setAttemptedSubmit] = useState(false);

  const contextError = attemptedSubmit && !context.trim() ? 'Instructions are required' : undefined;

  const handleSubmit = async () => {
    setAttemptedSubmit(true);
    if (!goal.trim() || !context.trim()) {
      return;
    }
    setLoading(true);
    try {
      const result = await agentApi.runTask(goal, context || null, conversationId);
      setResponse(result);
      setConversationId(result.conversationId);
      setGoal('');
      setAttemptedSubmit(false);
      showSnackbar('Agent finished', 'success');
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Agent task failed', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleNewConversation = () => {
    setConversationId(null);
    setResponse(null);
    setGoal('');
    setContext('');
    setAttemptedSubmit(false);
  };

  const handleConfirm = async () => {
    if (!response) {
      return;
    }
    setActioning(true);
    try {
      const updated = await agentApi.confirmPendingAction(response.taskId);
      setResponse(updated);
      showSnackbar('Confirmed', 'success');
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not confirm', 'error');
    } finally {
      setActioning(false);
    }
  };

  const handleReject = async () => {
    if (!response) {
      return;
    }
    setActioning(true);
    try {
      const updated = await agentApi.rejectPendingAction(response.taskId);
      setResponse(updated);
      showSnackbar('Rejected', 'success');
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not reject', 'error');
    } finally {
      setActioning(false);
    }
  };

  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          {conversationId && (
            <Stack direction="row" spacing={1} alignItems="center">
              <Chip size="small" color="primary" label="Conversation active - follow-ups will use context" />
              <Button size="small" startIcon={<RestartAltIcon />} onClick={handleNewConversation}>
                Start New Conversation
              </Button>
            </Stack>
          )}
          <EmailEditor
            label="What do you want the agent to do?"
            value={goal}
            onChange={setGoal}
            minRows={3}
            maxLength={4000}
            placeholder="e.g. Reply politely to this email, then translate the reply to German"
          />
          <EmailEditor
            label="Instructions"
            value={context}
            onChange={setContext}
            minRows={6}
            maxLength={20000}
            placeholder="Paste the original email or any other background text here"
            error={contextError}
          />
          <Button
            variant="contained"
            size="large"
            endIcon={<AutoAwesomeIcon />}
            onClick={handleSubmit}
            disabled={loading || !goal.trim() || (attemptedSubmit && !context.trim())}
          >
            {loading ? 'Working…' : 'Run Agent'}
          </Button>
        </Stack>
      </Paper>

      <Box>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          Agent Output
        </Typography>
        {loading && <Loader />}
        {!loading && !response && (
          <Paper variant="outlined" sx={{ p: 3 }}>
            <Typography color="text.secondary">The agent&apos;s steps and result will appear here.</Typography>
          </Paper>
        )}
        {!loading && response && (
          <Paper variant="outlined" sx={{ p: 3 }}>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
              <Chip size="small" color={STATUS_COLORS[response.status] || 'default'} label={response.status} />
            </Stack>
            <AgentStepsTimeline steps={response.steps} />
            {response.finalResult && (
              <Box sx={{ mt: 2 }}>
                <MarkdownViewer content={response.finalResult} />
              </Box>
            )}
          </Paper>
        )}
      </Box>

      <ConfirmDialog
        open={Boolean(response?.pendingAction)}
        title="Confirm proposed action"
        description={pendingActionDescription(response?.pendingAction)}
        confirming={actioning}
        onConfirm={handleConfirm}
        onCancel={handleReject}
      />
    </Box>
  );
}

function TaskHistoryTab() {
  const { showSnackbar } = useSnackbar();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [details, setDetails] = useState({});

  const load = useCallback(() => {
    setLoading(true);
    agentApi
      .listTasks(page, 10)
      .then(setData)
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const handleExpand = async (taskId) => {
    if (details[taskId]) {
      return;
    }
    try {
      const detail = await agentApi.getTask(taskId);
      setDetails((prev) => ({ ...prev, [taskId]: detail }));
    } catch {
      showSnackbar('Could not load task detail', 'error');
    }
  };

  if (loading && !data) {
    return <Loader fullHeight />;
  }

  return (
    <Box>
      {data?.content.length === 0 && (
        <EmptyState icon={<SmartToyIcon />} title="No agent tasks yet" description="Tasks you run will show up here." />
      )}

      {data?.content.map((task) => (
        <Accordion key={task.id} variant="outlined" disableGutters sx={{ mb: 1 }} onChange={() => handleExpand(task.id)}>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Stack direction="row" spacing={2} alignItems="center" sx={{ flexGrow: 1, pr: 1 }}>
              <Chip size="small" color={STATUS_COLORS[task.status] || 'default'} label={task.status} />
              <Typography noWrap sx={{ flexGrow: 1, maxWidth: 400 }} color="text.secondary" variant="body2">
                {task.goal}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {new Date(task.createdAt).toLocaleString()}
              </Typography>
            </Stack>
          </AccordionSummary>
          <AccordionDetails>
            {details[task.id] ? (
              <>
                <AgentStepsTimeline steps={details[task.id].steps} />
                {details[task.id].finalResult && (
                  <Box sx={{ mt: 2 }}>
                    <MarkdownViewer content={details[task.id].finalResult} />
                  </Box>
                )}
              </>
            ) : (
              <Loader />
            )}
          </AccordionDetails>
        </Accordion>
      ))}

      {data && data.totalPages > 1 && (
        <Stack alignItems="center" sx={{ mt: 2 }}>
          <Pagination count={data.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
        </Stack>
      )}
    </Box>
  );
}

export default function AgentPage() {
  const [tab, setTab] = useState('new');

  return (
    <Box>
      <PageHeader
        title="AI Agent"
        subtitle="Give the agent a goal and it will chain the right tools together - drafting, rewriting, translating, summarizing, and more."
      />
      <Paper variant="outlined" sx={{ mb: 3 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab label="New Task" value="new" />
          <Tab label="Task History" value="history" />
        </Tabs>
      </Paper>

      {tab === 'new' ? <NewTaskTab /> : <TaskHistoryTab />}
    </Box>
  );
}
