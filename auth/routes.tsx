import { RouteObject } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { UsersPage } from './pages/UsersPage';
import { ProfilePage } from './pages/ProfilePage';

export const authRoutes: RouteObject[] = [
  {
    path: 'login',
    element: <LoginPage />,
  },
  {
    path: 'admin/users',
    element: <UsersPage />,
  },
  {
    path: 'profile',
    element: <ProfilePage />,
  },
];
