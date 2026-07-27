// Original abstract composition (padlock + keyhole + confirmation badge) — no
// external/stock imagery, same brand palette and style as LoginIllustration.
export default function ResetPasswordIllustration() {
  return (
    <svg viewBox="0 0 360 360" width="100%" height="100%" style={{ maxWidth: 320 }} role="presentation" aria-hidden="true">
      <circle cx="180" cy="180" r="150" fill="#4F46E5" opacity="0.1" />
      <circle cx="120" cy="250" r="60" fill="#14B8A6" opacity="0.12" />

      {/* Lock shackle */}
      <path
        d="M140 168 V132 a40 40 0 0 1 80 0 v36"
        fill="none"
        stroke="#4F46E5"
        strokeWidth="10"
        strokeLinecap="round"
      />

      {/* Lock body */}
      <rect x="110" y="160" width="140" height="120" rx="20" fill="#ffffff" stroke="#4F46E5" strokeWidth="4" />

      {/* Keyhole */}
      <circle cx="180" cy="205" r="14" fill="#4F46E5" />
      <path d="M172 216 L188 216 L182 244 L178 244 Z" fill="#4F46E5" />

      {/* Confirmation badge */}
      <circle cx="258" cy="252" r="30" fill="#14B8A6" />
      <path d="M244 252 L253 261 L273 240" fill="none" stroke="#ffffff" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" />

      {/* Floating dots */}
      <circle cx="90" cy="120" r="5" fill="#4F46E5" opacity="0.5" />
      <circle cx="300" cy="150" r="4" fill="#14B8A6" opacity="0.5" />
      <circle cx="100" cy="300" r="6" fill="#4F46E5" opacity="0.4" />
    </svg>
  );
}
