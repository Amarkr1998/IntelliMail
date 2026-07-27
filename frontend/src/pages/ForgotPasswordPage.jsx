import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { TextField, Button, Link, Alert, Collapse } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { forgotPassword } from '../api/authApi';
import AuthLayout from '../components/common/AuthLayout';
import ResetPasswordIllustration from '../components/illustrations/ResetPasswordIllustration';

export default function ForgotPasswordPage() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm();
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);

  const onSubmit = async (data) => {
    setServerError('');
    setSubmitting(true);
    try {
      await forgotPassword(data.email);
      setSent(true);
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Something went wrong. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      illustration={<ResetPasswordIllustration />}
      title="Forgot your password?"
      subtitle="Enter the email on your account and we'll send you a link to reset your password."
      footer={
        <>
          Remembered it? <Link component={RouterLink} to="/login">Back to log in</Link>
        </>
      }
    >
      <Collapse in={Boolean(serverError)}>
        <Alert severity="error" sx={{ mb: 2 }}>
          {serverError}
        </Alert>
      </Collapse>
      <Collapse in={sent}>
        <Alert severity="success" sx={{ mb: 2 }}>
          If that email is registered, a reset link is on its way. Check your inbox (and spam folder).
        </Alert>
      </Collapse>
      {!sent && (
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
          <Button type="submit" variant="contained" fullWidth size="large" sx={{ mt: 2 }} disabled={submitting}>
            {submitting ? 'Sending…' : 'Send reset link'}
          </Button>
        </form>
      )}
    </AuthLayout>
  );
}
