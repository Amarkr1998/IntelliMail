import { useEffect, useRef, useState } from 'react';
import { Box, Paper, Tabs, Tab, Button, Chip, MenuItem, TextField, Typography, Stack } from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import EmailEditor from '../components/EmailEditor';
import ReplyCard from '../components/ReplyCard';
import Loader from '../components/Loader';
import * as emailApi from '../api/emailApi';
import * as historyApi from '../api/historyApi';
import * as templateApi from '../api/templateApi';
import { useSnackbar } from '../context/SnackbarContext';
import { REWRITE_STYLES, CUSTOM_GENERATOR_TYPES } from '../utils/requestTypes';

const MAX_UPLOAD_BYTES = 10 * 1024 * 1024; // must match backend's UPLOAD_MAX_FILE_SIZE (application.yml)

export default function ComposeAssistantPage() {
  const { showSnackbar } = useSnackbar();
  const [tab, setTab] = useState('generate');
  const [content, setContent] = useState('');
  const [instructions, setInstructions] = useState('');
  const [style, setStyle] = useState(REWRITE_STYLES[0].value);
  const [targetLanguage, setTargetLanguage] = useState('French');
  const [customType, setCustomType] = useState(CUSTOM_GENERATOR_TYPES[0].value);
  const [customPrompt, setCustomPrompt] = useState('');
  const [templateId, setTemplateId] = useState('');
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [reply, setReply] = useState(null);
  // The uploaded file's extracted text is kept entirely separate from `content` -
  // it's background information the AI may draw on, never the email being acted on.
  const [referenceContext, setReferenceContext] = useState('');
  const [uploadedFileName, setUploadedFileName] = useState('');
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef(null);

  useEffect(() => {
    templateApi
      .getTemplates(0, 100)
      .then((page) => setTemplates(page.content))
      .catch(() => {
        // Templates are optional context for generation; silently degrade if unavailable.
      });
  }, []);

  const resetOutput = () => setReply(null);

  const handleTabChange = (_, value) => {
    setTab(value);
    resetOutput();
  };

  const handleSubmit = async () => {
    setLoading(true);
    resetOutput();
    try {
      let result;
      switch (tab) {
        case 'generate':
          result = await emailApi.generateReply({
            originalContent: content,
            instructions: instructions || null,
            promptTemplateId: templateId || null,
            referenceContext: referenceContext || null,
          });
          break;
        case 'improve':
          result = await emailApi.improveEmail({ content, style, referenceContext: referenceContext || null });
          break;
        case 'translate':
          result = await emailApi.translateEmail({ content, targetLanguage, referenceContext: referenceContext || null });
          break;
        case 'summarize':
          result = await emailApi.summarizeEmail({ content, referenceContext: referenceContext || null });
          break;
        case 'subject':
          result = await emailApi.generateSubject({ content, referenceContext: referenceContext || null });
          break;
        case 'followup':
          result = await emailApi.generateFollowup({
            originalContent: content,
            instructions: instructions || null,
            referenceContext: referenceContext || null,
          });
          break;
        case 'custom':
          result = await emailApi.generateCustom({
            requestType: customType,
            context: content,
            customPrompt: customPrompt || null,
            promptTemplateId: templateId || null,
            referenceContext: referenceContext || null,
          });
          break;
        default:
          return;
      }
      setReply(result);
      showSnackbar('AI reply generated', 'success');
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Generation failed', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleRegenerate = async () => {
    if (!reply) {
      return;
    }
    setRegenerating(true);
    try {
      const result = await historyApi.regenerateReply(reply.emailRequestId);
      setReply(result);
      showSnackbar('Regenerated', 'success');
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Regeneration failed', 'error');
    } finally {
      setRegenerating(false);
    }
  };

  const handleToggleFavorite = async (currentReply) => {
    try {
      const updated = await historyApi.setFavorite(currentReply.id, !currentReply.favorite);
      setReply(updated);
    } catch {
      showSnackbar('Could not update favorite', 'error');
    }
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
      // Deliberately NOT setContent(result.content) - the uploaded file is background
      // reference material for the AI, not the email being replied to/rewritten/etc.
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

  const contentLabel =
    tab === 'generate' || tab === 'followup' ? 'Original Email' : tab === 'custom' ? 'Context' : 'Email Content';

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Compose Assistant
      </Typography>
      <Paper variant="outlined" sx={{ mb: 3 }}>
        <Tabs value={tab} onChange={handleTabChange} variant="scrollable" scrollButtons="auto">
          <Tab label="Generate Reply" value="generate" />
          <Tab label="Improve / Rewrite" value="improve" />
          <Tab label="Translate" value="translate" />
          <Tab label="Summarize" value="summarize" />
          <Tab label="Subject Line" value="subject" />
          <Tab label="Follow-up" value="followup" />
          <Tab label="Custom / Generators" value="custom" />
        </Tabs>
      </Paper>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Stack spacing={2}>
            <EmailEditor label={contentLabel} value={content} onChange={setContent} minRows={8} />

            {(tab === 'generate' || tab === 'followup') && (
              <EmailEditor
                label="Instructions (optional)"
                value={instructions}
                onChange={setInstructions}
                minRows={2}
                maxLength={2000}
              />
            )}

            {tab === 'improve' && (
              <TextField select label="Style" value={style} onChange={(e) => setStyle(e.target.value)}>
                {REWRITE_STYLES.map((s) => (
                  <MenuItem key={s.value} value={s.value}>
                    {s.label}
                  </MenuItem>
                ))}
              </TextField>
            )}

            {tab === 'translate' && (
              <TextField
                label="Target Language"
                value={targetLanguage}
                onChange={(e) => setTargetLanguage(e.target.value)}
              />
            )}

            {tab === 'custom' && (
              <>
                <TextField select label="Email Type" value={customType} onChange={(e) => setCustomType(e.target.value)}>
                  {CUSTOM_GENERATOR_TYPES.map((s) => (
                    <MenuItem key={s.value} value={s.value}>
                      {s.label}
                    </MenuItem>
                  ))}
                </TextField>
                <EmailEditor
                  label="Custom Prompt (optional)"
                  value={customPrompt}
                  onChange={setCustomPrompt}
                  minRows={2}
                  maxLength={5000}
                />
              </>
            )}

            {(tab === 'generate' || tab === 'custom') && templates.length > 0 && (
              <TextField select label="Prompt Template (optional)" value={templateId} onChange={(e) => setTemplateId(e.target.value)}>
                <MenuItem value="">None</MenuItem>
                {templates.map((t) => (
                  <MenuItem key={t.id} value={t.id}>
                    {t.name}
                  </MenuItem>
                ))}
              </TextField>
            )}

            <Paper variant="outlined" sx={{ p: 2, bgcolor: 'action.hover' }}>
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
                  {uploading ? 'Extracting…' : 'Upload Reference File'}
                </Button>
                {uploadedFileName && (
                  <Chip size="small" label={uploadedFileName} onDelete={clearReferenceContext} />
                )}
              </Stack>
              {uploadedFileName ? (
                <Typography variant="caption" color="text.secondary">
                  The AI will use {referenceContext.length.toLocaleString()} characters from this file as background
                  reference — it will not be inserted into {contentLabel.toLowerCase()} above.
                </Typography>
              ) : (
                <Typography variant="caption" color="text.secondary">
                  Optional: attach a document (PDF, Word, plain text, etc.) for the AI to use as background
                  information — e.g. a price list, policy, or spec sheet. It won't replace the text above.
                </Typography>
              )}
            </Paper>

            <Button variant="contained" size="large" onClick={handleSubmit} disabled={loading || !content}>
              {loading ? 'Generating…' : 'Generate with AI'}
            </Button>
          </Stack>
        </Paper>

        <Box>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            AI Output
          </Typography>
          {loading && <Loader />}
          {!loading && !reply && (
            <Paper variant="outlined" sx={{ p: 3 }}>
              <Typography color="text.secondary">Your AI-generated email will appear here.</Typography>
            </Paper>
          )}
          {!loading && reply && (
            <ReplyCard
              reply={reply}
              onToggleFavorite={handleToggleFavorite}
              onRegenerate={handleRegenerate}
              regenerating={regenerating}
            />
          )}
        </Box>
      </Box>
    </Box>
  );
}
