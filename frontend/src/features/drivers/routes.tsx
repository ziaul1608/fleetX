import { lazy } from 'react';
import { RouteObject } from 'react-router-dom';

const DriversPage = lazy(() => import('./pages/DriversPage').then(m => ({ default: m.DriversPage })));
const TripsPage = lazy(() => import('./pages/TripsPage').then(m => ({ default: m.TripsPage })));

export const driverRoutes: RouteObject[] = [
  {
    path: 'trips/drivers',
    element: <DriversPage />,
  },
  {
    path: 'trips/dispatch',
    element: <TripsPage />,
  },
];

