// Explicit MD3-ish type scale — the previous theme set none of this, so every page manually
// re-declared fontWeight={700} on its h4 page title; centralizing it here removes that repetition.
const typography = {
  fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
  h1: { fontWeight: 700, letterSpacing: '-0.02em' },
  h2: { fontWeight: 700, letterSpacing: '-0.02em' },
  h3: { fontWeight: 700, letterSpacing: '-0.01em' },
  h4: { fontWeight: 700, fontSize: '1.75rem', letterSpacing: '-0.01em' },
  h5: { fontWeight: 700, fontSize: '1.375rem' },
  h6: { fontWeight: 600, fontSize: '1.125rem' },
  subtitle1: { fontWeight: 600 },
  subtitle2: { fontWeight: 600 },
  button: { textTransform: 'none', fontWeight: 600 },
  overline: { fontWeight: 700, letterSpacing: '0.08em' },
};

export default typography;
