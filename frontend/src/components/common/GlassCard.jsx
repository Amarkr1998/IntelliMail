import { Paper } from '@mui/material';
import { useColorMode } from '../../theme/ThemeContext';
import { getGlassSx } from '../../theme/glass';

/** A Paper with the translucent/blurred glass treatment — used sparingly (Navbar, Sidebar, AuthLayout). */
export default function GlassCard({ sx, children, ...rest }) {
  const { mode } = useColorMode();
  return (
    <Paper elevation={0} sx={{ ...getGlassSx(mode), ...sx }} {...rest}>
      {children}
    </Paper>
  );
}
