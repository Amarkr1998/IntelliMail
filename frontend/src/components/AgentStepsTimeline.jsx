import { List, ListItem, ListItemIcon, ListItemText, Typography, Chip, Stack } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';

/** Renders the ordered trail of tools an agent task invoked - no equivalent chat/step-list pattern existed in the app before this. */
export default function AgentStepsTimeline({ steps }) {
  if (!steps || steps.length === 0) {
    return null;
  }

  return (
    <List dense sx={{ bgcolor: 'action.hover', borderRadius: 1 }}>
      {steps.map((step) => (
        <ListItem key={step.stepNumber} alignItems="flex-start">
          <ListItemIcon sx={{ minWidth: 36, mt: 0.5 }}>
            {step.status === 'SUCCESS' ? (
              <CheckCircleIcon color="success" fontSize="small" />
            ) : (
              <ErrorIcon color="error" fontSize="small" />
            )}
          </ListItemIcon>
          <ListItemText
            primary={
              <Stack direction="row" spacing={1} alignItems="center">
                <Chip size="small" label={`Step ${step.stepNumber}`} />
                <Typography variant="subtitle2">{step.toolName}</Typography>
              </Stack>
            }
            secondary={
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
                {step.outputSummary}
              </Typography>
            }
          />
        </ListItem>
      ))}
    </List>
  );
}
