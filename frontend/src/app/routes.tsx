import { createBrowserRouter } from 'react-router-dom';
import { Layout } from './Layout';
import { ProtectedRoute } from '../shared/components/ProtectedRoute';
import { authRoutes } from '../features/auth/routes';
import { fleetRoutes } from '../features/fleet/routes';
import { driverRoutes } from '../features/drivers/routes';
import { shipmentRoutes } from '../features/shipments/routes';
import { trackingRoutes } from '../features/tracking/routes';

/**
 * Central Leader-Maintained Route Registry.
 * Aggregates independently maintained route exports from all 5 feature folders.
 * Application shell routes are strictly guarded by ProtectedRoute.
 */
export const router = createBrowserRouter([
  // Standalone public routes
  ...authRoutes.filter((r) => r.path === 'login'),

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
      ...authRoutes.filter((r) => r.path !== 'login'),
    ],
  },
]);
