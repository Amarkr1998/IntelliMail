import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Box, Paper, TextField, Button, Alert, Collapse, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import * as organizationApi from '../api/organizationApi';
import { useAuth } from '../context/AuthContext';
import { useSnackbar } from '../context/SnackbarContext';
import PageHeader from '../components/common/PageHeader';

function slugify(name) {
  return name
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-');
}

export default function CreateOrganizationPage() {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm();
  const { refreshProfile } = useAuth();
  const { showSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [slugEdited, setSlugEdited] = useState(false);

  const name = watch('name');

  const onNameChange = (event) => {
    const value = event.target.value;
    setValue('name', value);
    if (!slugEdited) {
      setValue('slug', slugify(value));
    }
  };

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      await organizationApi.createOrganization(data.name, data.slug);
      await refreshProfile();
      showSnackbar('Organization created', 'success');
      navigate('/organization');
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Could not create the organization. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <PageHeader title="Create an organization" subtitle="Invite teammates, share templates, and manage billing together." />
      <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
        <Collapse in={Boolean(serverError)}>
          <Alert severity="error" sx={{ mb: 2 }}>
            {serverError}
          </Alert>
        </Collapse>
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <TextField
            label="Organization name"
            fullWidth
            margin="normal"
            {...register('name', { required: 'Name is required' })}
            onChange={onNameChange}
            value={name || ''}
            error={Boolean(errors.name)}
            helperText={errors.name?.message}
          />
          <TextField
            label="Slug"
            fullWidth
            margin="normal"
            {...register('slug', {
              required: 'Slug is required',
              pattern: { value: /^[a-z0-9]+(-[a-z0-9]+)*$/, message: 'Lowercase letters, numbers, and hyphens only' },
              onChange: () => setSlugEdited(true),
            })}
            error={Boolean(errors.slug)}
            helperText={errors.slug?.message || 'Used in URLs - lowercase letters, numbers, and hyphens only'}
          />
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
            This is optional - IntelliMail works great solo too. Creating an organization is only needed if you want
            to invite teammates or set up billing.
          </Typography>
          <Button type="submit" variant="contained" fullWidth size="large" sx={{ mt: 1 }} disabled={submitting}>
            {submitting ? 'Creating…' : 'Create organization'}
          </Button>
        </form>
      </Paper>
    </Box>
  );
}
