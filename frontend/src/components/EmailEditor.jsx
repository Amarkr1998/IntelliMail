import { TextField } from '@mui/material';

export default function EmailEditor({
  label,
  value,
  onChange,
  minRows = 6,
  maxLength = 20000,
  placeholder,
  error,
  helperText,
}) {
  return (
    <TextField
      label={label}
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      multiline
      minRows={minRows}
      fullWidth
      error={Boolean(error)}
      helperText={error || helperText || `${value?.length || 0}/${maxLength}`}
      slotProps={{ htmlInput: { maxLength } }}
    />
  );
}
