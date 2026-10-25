import React from 'react';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  children?: React.ReactNode;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ title, subtitle, children }) => {
  return (
    <div className="d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center mb-4 gap-2">
      <div>
        <h1 className="h4 fw-bold mb-1" style={{ color: 'var(--fx-ivory-text)', letterSpacing: '-0.02em' }}>
          {title}
        </h1>
        {subtitle && <p className="small mb-0" style={{ color: 'var(--fx-muted-sand)' }}>{subtitle}</p>}
      </div>
      {children && <div className="d-flex align-items-center gap-2">{children}</div>}
    </div>
  );
};
