import { lazy } from 'react';
import { RouteObject } from 'react-router-dom';

const VehiclesPage = lazy(() => import('./pages/VehiclesPage').then(m => ({ default: m.VehiclesPage })));
const MaintenancePage = lazy(() => import('./pages/MaintenancePage').then(m => ({ default: m.MaintenancePage })));

export const fleetRoutes: RouteObject[] = [
  {
    path: 'fleet/vehicles',
    element: <VehiclesPage />,
  },
  {
    path: 'fleet/maintenance',
    element: <MaintenancePage />,
  },
];

