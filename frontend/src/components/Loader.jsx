import { Box, CircularProgress } from '@mui/material';

export default function Loader({ size = 40, fullHeight = false }) {
  return (
    <Box
      display="flex"
      alignItems="center"
      justifyContent="center"
      sx={{ width: '100%', height: fullHeight ? '60vh' : 'auto', py: fullHeight ? 0 : 4 }}
    >
      <CircularProgress size={size} />
    </Box>
  );
}
