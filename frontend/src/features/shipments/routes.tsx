import { lazy } from 'react';
import { RouteObject } from 'react-router-dom';

const ShipmentsPage = lazy(() => import('./pages/ShipmentsPage').then(m => ({ default: m.ShipmentsPage })));
const WaypointsPage = lazy(() => import('./pages/WaypointsPage').then(m => ({ default: m.WaypointsPage })));

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

