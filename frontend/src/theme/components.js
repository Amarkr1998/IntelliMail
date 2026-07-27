// Scoped component overrides — deliberately limited to components used on nearly every page
// (Paper/Card, Button, TextField, Chip, AppBar). Components with only one or two call sites
// (Accordion, Pagination) are styled inline at their call site instead of here, since a theme
// override with a single consumer is speculative work with no real payoff.
export function getComponentOverrides(mode) {
  const isDark = mode === 'dark';

  return {
    MuiCssBaseline: {
      styleOverrides: {
        // Safety net for the hand-rolled @keyframes in MicButton (framer-motion's own
        // useReducedMotion handling doesn't reach plain CSS animations).
        '@media (prefers-reduced-motion: reduce)': {
          '*, *::before, *::after': {
            animationDuration: '0.01ms !important',
            transitionDuration: '0.01ms !important',
            animationIterationCount: '1 !important',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: {
          borderColor: isDark ? 'rgba(255,255,255,0.09)' : 'rgba(15,23,42,0.08)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          transition: 'box-shadow 0.2s ease, transform 0.2s ease',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          transition: 'box-shadow 0.2s ease, transform 0.15s ease',
          '&:active': { transform: 'scale(0.98)' },
        },
        contained: {
          boxShadow: 'none',
          '&:hover': { boxShadow: '0 8px 20px -8px rgba(79,70,229,0.55)' },
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          transition: 'box-shadow 0.15s ease',
          '&.Mui-focused': {
            boxShadow: `0 0 0 3px ${isDark ? 'rgba(129,140,248,0.25)' : 'rgba(79,70,229,0.15)'}`,
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 8 },
      },
    },
    MuiLink: {
      styleOverrides: {
        root: {
          // MuiLink defaults to primary.main, which only reaches ~3:1 against the dark
          // background (fails the 4.5:1 text-contrast minimum) - swap to the lighter
          // dark-mode accent token so link text stays readable in both modes.
          color: isDark ? '#818CF8' : undefined,
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          transition: 'box-shadow 0.2s ease, background-color 0.2s ease',
        },
      },
    },
    MuiButtonBase: {
      defaultProps: {
        // A visible focus ring matters more once custom shapes/radii are introduced —
        // ripple alone isn't enough feedback for keyboard navigation.
        disableTouchRipple: false,
      },
      styleOverrides: {
        root: {
          '&.Mui-focusVisible': {
            outline: `2px solid ${isDark ? '#818CF8' : '#4F46E5'}`,
            outlineOffset: 2,
          },
        },
      },
    },
  };
}
