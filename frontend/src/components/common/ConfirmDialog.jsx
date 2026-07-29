import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button } from '@mui/material';

/** Generic confirm/cancel dialog - the first extracted one in the app (prior usages were ad hoc per-page). */
export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  confirming = false,
  onConfirm,
  onCancel,
}) {
  return (
    <Dialog open={open} onClose={onCancel} maxWidth="sm" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ whiteSpace: 'pre-wrap' }}>{description}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={confirming}>
          {cancelLabel}
        </Button>
        <Button variant="contained" onClick={onConfirm} disabled={confirming}>
          {confirming ? 'Working…' : confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
