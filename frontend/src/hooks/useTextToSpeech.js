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
// so speak() below can stay fully synchronous. That matters: browsers
// that require speech synthesis to be triggered directly within a user
// gesture's call stack will silently swallow speak() calls made after an
// `await` or inside a callback.
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

/**
 * Wraps the browser's native SpeechSynthesis (Web Speech API) for read-aloud
 * buttons. `toggleSpeak(text)` is the simple play/stop toggle every existing
 * caller (ReplyCard, VoiceResponseCard) already uses; `speak`/`pause`/
 * `resume`/`stop` plus `rate`/`volume` options are for callers that want
 * fuller playback controls (the AI Agent page).
 */
export default function useTextToSpeech() {
  const [speaking, setSpeaking] = useState(false);
  const [paused, setPaused] = useState(false);
  const keepAliveRef = useRef(null);
  // Distinguishes "paused because the user asked to" from "paused because of
  // Chrome's long-utterance bug" - the keep-alive must never auto-resume the
  // former, only the latter.
  const userPausedRef = useRef(false);

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

  const stop = () => {
    if (!supported) {
      return;
    }
    clearKeepAlive();
    userPausedRef.current = false;
    window.speechSynthesis.cancel();
    setSpeaking(false);
    setPaused(false);
  };

  const speak = (text, { rate = 1, volume = 1 } = {}) => {
    if (!supported) {
      return;
    }

    // Safe even when nothing is currently speaking (a no-op in that case) -
    // and calling it synchronously back-to-back with speak(), in the same
    // tick, is the most broadly compatible pattern for this API.
    window.speechSynthesis.cancel();
    userPausedRef.current = false;

    const utterance = new SpeechSynthesisUtterance(stripMarkdownForSpeech(text));
    if (femaleVoice) {
      utterance.voice = femaleVoice;
      utterance.lang = femaleVoice.lang;
    }
    utterance.rate = rate;
    utterance.volume = volume;
    utterance.onend = () => {
      clearKeepAlive();
      setSpeaking(false);
      setPaused(false);
    };
    utterance.onerror = () => {
      clearKeepAlive();
      setSpeaking(false);
      setPaused(false);
    };

    window.speechSynthesis.speak(utterance);
    setSpeaking(true);
    setPaused(false);

    // Chrome has a long-standing bug where speechSynthesis silently pauses
    // itself partway through longer utterances (especially once the tab
    // loses focus) without ever firing onend - nudge it back if that
    // happens, but only when the user didn't intentionally pause.
    clearKeepAlive();
    keepAliveRef.current = setInterval(() => {
      if (window.speechSynthesis.paused && !userPausedRef.current) {
        window.speechSynthesis.resume();
      }
    }, 5000);
  };

  const pause = () => {
    if (!supported || !speaking) {
      return;
    }
    userPausedRef.current = true;
    window.speechSynthesis.pause();
    setPaused(true);
  };

  const resume = () => {
    if (!supported || !speaking) {
      return;
    }
    userPausedRef.current = false;
    window.speechSynthesis.resume();
    setPaused(false);
  };

  const toggleSpeak = (text, options) => {
    if (!supported) {
      return;
    }
    if (speaking) {
      stop();
      return;
    }
    speak(text, options);
  };

  return { supported, speaking, paused, speak, toggleSpeak, pause, resume, stop };
}
