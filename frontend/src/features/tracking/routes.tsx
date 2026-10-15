import { lazy } from 'react';
import { RouteObject } from 'react-router-dom';

const ControlTowerPage = lazy(() => import('./pages/ControlTowerPage').then(m => ({ default: m.ControlTowerPage })));
const ReportsPage = lazy(() => import('./pages/ReportsPage').then(m => ({ default: m.ReportsPage })));

export const trackingRoutes: RouteObject[] = [
  {
    index: true,
    element: <ControlTowerPage />,
  },
  {
    path: 'tracking/map',
    element: <ControlTowerPage />,
  },
  {
    path: 'tracking/reports',
    element: <ReportsPage />,
  },
];

