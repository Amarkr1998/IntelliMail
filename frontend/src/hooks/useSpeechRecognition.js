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
    try {
      recognitionRef.current.start();
      setListening(true);
    } catch {
      // start() throws InvalidStateError if a session is already starting/running; safe to ignore.
    }
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
