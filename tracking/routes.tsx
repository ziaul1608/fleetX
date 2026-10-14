import { RouteObject } from 'react-router-dom';
import { ControlTowerPage } from './pages/ControlTowerPage';
import { ReportsPage } from './pages/ReportsPage';

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
