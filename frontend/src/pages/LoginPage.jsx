import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Box, TextField, Button, Link, Alert, Collapse, Divider, Typography } from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import AuthLayout from '../components/common/AuthLayout';
import LoginIllustration from '../components/illustrations/LoginIllustration';
import GoogleLoginButton from '../components/GoogleLoginButton';

export default function LoginPage() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm();
  const { login } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      await login(data);
      navigate('/dashboard');
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      illustration={<LoginIllustration />}
      title="Welcome back"
      subtitle="Log in to IntelliMail to keep drafting smarter emails."
      footer={
        <>
          Don&apos;t have an account? <Link component={RouterLink} to="/register">Register</Link>
        </>
      }
    >
      <Collapse in={Boolean(serverError)}>
        <Alert severity="error" sx={{ mb: 2 }}>
          {serverError}
        </Alert>
      </Collapse>
      <Box sx={{ mb: 2 }}>
        <GoogleLoginButton />
      </Box>
      <Divider sx={{ mb: 2 }}>
        <Typography variant="caption" color="text.secondary">
          or continue with email
        </Typography>
      </Divider>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Email"
          type="email"
          fullWidth
          margin="normal"
          {...register('email', { required: 'Email is required' })}
          error={Boolean(errors.email)}
          helperText={errors.email?.message}
        />
        <TextField
          label="Password"
          type="password"
          fullWidth
          margin="normal"
          {...register('password', { required: 'Password is required' })}
          error={Boolean(errors.password)}
          helperText={errors.password?.message}
        />
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 0.5 }}>
          <Link component={RouterLink} to="/forgot-password" variant="body2">
            Forgot password?
          </Link>
        </Box>
        <Button type="submit" variant="contained" fullWidth size="large" sx={{ mt: 2 }} disabled={submitting}>
          {submitting ? 'Logging in…' : 'Log In'}
        </Button>
      </form>
    </AuthLayout>
  );
}
