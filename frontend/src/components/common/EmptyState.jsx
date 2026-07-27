import { Box, Paper, Typography } from '@mui/material';

/** Consistent "nothing here yet" block, replacing the near-identical plain-text empty states
 * that were previously duplicated across Dashboard, Analytics, History, Templates, and Voice AI. */
export default function EmptyState({ icon, title, description, action }) {
  return (
    <Paper variant="outlined" sx={{ p: 5, textAlign: 'center' }}>
      {icon && (
        <Box
          sx={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 56,
            height: 56,
            borderRadius: '50%',
            bgcolor: 'action.hover',
            color: 'text.secondary',
            mb: 2,
          }}
        >
          {icon}
        </Box>
      )}
      <Typography variant="subtitle1" gutterBottom={Boolean(description)}>
        {title}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 360, mx: 'auto' }}>
          {description}
        </Typography>
      )}
      {action && <Box sx={{ mt: 2 }}>{action}</Box>}
    </Paper>
  );
}
