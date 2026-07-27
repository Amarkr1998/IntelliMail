import { useEffect, useState, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import {
  Box,
  Paper,
  Stack,
  Typography,
  Chip,
  IconButton,
  Pagination,
  TextField,
  MenuItem,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import GroupIcon from '@mui/icons-material/Group';
import DeleteIcon from '@mui/icons-material/Delete';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import * as organizationApi from '../api/organizationApi';
import { useAuth } from '../context/AuthContext';
import { useSnackbar } from '../context/SnackbarContext';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import Loader from '../components/Loader';
import RequireOrgRole from '../components/RequireOrgRole';

const ORG_ROLES = ['MEMBER', 'ADMIN'];

function InviteMemberDialog({ open, onClose, onInvited }) {
  const { register, handleSubmit, reset, formState: { errors } } = useForm({ defaultValues: { orgRole: 'MEMBER' } });
  const { showSnackbar } = useSnackbar();
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (data) => {
    setSubmitting(true);
    try {
      await organizationApi.inviteMember(data.email, data.orgRole);
      showSnackbar('Invitation sent', 'success');
      reset();
      onInvited();
      onClose();
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not send invitation', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogTitle>Invite a teammate</DialogTitle>
        <DialogContent>
          <TextField
            label="Email"
            type="email"
            fullWidth
            margin="normal"
            {...register('email', { required: 'Email is required' })}
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
          />
          <TextField label="Role" select fullWidth margin="normal" {...register('orgRole')}>
            {ORG_ROLES.map((role) => (
              <MenuItem key={role} value={role}>
                {role}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={submitting}>
            {submitting ? 'Sending…' : 'Send invitation'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

export default function OrganizationPage() {
  const { user } = useAuth();
  const { showSnackbar } = useSnackbar();
  const [organization, setOrganization] = useState(null);
  const [members, setMembers] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [inviteOpen, setInviteOpen] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([organizationApi.getMyOrganization(), organizationApi.getMembers(page, 20)])
      .then(([org, memberPage]) => {
        setOrganization(org);
        setMembers(memberPage);
      })
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const handleRemove = async (memberId) => {
    try {
      await organizationApi.removeMember(memberId);
      showSnackbar('Member removed', 'success');
      load();
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not remove member', 'error');
    }
  };

  if (loading && !organization) {
    return <Loader fullHeight />;
  }

  if (!organization) {
    return (
      <EmptyState
        icon={<GroupIcon />}
        title="No organization yet"
        description="Create an organization from Settings to invite teammates and manage billing together."
      />
    );
  }

  return (
    <Box>
      <PageHeader
        title={organization.name}
        subtitle={`/${organization.slug}`}
        action={
          <RequireOrgRole roles={['OWNER', 'ADMIN']}>
            <Button variant="contained" startIcon={<PersonAddIcon />} onClick={() => setInviteOpen(true)}>
              Invite member
            </Button>
          </RequireOrgRole>
        }
      />

      <Paper variant="outlined">
        {members?.content.map((member) => (
          <Stack
            key={member.id}
            direction="row"
            alignItems="center"
            justifyContent="space-between"
            sx={{ p: 2, borderBottom: 1, borderColor: 'divider', '&:last-child': { borderBottom: 0 } }}
          >
            <Box>
              <Typography variant="subtitle2">{member.fullName}</Typography>
              <Typography variant="body2" color="text.secondary">
                {member.email}
              </Typography>
            </Box>
            <Stack direction="row" spacing={1} alignItems="center">
              <Chip size="small" label={member.orgRole} />
              <RequireOrgRole roles={['OWNER', 'ADMIN']}>
                {member.id === user?.id ? null : (
                  <IconButton size="small" aria-label="Remove member" onClick={() => handleRemove(member.id)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                )}
              </RequireOrgRole>
            </Stack>
          </Stack>
        ))}
      </Paper>

      {members && members.totalPages > 1 && (
        <Stack alignItems="center" sx={{ mt: 2 }}>
          <Pagination count={members.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
        </Stack>
      )}

      <InviteMemberDialog open={inviteOpen} onClose={() => setInviteOpen(false)} onInvited={load} />
    </Box>
  );
}
