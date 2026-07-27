import { GoogleLogin } from '@react-oauth/google';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useSnackbar } from '../context/SnackbarContext';

export default function GoogleLoginButton() {
  const { loginWithGoogle } = useAuth();
  const { showSnackbar } = useSnackbar();
  const navigate = useNavigate();

  const handleSuccess = async (credentialResponse) => {
    try {
      await loginWithGoogle(credentialResponse.credential);
      navigate('/dashboard');
    } catch {
      showSnackbar('Google sign-in failed. Please try again.', 'error');
    }
  };

  return (
    <GoogleLogin
      onSuccess={handleSuccess}
      onError={() => showSnackbar('Google sign-in failed. Please try again.', 'error')}
      useOneTap={false}
      width="100%"
    />
  );
}
