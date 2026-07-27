import { useEffect, useRef, useState } from 'react';

const BAR_COUNT = 24;
const IDLE_LEVELS = new Array(BAR_COUNT).fill(2);

/**
 * Captures a *separate* raw microphone stream (via getUserMedia + Web Audio's
 * AnalyserNode) purely to drive a real-time waveform animation while
 * `active` is true. Kept independent of SpeechRecognition, which owns its
 * own internal audio capture and exposes no amplitude/frequency data at all.
 */
export default function useAudioWaveform(active) {
  const [levels, setLevels] = useState(IDLE_LEVELS);
  const [permissionError, setPermissionError] = useState(null);
  const animationFrameRef = useRef(null);

  useEffect(() => {
    if (!active) {
      setLevels(IDLE_LEVELS);
      return undefined;
    }

    let audioContext;
    let stream;
    let cancelled = false;

    const setup = async () => {
      try {
        stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        if (cancelled) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }

        const AudioContextApi = window.AudioContext || window.webkitAudioContext;
        audioContext = new AudioContextApi();
        const source = audioContext.createMediaStreamSource(stream);
        const analyser = audioContext.createAnalyser();
        analyser.fftSize = 128;
        source.connect(analyser);

        const frequencyData = new Uint8Array(analyser.frequencyBinCount);
        const samplesPerBar = Math.floor(frequencyData.length / BAR_COUNT) || 1;

        const tick = () => {
          analyser.getByteFrequencyData(frequencyData);
          const next = new Array(BAR_COUNT);
          for (let bar = 0; bar < BAR_COUNT; bar += 1) {
            let sum = 0;
            for (let sample = 0; sample < samplesPerBar; sample += 1) {
              sum += frequencyData[bar * samplesPerBar + sample];
            }
            const average = sum / samplesPerBar / 255;
            next[bar] = Math.max(2, Math.round(average * 32));
          }
          setLevels(next);
          animationFrameRef.current = requestAnimationFrame(tick);
        };
        tick();
      } catch (err) {
        setPermissionError(
          err?.name === 'NotAllowedError'
            ? 'Microphone access was denied, so the waveform display is unavailable.'
            : 'Could not access the microphone for the waveform display.',
        );
      }
    };

    setup();

    return () => {
      cancelled = true;
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
      if (audioContext) {
        audioContext.close().catch(() => {});
      }
      if (stream) {
        stream.getTracks().forEach((track) => track.stop());
      }
    };
  }, [active]);

  return { levels, permissionError };
}
