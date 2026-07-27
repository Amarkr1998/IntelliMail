import { useEffect, useState, useCallback } from 'react';
import { Box, Paper, Stack, Typography, Chip, Button, Grid } from '@mui/material';
import CreditCardIcon from '@mui/icons-material/CreditCard';
import * as billingApi from '../api/billingApi';
import { useSnackbar } from '../context/SnackbarContext';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import Loader from '../components/Loader';
import RequireOrgRole from '../components/RequireOrgRole';

const PLANS = [
  { id: 'STARTER', name: 'Starter', description: 'For small teams getting started with AI-assisted email.' },
  { id: 'PRO', name: 'Pro', description: 'Higher usage limits and priority support.' },
];

export default function BillingPage() {
  const { showSnackbar } = useSnackbar();
  const [subscription, setSubscription] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notInOrg, setNotInOrg] = useState(false);
  const [busyPlanId, setBusyPlanId] = useState(null);
  const [portalBusy, setPortalBusy] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    billingApi
      .getSubscription()
      .then(setSubscription)
      .catch((err) => {
        if (err?.response?.status === 404) {
          setNotInOrg(true);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleUpgrade = async (planId) => {
    setBusyPlanId(planId);
    try {
      const { checkoutUrl } = await billingApi.createCheckoutSession(planId);
      window.location.href = checkoutUrl;
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not start checkout', 'error');
      setBusyPlanId(null);
    }
  };

  const handleManageBilling = async () => {
    setPortalBusy(true);
    try {
      const { portalUrl } = await billingApi.createPortalSession();
      window.location.href = portalUrl;
    } catch (err) {
      showSnackbar(err?.response?.data?.message || 'Could not open the billing portal', 'error');
      setPortalBusy(false);
    }
  };

  if (loading) {
    return <Loader fullHeight />;
  }

  if (notInOrg) {
    return (
      <EmptyState
        icon={<CreditCardIcon />}
        title="No organization yet"
        description="Billing applies to organizations. Create one from Settings to start a subscription."
      />
    );
  }

  const trialActive = subscription?.status === 'TRIALING' && subscription.active;
  const trialDaysLeft = subscription?.trialEndsAt
    ? Math.max(0, Math.ceil((new Date(subscription.trialEndsAt) - Date.now()) / (1000 * 60 * 60 * 24)))
    : null;

  return (
    <Box>
      <PageHeader title="Billing" subtitle="Manage your organization's subscription plan." />

      <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={2}>
          <Box>
            <Typography variant="subtitle1">Current plan: {subscription?.planId}</Typography>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
              <Chip
                size="small"
                label={subscription?.status}
                color={subscription?.active ? 'success' : 'default'}
              />
              {trialActive && (
                <Typography variant="body2" color="text.secondary">
                  {trialDaysLeft} day{trialDaysLeft === 1 ? '' : 's'} left in your trial
                </Typography>
              )}
              {!subscription?.active && (
                <Typography variant="body2" color="error">
                  AI features are paused until you upgrade
                </Typography>
              )}
            </Stack>
          </Box>
          <RequireOrgRole roles={['OWNER']}>
            <Button variant="outlined" onClick={handleManageBilling} disabled={portalBusy}>
              {portalBusy ? 'Opening…' : 'Manage billing'}
            </Button>
          </RequireOrgRole>
        </Stack>
      </Paper>

      <RequireOrgRole roles={['OWNER']}>
        <Grid container spacing={2}>
          {PLANS.map((plan) => (
            <Grid key={plan.id} item xs={12} sm={6}>
              <Paper variant="outlined" sx={{ p: 3, height: '100%' }}>
                <Typography variant="h6" gutterBottom>
                  {plan.name}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  {plan.description}
                </Typography>
                <Button
                  variant="contained"
                  fullWidth
                  onClick={() => handleUpgrade(plan.id)}
                  disabled={busyPlanId === plan.id || subscription?.planId === plan.id}
                >
                  {subscription?.planId === plan.id
                    ? 'Current plan'
                    : busyPlanId === plan.id
                      ? 'Redirecting…'
                      : `Upgrade to ${plan.name}`}
                </Button>
              </Paper>
            </Grid>
          ))}
        </Grid>
      </RequireOrgRole>
    </Box>
  );
}
