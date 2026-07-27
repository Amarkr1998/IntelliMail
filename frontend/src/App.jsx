import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { SnackbarProvider } from './context/SnackbarContext';
import { ColorModeProvider } from './theme/ThemeContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Loader from './components/Loader';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import AcceptInvitationPage from './pages/AcceptInvitationPage';
import ComposeAssistantPage from './pages/ComposeAssistantPage';
import VoiceAssistantPage from './pages/VoiceAssistantPage';
import HistoryPage from './pages/HistoryPage';
import TemplatesPage from './pages/TemplatesPage';
import SettingsPage from './pages/SettingsPage';
import ProfilePage from './pages/ProfilePage';
import CreateOrganizationPage from './pages/CreateOrganizationPage';
import OrganizationPage from './pages/OrganizationPage';
import BillingPage from './pages/BillingPage';

// Code-split the two pages that pull in @mui/x-charts, so the chart library
// isn't part of the initial app-shell bundle every route pays for.
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage'));

function LazyPage({ children }) {
  return <Suspense fallback={<Loader fullHeight />}>{children}</Suspense>;
}

export default function App() {
  return (
    <ColorModeProvider>
      <SnackbarProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/reset-password" element={<ResetPasswordPage />} />
              <Route path="/accept-invitation" element={<AcceptInvitationPage />} />

              <Route element={<ProtectedRoute />}>
                <Route element={<Layout />}>
                  <Route
                    path="/dashboard"
                    element={
                      <LazyPage>
                        <DashboardPage />
                      </LazyPage>
                    }
                  />
                  <Route path="/compose" element={<ComposeAssistantPage />} />
                  <Route path="/voice-ai" element={<VoiceAssistantPage />} />
                  <Route path="/history" element={<HistoryPage />} />
                  <Route path="/templates" element={<TemplatesPage />} />
                  <Route
                    path="/analytics"
                    element={
                      <LazyPage>
                        <AnalyticsPage />
                      </LazyPage>
                    }
                  />
                  <Route path="/settings" element={<SettingsPage />} />
                  <Route path="/profile" element={<ProfilePage />} />
                  <Route path="/create-organization" element={<CreateOrganizationPage />} />
                  <Route path="/organization" element={<OrganizationPage />} />
                  <Route path="/billing" element={<BillingPage />} />
                </Route>
              </Route>

              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </SnackbarProvider>
    </ColorModeProvider>
  );
}
