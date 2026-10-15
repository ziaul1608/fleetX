import { lazy, Suspense } from 'react';
import { RouteObject } from 'react-router-dom';
import { ProtectedRoute } from '../../shared/components/ProtectedRoute';
import { LoadingState } from '../../shared/components/LoadingState';

const LoginPage = lazy(() => import('./pages/LoginPage').then(m => ({ default: m.LoginPage })));
const UsersPage = lazy(() => import('./pages/UsersPage').then(m => ({ default: m.UsersPage })));
const ProfilePage = lazy(() => import('./pages/ProfilePage').then(m => ({ default: m.ProfilePage })));

export const authRoutes: RouteObject[] = [
  {
    path: 'login',
    element: (
      <Suspense fallback={<LoadingState message="Loading login portal..." />}>
        <LoginPage />
      </Suspense>
    ),
  },
  {
    path: 'admin/users',
    element: (
      <ProtectedRoute requiredRoles={['ADMIN']}>
        <UsersPage />
      </ProtectedRoute>
    ),
  },
  {
    path: 'profile',
    element: <ProfilePage />,
  },
];

