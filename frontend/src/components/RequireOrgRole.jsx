import { useAuth } from '../context/AuthContext';

/** Renders children only if the current user's orgRole is one of `roles`. Renders nothing otherwise. */
export default function RequireOrgRole({ roles, children }) {
  const { user } = useAuth();
  if (!user?.orgRole || !roles.includes(user.orgRole)) {
    return null;
  }
  return children;
}
