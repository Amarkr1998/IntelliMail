import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Box, Typography, Paper, TextField, Button, Chip, Stack } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { useSnackbar } from '../context/SnackbarContext';
import * as userApi from '../api/userApi';

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
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Profile
      </Typography>
      <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
        <Stack spacing={2}>
          <TextField label="Email" value={user?.email || ''} disabled fullWidth />
          <Stack direction="row" spacing={1}>
            {user?.roles?.map((role) => (
              <Chip key={role} size="small" label={role} />
            ))}
          </Stack>
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
