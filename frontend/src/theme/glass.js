// A translucent, blurred-surface treatment used sparingly (Navbar, Sidebar, AuthLayout card) —
// deliberately not applied to dense data pages (Analytics/History), which need legibility over
// decoration. backdrop-filter has a real perf/compat cost, so keep usage to a handful of surfaces.
export function getGlassSx(mode) {
  const isDark = mode === 'dark';
  return {
    backgroundColor: isDark ? 'rgba(22,26,32,0.72)' : 'rgba(255,255,255,0.72)',
    backdropFilter: 'blur(16px) saturate(150%)',
    WebkitBackdropFilter: 'blur(16px) saturate(150%)',
    border: '1px solid',
    borderColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(79,70,229,0.08)',
  };
}
