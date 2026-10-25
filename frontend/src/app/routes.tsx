import { createBrowserRouter, Navigate } from 'react-router-dom';
import { Layout } from './Layout';
import { ProtectedRoute } from '../shared/components/ProtectedRoute';
import { authRoutes } from '../features/auth/routes';
import { fleetRoutes } from '../features/fleet/routes';
import { driverRoutes } from '../features/drivers/routes';
import { shipmentRoutes } from '../features/shipments/routes';
import { trackingRoutes } from '../features/tracking/routes';
import { LandingPage } from '../features/landing/pages/LandingPage';
import { useAuth } from '../shared/context/AuthContext';
import { LoadingState } from '../shared/components/LoadingState';

/**
 * Root Router Component:
 * - Unauthenticated users visiting '/' see the public marketing LandingPage.
 * - Authenticated users visiting '/' are redirected to '/dashboard' (Control Tower).
 */
const RootHandler: React.FC = () => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) {
    return <LoadingState message="Connecting to FleetX..." />;
  }
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }
  return <LandingPage />;
};

export const router = createBrowserRouter([
  // Public Landing Page
  {
    path: '/',
    element: <RootHandler />,
  },

  // Standalone public routes (login and signup)
  ...authRoutes.filter((r) => r.path === 'login' || r.path === 'signup'),

  // Main application shell routes (all protected)
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <Layout />
      </ProtectedRoute>
    ),
    children: [
      ...trackingRoutes,
      ...fleetRoutes,
      ...driverRoutes,
      ...shipmentRoutes,
      ...authRoutes.filter((r) => r.path !== 'login' && r.path !== 'signup'),
    ],
  },
]);
