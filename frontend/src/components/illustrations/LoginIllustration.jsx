// Original abstract composition (envelope + AI sparkle) — no external/stock imagery.
export default function LoginIllustration() {
  return (
    <svg viewBox="0 0 360 360" width="100%" height="100%" style={{ maxWidth: 320 }} role="presentation" aria-hidden="true">
      <circle cx="180" cy="180" r="150" fill="#14B8A6" opacity="0.12" />
      <circle cx="240" cy="110" r="60" fill="#4F46E5" opacity="0.1" />

      {/* Envelope */}
      <rect x="70" y="130" width="220" height="150" rx="18" fill="#ffffff" stroke="#4F46E5" strokeWidth="4" />
      <path d="M78 140 L180 220 L282 140" fill="none" stroke="#4F46E5" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />

      {/* Reply bubble */}
      <rect x="196" y="60" width="120" height="64" rx="16" fill="#4F46E5" />
      <circle cx="222" cy="92" r="5" fill="#ffffff" />
      <circle cx="246" cy="92" r="5" fill="#ffffff" />
      <circle cx="270" cy="92" r="5" fill="#ffffff" />

      {/* Sparkle */}
      <path
        d="M84 220 L90 236 L106 242 L90 248 L84 264 L78 248 L62 242 L78 236 Z"
        fill="#14B8A6"
      />

      {/* Floating dots */}
      <circle cx="300" cy="260" r="6" fill="#4F46E5" opacity="0.5" />
      <circle cx="60" cy="110" r="5" fill="#14B8A6" opacity="0.6" />
      <circle cx="320" cy="190" r="4" fill="#4F46E5" opacity="0.4" />
    </svg>
  );
}
