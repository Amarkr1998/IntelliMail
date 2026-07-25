import { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Typography,
  Button,
  Paper,
  List,
  ListItem,
  ListItemText,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Chip,
  Stack,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import * as templateApi from '../api/templateApi';
import Loader from '../components/Loader';
import { useSnackbar } from '../context/SnackbarContext';
import { REQUEST_TYPE_LABELS } from '../utils/requestTypes';

const emptyForm = {
  id: null,
  name: '',
  description: '',
  category: 'CUSTOM_PROMPT',
  promptText: '',
  systemPrompt: '',
  isPublic: false,
};

export default function TemplatesPage() {
  const { showSnackbar } = useSnackbar();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    templateApi
      .getTemplates(0, 50)
      .then(setData)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEdit = (template) => {
    setForm(template);
    setDialogOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    const payload = {
      name: form.name,
      description: form.description,
      category: form.category,
      promptText: form.promptText,
      systemPrompt: form.systemPrompt || null,
      isPublic: form.isPublic,
    };
    try {
      if (form.id) {
        await templateApi.updateTemplate(form.id, payload);
        showSnackbar('Template updated', 'success');
      } else {
        await templateApi.createTemplate(payload);
        showSnackbar('Template created', 'success');
      }
      setDialogOpen(false);
      load();
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not save template', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      await templateApi.deleteTemplate(id);
      showSnackbar('Template deleted', 'success');
      load();
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not delete template', 'error');
    }
  };

  if (loading && !data) {
    return <Loader fullHeight />;
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h4" fontWeight={700}>
          Prompt Templates
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          New Template
        </Button>
      </Stack>

      <Paper variant="outlined">
        <List disablePadding>
          {data?.content.map((template) => (
            <ListItem
              key={template.id}
              divider
              secondaryAction={
                <>
                  <IconButton onClick={() => openEdit(template)} aria-label="Edit template">
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton onClick={() => handleDelete(template.id)} aria-label="Delete template">
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </>
              }
            >
              <ListItemText
                primary={
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Typography fontWeight={600}>{template.name}</Typography>
                    <Chip size="small" label={REQUEST_TYPE_LABELS[template.category] || template.category} />
                    {template.isPublic && <Chip size="small" color="secondary" label="Public" />}
                  </Stack>
                }
                secondary={template.description}
              />
            </ListItem>
          ))}
          {data?.content.length === 0 && (
            <Box sx={{ p: 3 }}>
              <Typography color="text.secondary">
                No templates yet — create one to reuse your favorite prompts.
              </Typography>
            </Box>
          )}
        </List>
      </Paper>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{form.id ? 'Edit Template' : 'New Template'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} fullWidth />
            <TextField
              label="Description"
              value={form.description || ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              fullWidth
            />
            <TextField select label="Category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              {Object.entries(REQUEST_TYPE_LABELS).map(([value, label]) => (
                <MenuItem key={value} value={value}>
                  {label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Prompt Text"
              value={form.promptText}
              onChange={(e) => setForm({ ...form, promptText: e.target.value })}
              multiline
              minRows={3}
              fullWidth
            />
            <TextField
              label="System Prompt Override (optional)"
              value={form.systemPrompt || ''}
              onChange={(e) => setForm({ ...form, systemPrompt: e.target.value })}
              multiline
              minRows={2}
              fullWidth
            />
            <TextField
              select
              label="Visibility"
              value={form.isPublic ? 'public' : 'private'}
              onChange={(e) => setForm({ ...form, isPublic: e.target.value === 'public' })}
            >
              <MenuItem value="private">Private</MenuItem>
              <MenuItem value="public">Public</MenuItem>
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving || !form.name || !form.promptText}>
            {saving ? 'Saving…' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
