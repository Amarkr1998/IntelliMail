import { Box, Typography, Paper, Stack, Switch, FormControlLabel } from '@mui/material';
import { useColorMode } from '../theme/ThemeContext';

export default function SettingsPage() {
  const { mode, toggleColorMode } = useColorMode();

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Settings
      </Typography>
      <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
        <Stack spacing={2}>
          <FormControlLabel control={<Switch checked={mode === 'dark'} onChange={toggleColorMode} />} label="Dark mode" />
        </Stack>
      </Paper>
    </Box>
  );
}
