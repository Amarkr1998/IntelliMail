import { useState } from 'react';
import { Box, Button, Alert, Link, Paper, Typography, Stack } from '@mui/material';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import * as organizationApi from '../api/organizationApi';
import { useAuth } from '../context/AuthContext';
import { useSnackbar } from '../context/SnackbarContext';
import PageHeader from '../components/common/PageHeader';

export default function AcceptInvitationPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const { user, refreshProfile } = useAuth();
  const { showSnackbar } = useSnackbar();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleAccept = async () => {
    setError('');
    setSubmitting(true);
    try {
      await organizationApi.acceptInvitation(token);
      await refreshProfile();
      showSnackbar('You joined the organization', 'success');
      navigate('/organization');
    } catch (err) {
      setError(err?.response?.data?.message || 'This invitation link is invalid or has expired.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <PageHeader title="Organization invitation" />
      <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
        {!token ? (
          <Alert severity="error">This invitation link is missing its token. Ask for a new invitation.</Alert>
        ) : error ? (
          <Alert severity="error">{error}</Alert>
        ) : !user ? (
          <Stack spacing={2}>
            <Typography>Log in or create an account to accept this invitation.</Typography>
            <Stack direction="row" spacing={2}>
              <Button component={RouterLink} to="/login" variant="contained">
                Log in
              </Button>
              <Button component={RouterLink} to="/register" variant="outlined">
                Register
              </Button>
            </Stack>
            <Typography variant="caption" color="text.secondary">
              Come back to this link after signing in to accept the invitation.
            </Typography>
          </Stack>
        ) : (
          <Stack spacing={2}>
            <Typography>You've been invited to join an organization on IntelliMail.</Typography>
            <Button variant="contained" onClick={handleAccept} disabled={submitting}>
              {submitting ? 'Joining…' : 'Accept invitation'}
            </Button>
          </Stack>
        )}
      </Paper>
      <Typography variant="body2" sx={{ mt: 2 }}>
        <Link component={RouterLink} to="/dashboard">
          Back to dashboard
        </Link>
      </Typography>
    </Box>
  );
}
