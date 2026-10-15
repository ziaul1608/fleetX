import { RouteObject } from 'react-router-dom';
import { DriversPage } from './pages/DriversPage';
import { TripsPage } from './pages/TripsPage';

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
