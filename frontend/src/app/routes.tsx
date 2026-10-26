import { createBrowserRouter, RouteObject } from 'react-router-dom';
import { Layout } from './Layout';
import { ProtectedRoute } from '../shared/components/ProtectedRoute';
import { authRoutes } from '../features/auth/routes';
import { fleetRoutes } from '../features/fleet/routes';
import { driverRoutes } from '../features/drivers/routes';
import { shipmentRoutes } from '../features/shipments/routes';
import { trackingRoutes } from '../features/tracking/routes';
import { LandingPage } from '../features/landing/pages/LandingPage';

export const routes: RouteObject[] = [
  // Public FleetX Cover Page: default entry point for everyone visiting '/'
  {
    path: '/',
    element: <LandingPage />,
  },

  // Standalone public routes (login and signup)
  ...authRoutes.filter((r) => r.path === 'login' || r.path === 'signup'),

  // Main application shell routes (all protected)
  {
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
];

export const router = createBrowserRouter(routes);
