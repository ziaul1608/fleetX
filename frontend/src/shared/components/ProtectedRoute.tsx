import React from 'react';
import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LoadingState } from './LoadingState';
import { UnauthorizedState } from './UnauthorizedState';
import { RoleType } from '../../features/auth/types';

interface ProtectedRouteProps {
  children?: React.ReactNode;
  requiredRoles?: RoleType[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, requiredRoles }) => {
  const { currentUser, isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="d-flex align-items-center justify-content-center min-vh-100 bg-light p-4">
        <LoadingState message="Verifying session credentials..." />
      </div>
    );
  }

  if (!isAuthenticated || !currentUser) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredRoles && requiredRoles.length > 0) {
    const hasRequiredRole = currentUser.roles.some((role) => requiredRoles.includes(role));
    if (!hasRequiredRole) {
      return (
        <div className="p-4">
          <UnauthorizedState
            message={`Access Restricted. Required role: ${requiredRoles.join(' or ')}. Your current role: ${currentUser.roles.join(', ')}.`}
          />
        </div>
      );
    }
  }

  return children ? <>{children}</> : <Outlet />;
};
