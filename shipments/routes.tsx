import { RouteObject } from 'react-router-dom';
import { ShipmentsPage } from './pages/ShipmentsPage';
import { WaypointsPage } from './pages/WaypointsPage';

export const shipmentRoutes: RouteObject[] = [
  {
    path: 'shipments',
    element: <ShipmentsPage />,
  },
  {
    path: 'shipments/waypoints',
    element: <WaypointsPage />,
  },
];
