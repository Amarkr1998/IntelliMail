// Primary/secondary are locked brand colors (match the shipped favicon/logo) — never change
// their `main` value. Accessibility-driven tokens live alongside them instead of replacing them:
// #14B8A6 fails WCAG AA as foreground text/icon color on white (~2.5:1), so `secondaryText` is
// the safe alternative for teal *text*, while `secondary.main` stays reserved for fills/chips/
// chart series where the stricter text-contrast bar doesn't apply. Likewise `#4F46E5` as text on
// the dark background only reaches ~3:1 (fails the 4.5:1 body-text minimum), so `accentText` is a
// lighter, dark-mode-only tone for links/inline icons.
const PRIMARY = '#4F46E5';
const SECONDARY = '#14B8A6';

export function getPalette(mode) {
  const isDark = mode === 'dark';

  return {
    mode,
    primary: { main: PRIMARY },
    secondary: {
      main: SECONDARY,
      // MUI's auto-picked contrastText for this mid-tone teal is white, which fails badly
      // (~1.9:1) on secondary-filled Chips — force a safe dark high-emphasis text instead.
      contrastText: 'rgba(0,0,0,0.87)',
    },
    background: isDark
      ? { default: '#0f1115', paper: '#161a20' }
      : { default: '#f5f6fa', paper: '#ffffff' },
    custom: {
      // Safe teal for text/icons (foreground use only) — ~4.6:1 on white.
      secondaryText: '#0F766E',
      // Safe indigo for links/inline icons on the dark background — ~4.6:1 on #0f1115.
      accentText: isDark ? '#818CF8' : PRIMARY,
      glass: isDark ? 'rgba(22,26,32,0.72)' : 'rgba(255,255,255,0.72)',
      glassBorder: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(79,70,229,0.08)',
      gradient: isDark
        ? 'linear-gradient(135deg, #1e1b4b 0%, #0f1115 60%)'
        : 'linear-gradient(135deg, #EEF2FF 0%, #F0FDFA 100%)',
    },
  };
}
