import { useEffect, useState } from 'react';
import { Box, Paper, Tabs, Tab, Button, MenuItem, TextField, Typography, Stack } from '@mui/material';
import EmailEditor from '../components/EmailEditor';
import ReplyCard from '../components/ReplyCard';
import Loader from '../components/Loader';
import * as emailApi from '../api/emailApi';
import * as historyApi from '../api/historyApi';
import * as templateApi from '../api/templateApi';
import { useSnackbar } from '../context/SnackbarContext';
import { REWRITE_STYLES, CUSTOM_GENERATOR_TYPES } from '../utils/requestTypes';

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
          });
          break;
        case 'improve':
          result = await emailApi.improveEmail({ content, style });
          break;
        case 'translate':
          result = await emailApi.translateEmail({ content, targetLanguage });
          break;
        case 'summarize':
          result = await emailApi.summarizeEmail({ content });
          break;
        case 'subject':
          result = await emailApi.generateSubject({ content });
          break;
        case 'followup':
          result = await emailApi.generateFollowup({ originalContent: content, instructions: instructions || null });
          break;
        case 'custom':
          result = await emailApi.generateCustom({
            requestType: customType,
            context: content,
            customPrompt: customPrompt || null,
            promptTemplateId: templateId || null,
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
