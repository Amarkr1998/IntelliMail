import { useState } from 'react';
import { IconButton, Tooltip, Stack, CircularProgress } from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DescriptionIcon from '@mui/icons-material/Description';
import PrintIcon from '@mui/icons-material/Print';
import ShareIcon from '@mui/icons-material/Share';
import * as agentApi from '../api/agentApi';
import { useSnackbar } from '../context/SnackbarContext';

function slugify(text) {
  const slug = (text || '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
    .slice(0, 60);
  return slug || 'agent-response';
}

function triggerBlobDownload(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/** Export actions for one AI Agent response: Copy, PDF, Markdown, Print, Share. */
export default function ExportMenu({ taskId, title, content, contentRef }) {
  const { showSnackbar } = useSnackbar();
  const [downloadingPdf, setDownloadingPdf] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(content || '');
    showSnackbar('Copied to clipboard', 'success');
  };

  const handleDownloadMarkdown = () => {
    const blob = new Blob([content || ''], { type: 'text/markdown;charset=utf-8' });
    triggerBlobDownload(blob, `${slugify(title)}.md`);
  };

  const handleDownloadPdf = async () => {
    if (!taskId) {
      return;
    }
    setDownloadingPdf(true);
    try {
      const blob = await agentApi.downloadPdf(taskId);
      triggerBlobDownload(blob, `${slugify(title)}.pdf`);
    } catch {
      showSnackbar('Could not generate PDF', 'error');
    } finally {
      setDownloadingPdf(false);
    }
  };

  const handlePrint = () => {
    const html = contentRef?.current?.innerHTML || '';
    const printWindow = window.open('', '_blank', 'width=800,height=1000');
    if (!printWindow) {
      showSnackbar('Allow pop-ups for this site to print', 'error');
      return;
    }
    printWindow.document.write(`<!doctype html>
      <html>
        <head>
          <title>${title}</title>
          <style>
            body { font-family: -apple-system, "Segoe UI", Roboto, sans-serif; padding: 40px; color: #1B1E2B; }
            .brand { color: #4338CA; font-weight: 700; font-size: 13px; letter-spacing: 0.02em; margin-bottom: 6px; }
            h1.title { font-size: 20px; margin: 0 0 24px; }
            table { border-collapse: collapse; width: 100%; margin: 10px 0; }
            th, td { border: 1px solid #DFE2ED; padding: 6px 10px; text-align: left; }
            code { background: #EDEFF7; padding: 1px 4px; border-radius: 3px; }
            pre { background: #12141C; color: #E4E6F2; padding: 12px; border-radius: 6px; overflow-x: auto; }
          </style>
        </head>
        <body>
          <div class="brand">IntelliMail</div>
          <h1 class="title">${title}</h1>
          ${html}
        </body>
      </html>`);
    printWindow.document.close();
    printWindow.focus();
    // onload doesn't reliably fire for document.write()'d content in every browser,
    // so trigger print either way shortly after - print() is a no-op if already shown.
    printWindow.onload = () => printWindow.print();
    setTimeout(() => printWindow.print(), 300);
  };

  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({ title, text: content || '' });
      } catch (err) {
        if (err?.name !== 'AbortError') {
          showSnackbar('Could not share', 'error');
        }
      }
      return;
    }
    await navigator.clipboard.writeText(content || '');
    showSnackbar('Sharing is not supported in this browser - copied to clipboard instead', 'info');
  };

  return (
    <Stack direction="row" spacing={0.5}>
      <Tooltip title="Copy">
        <IconButton size="small" onClick={handleCopy} aria-label="Copy response">
          <ContentCopyIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Download PDF">
        <span>
          <IconButton size="small" onClick={handleDownloadPdf} disabled={!taskId || downloadingPdf} aria-label="Download as PDF">
            {downloadingPdf ? <CircularProgress size={16} /> : <PictureAsPdfIcon fontSize="small" />}
          </IconButton>
        </span>
      </Tooltip>
      <Tooltip title="Download Markdown">
        <IconButton size="small" onClick={handleDownloadMarkdown} aria-label="Download as Markdown">
          <DescriptionIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Print">
        <IconButton size="small" onClick={handlePrint} aria-label="Print response">
          <PrintIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Share">
        <IconButton size="small" onClick={handleShare} aria-label="Share response">
          <ShareIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </Stack>
  );
}
