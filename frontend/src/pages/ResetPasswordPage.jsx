import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { TextField, Button, Link, Alert, Collapse } from '@mui/material';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../api/authApi';
import { useSnackbar } from '../context/SnackbarContext';
import AuthLayout from '../components/common/AuthLayout';
import ResetPasswordIllustration from '../components/illustrations/ResetPasswordIllustration';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm();
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const password = watch('password');

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      await resetPassword(token, data.password);
      showSnackbar('Password reset successfully. Please log in.', 'success');
      navigate('/login');
    } catch (err) {
      setServerError(err?.response?.data?.message || 'This reset link is invalid or has expired.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      illustration={<ResetPasswordIllustration />}
      title="Set a new password"
      subtitle="Choose a new password for your account."
      footer={
        <>
          Remembered your password? <Link component={RouterLink} to="/login">Back to log in</Link>
        </>
      }
    >
      {!token ? (
        <Alert severity="error" sx={{ mb: 2 }}>
          This reset link is missing its token. Please request a new one.{' '}
          <Link component={RouterLink} to="/forgot-password">Request a new link</Link>
        </Alert>
      ) : (
        <>
          <Collapse in={Boolean(serverError)}>
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          </Collapse>
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField
              label="New Password"
              type="password"
              fullWidth
              margin="normal"
              {...register('password', {
                required: 'Password is required',
                minLength: { value: 8, message: 'Password must be at least 8 characters' },
              })}
              error={Boolean(errors.password)}
              helperText={errors.password?.message}
            />
            <TextField
              label="Confirm New Password"
              type="password"
              fullWidth
              margin="normal"
              {...register('confirmPassword', {
                required: 'Please confirm your password',
                validate: (value) => value === password || 'Passwords do not match',
              })}
              error={Boolean(errors.confirmPassword)}
              helperText={errors.confirmPassword?.message}
            />
            <Button type="submit" variant="contained" fullWidth size="large" sx={{ mt: 2 }} disabled={submitting}>
              {submitting ? 'Resetting…' : 'Reset password'}
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  );
}
