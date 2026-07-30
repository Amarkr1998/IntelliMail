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
  MenuItem,
  TextField,
  LinearProgress,
  Alert,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import CloseIcon from '@mui/icons-material/Close';
import EmailEditor from '../components/EmailEditor';
import MarkdownViewer from '../components/MarkdownViewer';
import AgentStepsTimeline from '../components/AgentStepsTimeline';
import ExportMenu from '../components/ExportMenu';
import SpeechControls from '../components/SpeechControls';
import MicButton from '../components/MicButton';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Loader from '../components/Loader';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import * as agentApi from '../api/agentApi';
import * as emailApi from '../api/emailApi';
import { useSnackbar } from '../context/SnackbarContext';
import useSpeechRecognition from '../hooks/useSpeechRecognition';
import { VOICE_LANGUAGES } from '../utils/voiceLanguages';

const MAX_UPLOAD_BYTES = 10 * 1024 * 1024; // must match backend's UPLOAD_MAX_FILE_SIZE (application.yml)
const MAX_REFERENCE_CHARS = 20_000; // matches backend's @Size cap on referenceContext

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

function combineReferenceContext(files) {
  if (!files.length) {
    return '';
  }
  return files
    .map((f) => `--- ${f.name} ---\n${f.content}`)
    .join('\n\n')
    .slice(0, MAX_REFERENCE_CHARS);
}

function FileDropzone({ files, uploading, dragActive, fileInputRef, onBrowse, onFileInputChange, onDrop, onDragOver, onDragLeave, onRemoveFile }) {
  return (
    <Paper
      variant="outlined"
      onDrop={onDrop}
      onDragOver={onDragOver}
      onDragLeave={onDragLeave}
      sx={{
        p: 2,
        bgcolor: dragActive ? 'action.selected' : 'action.hover',
        borderStyle: dragActive ? 'dashed' : 'solid',
        borderColor: dragActive ? 'primary.main' : files.length ? 'primary.main' : undefined,
        transition: 'border-color 0.2s ease, background-color 0.2s ease',
      }}
    >
      <input
        type="file"
        ref={fileInputRef}
        hidden
        multiple
        accept=".txt,.pdf,.doc,.docx,.rtf,.odt,.html,.md,.csv,.xlsx,.xls,.ppt,.pptx"
        onChange={onFileInputChange}
      />
      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: files.length ? 1 : 0 }}>
        <Button variant="outlined" size="small" startIcon={<UploadFileIcon />} onClick={onBrowse} disabled={uploading}>
          {uploading ? 'Extracting…' : 'Attach Files'}
        </Button>
        <Typography variant="caption" color="text.secondary">
          or drag & drop — PDF, Word, Excel, PowerPoint, plain text, and more
        </Typography>
      </Stack>
      {uploading && <LinearProgress sx={{ mb: 1, borderRadius: 1 }} />}
      {files.length > 0 && (
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {files.map((f) => (
            <Chip
              key={f.id}
              size="small"
              color="primary"
              label={f.truncated ? `${f.name} (truncated)` : f.name}
              onDelete={() => onRemoveFile(f.id)}
              deleteIcon={<CloseIcon />}
            />
          ))}
        </Stack>
      )}
      {files.length > 0 && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          The agent will use these {files.length > 1 ? 'files' : 'file'} as background reference for every tool it calls.
        </Typography>
      )}
    </Paper>
  );
}

