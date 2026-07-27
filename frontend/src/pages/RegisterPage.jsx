import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Box, TextField, Button, Link, Alert, Collapse, Divider, Typography } from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import AuthLayout from '../components/common/AuthLayout';
import RegisterIllustration from '../components/illustrations/RegisterIllustration';
import GoogleLoginButton from '../components/GoogleLoginButton';

export default function RegisterPage() {
  const {
    register: registerField,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm();
  const { register } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const password = watch('password');

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      await register({ fullName: data.fullName, email: data.email, password: data.password });
      navigate('/dashboard');
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      illustration={<RegisterIllustration />}
      title="Create your account"
      subtitle="Start writing better emails with AI in minutes."
      footer={
        <>
          Already have an account? <Link component={RouterLink} to="/login">Log in</Link>
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
          label="Full Name"
          fullWidth
          margin="normal"
          {...registerField('fullName', { required: 'Full name is required' })}
          error={Boolean(errors.fullName)}
          helperText={errors.fullName?.message}
        />
        <TextField
          label="Email"
          type="email"
          fullWidth
          margin="normal"
          {...registerField('email', { required: 'Email is required' })}
          error={Boolean(errors.email)}
          helperText={errors.email?.message}
        />
        <TextField
          label="Password"
          type="password"
          fullWidth
          margin="normal"
          {...registerField('password', {
            required: 'Password is required',
            minLength: { value: 8, message: 'Password must be at least 8 characters' },
          })}
          error={Boolean(errors.password)}
          helperText={errors.password?.message}
        />
        <TextField
          label="Confirm Password"
          type="password"
          fullWidth
          margin="normal"
          {...registerField('confirmPassword', {
            required: 'Please confirm your password',
            validate: (value) => value === password || 'Passwords do not match',
          })}
          error={Boolean(errors.confirmPassword)}
          helperText={errors.confirmPassword?.message}
        />
        <Button type="submit" variant="contained" fullWidth size="large" sx={{ mt: 2 }} disabled={submitting}>
          {submitting ? 'Creating account…' : 'Register'}
        </Button>
      </form>
    </AuthLayout>
  );
}
