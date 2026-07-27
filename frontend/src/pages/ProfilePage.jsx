import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Box, Typography, Paper, TextField, Button, Chip, Stack, Avatar } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { useSnackbar } from '../context/SnackbarContext';
import * as userApi from '../api/userApi';
import PageHeader from '../components/common/PageHeader';

export default function ProfilePage() {
  const { user, refreshProfile } = useAuth();
  const { showSnackbar } = useSnackbar();
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting, errors },
  } = useForm({ defaultValues: { fullName: user?.fullName || '' } });

  useEffect(() => {
    reset({ fullName: user?.fullName || '' });
  }, [user, reset]);

  const onSubmit = async (data) => {
    try {
      await userApi.updateProfile(data);
      await refreshProfile();
      showSnackbar('Profile updated', 'success');
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not update profile', 'error');
    }
  };

  return (
    <Box>
      <PageHeader title="Profile" subtitle="Your account details and preferences." />
      <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
        <Stack spacing={2}>
          <Stack direction="row" spacing={2} alignItems="center">
            <Avatar
              sx={{
                width: 64,
                height: 64,
                fontSize: '1.5rem',
                fontWeight: 700,
                background: 'linear-gradient(135deg, #4F46E5 0%, #14B8A6 100%)',
              }}
            >
              {user?.fullName?.charAt(0)?.toUpperCase() || '?'}
            </Avatar>
            <Box>
              <Typography variant="subtitle1" fontWeight={700}>
                {user?.fullName}
              </Typography>
              <Stack direction="row" spacing={1} sx={{ mt: 0.5 }}>
                {user?.roles?.map((role) => (
                  <Chip key={role} size="small" label={role} />
                ))}
              </Stack>
            </Box>
          </Stack>
          <TextField label="Email" value={user?.email || ''} disabled fullWidth />
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField
              label="Full Name"
              fullWidth
              margin="normal"
              {...register('fullName', { required: 'Full name is required' })}
              error={Boolean(errors.fullName)}
              helperText={errors.fullName?.message}
            />
            <Button type="submit" variant="contained" disabled={isSubmitting} sx={{ mt: 1 }}>
              Save Changes
            </Button>
          </form>
        </Stack>
      </Paper>
    </Box>
  );
}
