import React from 'react';
import { RouterProvider } from 'react-router-dom';
import { router } from './routes';
import { AuthProvider } from '../shared/context/AuthContext';

interface AppProps {
  routerOverride?: any;
}

export const App: React.FC<AppProps> = ({ routerOverride }) => {
  return (
    <AuthProvider>
      <RouterProvider router={routerOverride || router} />
    </AuthProvider>
  );
};
