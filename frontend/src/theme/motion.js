import { useReducedMotion } from 'framer-motion';

// Shared easing/duration + variant objects so every page pulls one canonical set of transitions
// instead of redefining them inline per component.
export const EASE = { standard: [0.2, 0, 0, 1] };
export const DURATION = { short: 0.15, medium: 0.25, long: 0.4 };

export const fadeInUp = {
  initial: { opacity: 0, y: 12 },
  animate: { opacity: 1, y: 0, transition: { duration: DURATION.long, ease: EASE.standard } },
  exit: { opacity: 0, y: -8, transition: { duration: DURATION.short } },
};

export const fadeIn = {
  initial: { opacity: 0 },
  animate: { opacity: 1, transition: { duration: DURATION.medium } },
  exit: { opacity: 0, transition: { duration: DURATION.short } },
};

export const scaleIn = {
  initial: { opacity: 0, scale: 0.96 },
  animate: { opacity: 1, scale: 1, transition: { duration: DURATION.medium, ease: EASE.standard } },
  exit: { opacity: 0, scale: 0.96, transition: { duration: DURATION.short } },
};

export const staggerContainer = {
  animate: { transition: { staggerChildren: 0.06 } },
};

export const hoverLift = { whileHover: { y: -4 }, whileTap: { scale: 0.98 } };

const REDUCED_VARIANT = { initial: { opacity: 0 }, animate: { opacity: 1 }, exit: { opacity: 0 } };
const REDUCED_STAGGER = { animate: { transition: { staggerChildren: 0 } } };

/**
 * Returns the requested variant set, swapped for a near-instant opacity-only fade when the OS
 * "reduce motion" preference is set. Every page should read variants through this hook rather
 * than importing the raw constants directly, so reduced-motion support isn't opt-in per call site.
 */
export function useReducedMotionSafe() {
  const reduce = useReducedMotion();
  return {
    fadeInUp: reduce ? REDUCED_VARIANT : fadeInUp,
    fadeIn: reduce ? REDUCED_VARIANT : fadeIn,
    scaleIn: reduce ? REDUCED_VARIANT : scaleIn,
    staggerContainer: reduce ? REDUCED_STAGGER : staggerContainer,
    hoverLift: reduce ? {} : hoverLift,
  };
}
