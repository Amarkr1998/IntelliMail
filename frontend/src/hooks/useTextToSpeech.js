import { useEffect, useRef, useState } from 'react';

const supported = typeof window !== 'undefined' && 'speechSynthesis' in window;

// SpeechSynthesisVoice has no gender field in the Web Speech API spec - only
// name/lang/default/localService/voiceURI - so picking a female voice has to
// be done by matching known female voice names across platforms/browsers.
const FEMALE_VOICE_HINTS = [
  'female',
  'zira',
  'samantha',
  'victoria',
  'karen',
  'moira',
  'tessa',
  'hazel',
  'susan',
  'fiona',
  'kate',
  'salli',
  'joanna',
  'ivy',
  'kendra',
  'kimberly',
  'google us english', // Chrome's default US English voice is female.
  'google uk english female',
];

function pickFemaleVoice(voices) {
  if (!voices.length) {
    return null;
  }
  const byHint = voices.find((voice) => FEMALE_VOICE_HINTS.some((hint) => voice.name.toLowerCase().includes(hint)));
  if (byHint) {
    return byHint;
  }
  return voices.find((voice) => voice.lang?.toLowerCase().startsWith('en')) || voices[0];
}

// Strips the most common Markdown markup so read-aloud doesn't speak literal
// asterisks/hashes/link brackets - reply/response content is Markdown, not
// plain text.
function stripMarkdownForSpeech(text) {
  return text
    .replace(/`{1,3}[^`]*`{1,3}/g, ' ')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/[*_#>~-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

// Resolved eagerly at module load, NOT lazily inside the click handler.
// speechSynthesis.getVoices() is empty on first call in Chrome until
// 'voiceschanged' fires - by module-load time (well before a user can
// actually click a speak button) this has almost always already resolved,
// so toggleSpeak below can stay fully synchronous. That matters: browsers
// that require speech synthesis to be triggered directly within a user
// gesture's call stack will silently swallow speak() calls made after an
// `await` or inside a callback - a real, previously-introduced regression
// this rewrite removes.
let femaleVoice = null;
function refreshVoiceCache() {
  const voices = window.speechSynthesis.getVoices();
  if (voices.length) {
    femaleVoice = pickFemaleVoice(voices);
  }
}
if (supported) {
  refreshVoiceCache();
  window.speechSynthesis.addEventListener('voiceschanged', refreshVoiceCache);
}

/** Wraps the browser's native SpeechSynthesis (Web Speech API) for read-aloud buttons. */
export default function useTextToSpeech() {
  const [speaking, setSpeaking] = useState(false);
  const keepAliveRef = useRef(null);

  const clearKeepAlive = () => {
    if (keepAliveRef.current) {
      clearInterval(keepAliveRef.current);
      keepAliveRef.current = null;
    }
  };

  // Stop speaking if the component using this hook unmounts (e.g. navigating
  // away, or a new reply replacing this one) - it must never keep talking
  // about content that's no longer on screen.
  useEffect(() => {
    return () => {
      clearKeepAlive();
      if (supported) {
        window.speechSynthesis.cancel();
      }
    };
  }, []);

  const toggleSpeak = (text) => {
    if (!supported) {
      return;
    }
    if (speaking) {
      clearKeepAlive();
      window.speechSynthesis.cancel();
      setSpeaking(false);
      return;
    }

    // Safe even when nothing is currently speaking (a no-op in that case) -
    // and calling it synchronously back-to-back with speak(), in the same
    // tick, is the most broadly compatible pattern for this API.
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(stripMarkdownForSpeech(text));
    if (femaleVoice) {
      utterance.voice = femaleVoice;
      utterance.lang = femaleVoice.lang;
    }
    utterance.onend = () => {
      clearKeepAlive();
      setSpeaking(false);
    };
    utterance.onerror = () => {
      clearKeepAlive();
      setSpeaking(false);
    };

    window.speechSynthesis.speak(utterance);
    setSpeaking(true);

    // Chrome has a long-standing bug where speechSynthesis silently pauses
    // itself partway through longer utterances (especially once the tab
    // loses focus) without ever firing onend - nudge it back if that
    // happens rather than leaving the button stuck showing "speaking".
    clearKeepAlive();
    keepAliveRef.current = setInterval(() => {
      if (window.speechSynthesis.paused) {
        window.speechSynthesis.resume();
      }
    }, 5000);
  };

  return { supported, speaking, toggleSpeak };
}
