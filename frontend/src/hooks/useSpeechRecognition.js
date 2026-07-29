import { useCallback, useEffect, useRef, useState } from 'react';

const SpeechRecognitionApi =
  typeof window !== 'undefined' ? window.SpeechRecognition || window.webkitSpeechRecognition : undefined;

// Maps the Web Speech API's error codes to user-facing guidance; 'aborted' is
// expected whenever the user (or our own cleanup) calls stop()/abort(), not a
// real failure, so it's deliberately left out.
const ERROR_MESSAGES = {
  'not-allowed': 'Microphone access was denied. Allow microphone permissions for this site and try again.',
  'service-not-allowed': 'Microphone access was denied. Allow microphone permissions for this site and try again.',
  'no-speech': 'No speech was detected. Try speaking closer to the microphone.',
  'audio-capture': 'No microphone was found. Connect a microphone and try again.',
  network: 'A network error interrupted speech recognition. Please try again.',
};

/**
 * Wraps the browser's native SpeechRecognition (Web Speech API) for
 * real-time speech-to-text. Not supported by every browser (notably
 * Firefox) — callers must check `supported` and offer a typing fallback.
 */
export default function useSpeechRecognition({ language = 'en-US' } = {}) {
  const supported = Boolean(SpeechRecognitionApi);
  const recognitionRef = useRef(null);
  const [listening, setListening] = useState(false);
  const [interimTranscript, setInterimTranscript] = useState('');
  const [finalTranscript, setFinalTranscript] = useState('');
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!supported) {
      return undefined;
    }

    const recognition = new SpeechRecognitionApi();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = language;

    recognition.onresult = (event) => {
      let interim = '';
      let finalChunk = '';
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i];
        if (result.isFinal) {
          finalChunk += result[0].transcript;
        } else {
          interim += result[0].transcript;
        }
      }
      if (finalChunk) {
        setFinalTranscript((prev) => (prev ? `${prev} ${finalChunk}`.trim() : finalChunk.trim()));
      }
      setInterimTranscript(interim);
    };

    recognition.onerror = (event) => {
      const message = ERROR_MESSAGES[event.error];
      if (message) {
        setError(message);
      }
      // no-speech keeps the session open (the browser just fires a warning);
      // every other error ends the session, so reflect that in `listening`.
      if (event.error !== 'no-speech') {
        setListening(false);
      }
    };

    recognition.onend = () => {
      setListening(false);
      setInterimTranscript('');
    };

    recognitionRef.current = recognition;

    return () => {
      recognition.onresult = null;
      recognition.onerror = null;
      recognition.onend = null;
      recognition.abort();
      recognitionRef.current = null;
    };
  }, [supported, language]);

  const start = useCallback(() => {
    if (!recognitionRef.current || listening) {
      return;
    }
    setError(null);
    setInterimTranscript('');

    const attemptStart = () => {
      try {
        recognitionRef.current.start();
        setListening(true);
        return true;
      } catch {
        return false;
      }
    };

    if (attemptStart()) {
      return;
    }

    // start() throws InvalidStateError when a previous session hasn't fully
    // torn down yet (its onend hasn't fired), which is exactly what made the
    // mic button silently "do nothing" on a quick stop-then-start - force an
    // abort and retry once shortly after, rather than swallowing the failure.
    try {
      recognitionRef.current.abort();
    } catch {
      // ignore - already stopped
    }
    setTimeout(() => {
      if (!attemptStart()) {
        setError('Could not start voice input. Please try again.');
      }
    }, 200);
  }, [listening]);

  const stop = useCallback(() => {
    recognitionRef.current?.stop();
  }, []);

  const resetTranscript = useCallback(() => {
    setFinalTranscript('');
    setInterimTranscript('');
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return {
    supported,
    listening,
    interimTranscript,
    finalTranscript,
    error,
    start,
    stop,
    resetTranscript,
    clearError,
  };
}
