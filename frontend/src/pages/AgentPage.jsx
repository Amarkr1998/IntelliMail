import { useCallback, useEffect, useRef, useState } from 'react';
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
import UploadFileIcon from '@mui/icons-material/UploadFile';
import EmailEditor from '../components/EmailEditor';
import MarkdownViewer from '../components/MarkdownViewer';
import AgentStepsTimeline from '../components/AgentStepsTimeline';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Loader from '../components/Loader';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import * as agentApi from '../api/agentApi';
import * as emailApi from '../api/emailApi';
import { useSnackbar } from '../context/SnackbarContext';

const MAX_UPLOAD_BYTES = 10 * 1024 * 1024; // must match backend's UPLOAD_MAX_FILE_SIZE (application.yml)

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
  const [referenceContext, setReferenceContext] = useState('');
  const [uploadedFileName, setUploadedFileName] = useState('');
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef(null);

  const contextError = attemptedSubmit && !context.trim() ? 'Instructions are required' : undefined;

  const handleSubmit = async () => {
    setAttemptedSubmit(true);
    if (!goal.trim() || !context.trim()) {
      return;
    }
    setLoading(true);
    try {
      const result = await agentApi.runTask(goal, context || null, conversationId, referenceContext || null);
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
    setReferenceContext('');
    setUploadedFileName('');
  };

  const handleFileSelected = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = ''; // reset so selecting the same file again still fires onChange
    if (!file) {
      return;
    }

    if (file.size > MAX_UPLOAD_BYTES) {
      showSnackbar('File is too large (max 10 MB)', 'error');
      return;
    }

    setUploading(true);
    try {
      const result = await emailApi.extractFile(file);
      setReferenceContext(result.content);
      setUploadedFileName(result.fileName);
      showSnackbar(
        result.truncated
          ? `"${result.fileName}" was longer than the limit — reference text was truncated`
          : `"${result.fileName}" will be used as reference context`,
        result.truncated ? 'warning' : 'success',
      );
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not extract text from this file', 'error');
    } finally {
      setUploading(false);
    }
  };

  const clearReferenceContext = () => {
    setReferenceContext('');
    setUploadedFileName('');
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

          <Paper
            variant="outlined"
            sx={{
              p: 2,
              bgcolor: 'action.hover',
              transition: 'border-color 0.2s ease',
              borderColor: uploadedFileName ? 'primary.main' : undefined,
            }}
          >
            <input
              type="file"
              ref={fileInputRef}
              hidden
              accept=".txt,.pdf,.doc,.docx,.rtf,.odt,.html,.md,.csv"
              onChange={handleFileSelected}
            />
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: uploadedFileName ? 1 : 0 }}>
              <Button
                variant="outlined"
                size="small"
                startIcon={<UploadFileIcon />}
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
              >
                {uploading ? 'Extracting…' : 'Attach a File'}
              </Button>
              {uploadedFileName && (
                <Chip size="small" color="primary" label={uploadedFileName} onDelete={clearReferenceContext} />
              )}
            </Stack>
            {uploadedFileName ? (
              <Typography variant="caption" color="text.secondary">
                Every tool the agent calls will automatically have {referenceContext.length.toLocaleString()}{' '}
                characters from this file available as background reference.
              </Typography>
            ) : (
              <Typography variant="caption" color="text.secondary">
                Optional: attach a document (PDF, Word, plain text, etc.) for the agent to draw on - e.g. a price
                list, policy, or spec sheet.
              </Typography>
            )}
          </Paper>

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
