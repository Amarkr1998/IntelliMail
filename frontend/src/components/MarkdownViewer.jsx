import ReactMarkdown from 'react-markdown';
import { Box } from '@mui/material';

export default function MarkdownViewer({ content }) {
  return (
    <Box
      sx={{
        '& p': { m: 0, mb: 1 },
        '& p:last-of-type': { mb: 0 },
        whiteSpace: 'pre-wrap',
        fontSize: '0.95rem',
        lineHeight: 1.6,
      }}
    >
      <ReactMarkdown>{content}</ReactMarkdown>
    </Box>
  );
}
