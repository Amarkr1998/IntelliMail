import { Box, Paper, Stack, Switch, FormControlLabel, Typography, Divider } from '@mui/material';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import { useColorMode } from '../theme/ThemeContext';
import PageHeader from '../components/common/PageHeader';

export default function SettingsPage() {
  const { mode, toggleColorMode } = useColorMode();

  return (
    <Box>
      <PageHeader title="Settings" subtitle="Manage how IntelliMail looks and feels." />
      <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 40,
              height: 40,
              borderRadius: '30%',
              bgcolor: 'action.hover',
              color: 'primary.main',
            }}
          >
            <DarkModeIcon />
          </Box>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="subtitle2">Appearance</Typography>
            <Typography variant="caption" color="text.secondary">
              Switch between light and dark mode
            </Typography>
          </Box>
        </Stack>
        <Divider sx={{ my: 2 }} />
        <FormControlLabel
          control={<Switch checked={mode === 'dark'} onChange={toggleColorMode} />}
          label="Dark mode"
        />
      </Paper>
    </Box>
  );
}
