// Original abstract composition (card stack + plus/sparkle, echoing LoginIllustration's language
// for account creation) — no external/stock imagery.
export default function RegisterIllustration() {
  return (
    <svg viewBox="0 0 360 360" width="100%" height="100%" style={{ maxWidth: 320 }} role="presentation" aria-hidden="true">
      <circle cx="180" cy="180" r="150" fill="#4F46E5" opacity="0.1" />
      <circle cx="120" cy="250" r="60" fill="#14B8A6" opacity="0.12" />

      {/* Back card */}
      <rect x="100" y="110" width="180" height="120" rx="16" fill="#ffffff" stroke="#4F46E5" strokeWidth="3" opacity="0.5" transform="rotate(-6 190 170)" />
      {/* Front card */}
      <rect x="90" y="130" width="180" height="120" rx="16" fill="#ffffff" stroke="#4F46E5" strokeWidth="4" />
      <line x1="112" y1="160" x2="228" y2="160" stroke="#4F46E5" strokeWidth="4" strokeLinecap="round" opacity="0.6" />
      <line x1="112" y1="182" x2="248" y2="182" stroke="#4F46E5" strokeWidth="4" strokeLinecap="round" opacity="0.35" />
      <line x1="112" y1="204" x2="200" y2="204" stroke="#4F46E5" strokeWidth="4" strokeLinecap="round" opacity="0.35" />

      {/* Plus badge (new account) */}
      <circle cx="260" cy="250" r="30" fill="#14B8A6" />
      <line x1="260" y1="236" x2="260" y2="264" stroke="#ffffff" strokeWidth="5" strokeLinecap="round" />
      <line x1="246" y1="250" x2="274" y2="250" stroke="#ffffff" strokeWidth="5" strokeLinecap="round" />

      {/* Floating dots */}
      <circle cx="80" cy="100" r="5" fill="#4F46E5" opacity="0.5" />
      <circle cx="300" cy="140" r="4" fill="#14B8A6" opacity="0.5" />
      <circle cx="90" cy="290" r="6" fill="#4F46E5" opacity="0.4" />
    </svg>
  );
}
