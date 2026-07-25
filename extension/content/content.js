// Injected into mail.google.com. Gmail's DOM structure is undocumented and
// changes without notice, so this deliberately avoids trying to blend into
// Gmail's own toolbar (fragile, and easy to break Gmail's own UI). Instead it
// injects a self-contained floating button + panel that reads the open
// email's text and, on request, tries to insert the AI's reply into an open
// compose/reply box - falling back to copy-to-clipboard if it can't find one.
(function () {
  const FAB_ID = 'intellimail-fab';
  const PANEL_ID = 'intellimail-panel';

  function getOpenEmailContent() {
    // div.a3s.aiL is the message-body container class Gmail has used for its
    // rendered email HTML for years; it is not an official/documented API,
    // so this is the first thing to re-check if Gmail changes its markup.
    const messageNodes = document.querySelectorAll('div.a3s.aiL');
    if (messageNodes.length === 0) {
      return '';
    }
    const lastMessage = messageNodes[messageNodes.length - 1];
    return lastMessage.innerText.trim();
  }

  function findComposeBox() {
    const boxes = document.querySelectorAll('div[contenteditable="true"][role="textbox"]');
    for (const box of boxes) {
      const label = (box.getAttribute('aria-label') || '').toLowerCase();
      if (label.includes('message body') || label.includes('body')) {
        return box;
      }
    }
    return boxes.length > 0 ? boxes[boxes.length - 1] : null;
  }

  function insertReplyIntoCompose(text) {
    const box = findComposeBox();
    if (!box) {
      return false;
    }
    box.focus();
    document.execCommand('insertText', false, text);
    return true;
  }

  function sendMessage(message) {
    return new Promise((resolve) => {
      chrome.runtime.sendMessage(message, (response) => resolve(response));
    });
  }

  function setStatus(text) {
    const panel = document.getElementById(PANEL_ID);
    if (panel) {
      panel.querySelector('.intellimail-status').textContent = text;
    }
  }

  function buildPayload(action, emailContent) {
    switch (action) {
      case 'GENERATE_REPLY':
        return { originalContent: emailContent, instructions: null, promptTemplateId: null };
      case 'IMPROVE':
        return { content: emailContent, style: 'PROFESSIONAL_REWRITE' };
      case 'TRANSLATE':
        return { content: emailContent, targetLanguage: 'French' };
      case 'FOLLOWUP':
        return { originalContent: emailContent, instructions: null };
      case 'SUMMARIZE':
      case 'SUBJECT':
      default:
        return { content: emailContent };
    }
  }

  async function handleGenerate() {
    const panel = document.getElementById(PANEL_ID);
    const action = panel.querySelector('.intellimail-action').value;
    const output = panel.querySelector('.intellimail-output');
    const insertBtn = panel.querySelector('.intellimail-insert');
    const copyBtn = panel.querySelector('.intellimail-copy');

    const authState = await sendMessage({ type: 'GET_AUTH_STATE' });
    if (!authState?.data?.authenticated) {
      setStatus('Please log in via the IntelliMail extension icon first.');
      return;
    }

    const emailContent = getOpenEmailContent();
    if (!emailContent) {
      setStatus('Open an email in Gmail first.');
      return;
    }

    setStatus('Generating…');
    output.value = '';
    insertBtn.disabled = true;
    copyBtn.disabled = true;

    const response = await sendMessage({ type: action, payload: buildPayload(action, emailContent) });

    if (response?.success) {
      output.value = response.data.content;
      insertBtn.disabled = false;
      copyBtn.disabled = false;
      setStatus('Done.');
    } else {
      setStatus(response?.error || 'Something went wrong.');
    }
  }

  function createPanel() {
    const existing = document.getElementById(PANEL_ID);
    if (existing) {
      return existing;
    }

    const panel = document.createElement('div');
    panel.id = PANEL_ID;
    panel.className = 'intellimail-panel intellimail-hidden';
    panel.innerHTML = `
      <div class="intellimail-panel-header">
        <span>IntelliMail AI</span>
        <button class="intellimail-close" type="button" aria-label="Close">&times;</button>
      </div>
      <div class="intellimail-panel-body">
        <select class="intellimail-action">
          <option value="GENERATE_REPLY">Generate Reply</option>
          <option value="IMPROVE">Improve / Rewrite</option>
          <option value="TRANSLATE">Translate</option>
          <option value="SUMMARIZE">Summarize</option>
          <option value="SUBJECT">Subject Line</option>
          <option value="FOLLOWUP">Follow-up</option>
        </select>
        <textarea class="intellimail-output" placeholder="AI output will appear here..." readonly></textarea>
        <div class="intellimail-actions">
          <button class="intellimail-generate" type="button">Generate</button>
          <button class="intellimail-insert" type="button" disabled>Insert into Reply</button>
          <button class="intellimail-copy" type="button" disabled>Copy</button>
        </div>
        <div class="intellimail-status"></div>
      </div>
    `;
    document.body.appendChild(panel);

    panel.querySelector('.intellimail-close').addEventListener('click', () => {
      panel.classList.add('intellimail-hidden');
    });
    panel.querySelector('.intellimail-generate').addEventListener('click', handleGenerate);
    panel.querySelector('.intellimail-insert').addEventListener('click', () => {
      const text = panel.querySelector('.intellimail-output').value;
      if (text && insertReplyIntoCompose(text)) {
        setStatus('Inserted into reply.');
      } else {
        setStatus('Open a reply box in Gmail first, then try again.');
      }
    });
    panel.querySelector('.intellimail-copy').addEventListener('click', async () => {
      const text = panel.querySelector('.intellimail-output').value;
      if (text) {
        await navigator.clipboard.writeText(text);
        setStatus('Copied to clipboard.');
      }
    });

    return panel;
  }

  function createFab() {
    if (document.getElementById(FAB_ID)) {
      return;
    }
    const fab = document.createElement('button');
    fab.id = FAB_ID;
    fab.type = 'button';
    fab.className = 'intellimail-fab';
    fab.title = 'IntelliMail AI Assistant';
    fab.textContent = 'AI';
    fab.addEventListener('click', () => {
      const panel = createPanel();
      panel.classList.toggle('intellimail-hidden');
    });
    document.body.appendChild(fab);
  }

  // Gmail is a single-page app that swaps content without full page
  // navigations, and can occasionally re-render <body> itself; re-assert the
  // floating button whenever the DOM settles rather than relying on one
  // initial injection.
  const observer = new MutationObserver(() => {
    if (!document.getElementById(FAB_ID)) {
      createFab();
    }
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });

  createFab();
})();