function NewTaskTab() {
  const { showSnackbar } = useSnackbar();
  const [goal, setGoal] = useState('');
  const [conversationId, setConversationId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState(null);
  const [actioning, setActioning] = useState(false);
  const [lastGoal, setLastGoal] = useState('');
  const [autoRead, setAutoRead] = useState(false);
  const contentRef = useRef(null);

  // Voice input
  const [sttLanguage, setSttLanguage] = useState(VOICE_LANGUAGES[0].code);
  const [autoSend, setAutoSend] = useState(false);
  const textBeforeListeningRef = useRef('');
  const wasListeningRef = useRef(false);
  const {
    supported: speechSupported,
    listening,
    interimTranscript,
    finalTranscript,
    error: speechError,
    start: startListening,
    stop: stopListening,
    resetTranscript,
    clearError,
  } = useSpeechRecognition({ language: sttLanguage });

  // File upload
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (!listening && !finalTranscript && !interimTranscript) {
      return;
    }
    const base = textBeforeListeningRef.current;
    const combined = [base, finalTranscript].filter(Boolean).join(' ').trim();
    setGoal(interimTranscript ? `${combined} ${interimTranscript}`.trim() : combined);
  }, [finalTranscript, interimTranscript, listening]);

  const handleSubmit = useCallback(async () => {
    if (!goal.trim()) {
      return;
    }
    setLoading(true);
    try {
      const referenceContext = combineReferenceContext(files);
      const result = await agentApi.runTask(goal, null, conversationId, referenceContext || null);
      setResponse(result);
      setLastGoal(goal);
      setConversationId(result.conversationId);
      setGoal('');
      setFiles([]);
      showSnackbar('Agent finished', 'success');
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Agent task failed', 'error');
    } finally {
      setLoading(false);
    }
  }, [goal, files, conversationId, showSnackbar]);

  // Auto-send once the mic stops and produced real transcribed text - opt-in,
  // since forcing an instant send removes any chance to review a mis-heard word.
  useEffect(() => {
    if (wasListeningRef.current && !listening && autoSend && finalTranscript.trim()) {
      handleSubmit();
    }
    wasListeningRef.current = listening;
  }, [listening, autoSend, finalTranscript, handleSubmit]);

  const handleMicToggle = () => {
    if (listening) {
      stopListening();
      return;
    }
    textBeforeListeningRef.current = goal;
    resetTranscript();
    startListening();
  };

  const handleNewConversation = () => {
    setConversationId(null);
    setResponse(null);
    setGoal('');
    setFiles([]);
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

  const processFiles = async (fileList) => {
    const list = Array.from(fileList || []);
    if (!list.length) {
      return;
    }
    setUploading(true);
    try {
      for (const file of list) {
        if (file.size > MAX_UPLOAD_BYTES) {
          showSnackbar(`"${file.name}" is too large (max 10 MB)`, 'error');
          continue;
        }
        try {
          // eslint-disable-next-line no-await-in-loop
          const result = await emailApi.extractFile(file);
          setFiles((prev) => [
            ...prev,
            {
              id: `${result.fileName}-${Date.now()}-${Math.random()}`,
              name: result.fileName,
              content: result.content,
              truncated: result.truncated,
            },
          ]);
        } catch (err) {
          showSnackbar(err?.response?.data?.message || `Could not read "${file.name}"`, 'error');
        }
      }
    } finally {
      setUploading(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragActive(false);
    processFiles(e.dataTransfer.files);
  };

  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          {conversationId && (
            <Stack direction="row" spacing={1} alignItems="center">
              <Chip size="small" color="primary" label="Conversation active - follow-ups will remember this conversation" />
              <Button size="small" startIcon={<RestartAltIcon />} onClick={handleNewConversation}>
                Start New Conversation
              </Button>
            </Stack>
          )}

          {!speechSupported && (
            <Alert severity="info">
              Voice input isn&apos;t supported in this browser (try Chrome or Edge) - you can still type below.
            </Alert>
          )}
          {speechError && (
            <Alert severity="error" onClose={clearError}>
              {speechError}
            </Alert>
          )}

          <Stack direction="row" spacing={1} alignItems="flex-start">
            <EmailEditor
              label="What do you want the agent to do?"
              value={goal}
              onChange={setGoal}
              minRows={7}
              maxLength={4000}
              placeholder="e.g. Reply politely to this email confirming Tuesday at 3pm, then translate the reply to German"
            />
            <MicButton listening={listening} supported={speechSupported} onClick={handleMicToggle} disabled={loading} />
          </Stack>

          {speechSupported && (
            <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
              <TextField
                select
                label="Voice language"
                value={sttLanguage}
                onChange={(e) => setSttLanguage(e.target.value)}
                disabled={listening}
                size="small"
                sx={{ minWidth: 180 }}
              >
                {VOICE_LANGUAGES.map((l) => (
                  <MenuItem key={l.code} value={l.code}>
                    {l.label}
                  </MenuItem>
                ))}
              </TextField>
              <Button
                size="small"
                variant={autoSend ? 'contained' : 'outlined'}
                onClick={() => setAutoSend((v) => !v)}
              >
                Auto-send after voice input: {autoSend ? 'On' : 'Off'}
              </Button>
              {listening && (
                <Typography variant="caption" color="error.main">
                  Listening… tap the microphone again to stop.
                </Typography>
              )}
            </Stack>
          )}

          <FileDropzone
            files={files}
            uploading={uploading}
            dragActive={dragActive}
            fileInputRef={fileInputRef}
            onBrowse={() => fileInputRef.current?.click()}
            onFileInputChange={(e) => {
              processFiles(e.target.files);
              e.target.value = '';
            }}
            onDrop={handleDrop}
            onDragOver={(e) => {
              e.preventDefault();
              setDragActive(true);
            }}
            onDragLeave={(e) => {
              e.preventDefault();
              setDragActive(false);
            }}
            onRemoveFile={(id) => setFiles((prev) => prev.filter((f) => f.id !== id))}
          />

          <Button
            variant="contained"
            size="large"
            endIcon={<AutoAwesomeIcon />}
            onClick={handleSubmit}
            disabled={loading || !goal.trim()}
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
            <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between" sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
              <Chip size="small" color={STATUS_COLORS[response.status] || 'default'} label={response.status} />
              {response.finalResult && (
                <ExportMenu
                  taskId={response.taskId}
                  title={lastGoal || 'AI Agent Response'}
                  content={response.finalResult}
                  contentRef={contentRef}
                />
              )}
            </Stack>
            {response.finalResult && (
              <Box sx={{ mb: 2 }}>
                <SpeechControls text={response.finalResult} autoRead={autoRead} onAutoReadChange={setAutoRead} />
              </Box>
            )}
            <AgentStepsTimeline steps={response.steps} />
            {response.finalResult && (
              <Box sx={{ mt: 2 }} ref={contentRef}>
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
  const historyRefs = useRef({});

  const getContentRef = (taskId) => {
    if (!historyRefs.current[taskId]) {
      historyRefs.current[taskId] = { current: null };
    }
    return historyRefs.current[taskId];
  };

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
                {details[task.id].finalResult && (
                  <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
                    <SpeechControls text={details[task.id].finalResult} />
                    <ExportMenu
                      taskId={task.id}
                      title={task.goal}
                      content={details[task.id].finalResult}
                      contentRef={getContentRef(task.id)}
                    />
                  </Stack>
                )}
                <AgentStepsTimeline steps={details[task.id].steps} />
                {details[task.id].finalResult && (
                  <Box sx={{ mt: 2 }} ref={(el) => { getContentRef(task.id).current = el; }}>
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
