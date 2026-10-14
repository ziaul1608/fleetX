import { RouteObject } from 'react-router-dom';
import { VehiclesPage } from './pages/VehiclesPage';
import { MaintenancePage } from './pages/MaintenancePage';

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
