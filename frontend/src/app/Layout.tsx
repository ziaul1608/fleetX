import React, { Suspense, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from '../shared/components/Sidebar';
import { TopNav } from '../shared/components/TopNav';
import { LoadingState } from '../shared/components/LoadingState';

export const Layout: React.FC = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  return (
    <div className="fx-app-container">
      <Sidebar
        collapsed={sidebarCollapsed}
        onToggle={() => setSidebarCollapsed(!sidebarCollapsed)}
      />
      <div className={`fx-main-wrapper ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
        <TopNav />
        <main className="fx-content-area" role="main">
          <Suspense fallback={<LoadingState message="Loading module..." />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  );
};

