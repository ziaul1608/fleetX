import React from 'react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: React.ReactNode;
  trend?: {
    value: string;
    isPositive?: boolean;
  };
}

export const StatCard: React.FC<StatCardProps> = ({ title, value, subtitle, icon, trend }) => {
  return (
    <div className="fx-stat-card">
      <div className="fx-stat-header">
        <span className="fx-stat-title">{title}</span>
        {icon && <div style={{ color: 'var(--fx-champagne-gold)' }}>{icon}</div>}
      </div>
      <div className="fx-stat-value">{value}</div>
      {(subtitle || trend) && (
        <div className="fx-stat-footer d-flex align-items-center justify-content-between">
          {subtitle && <span>{subtitle}</span>}
          {trend && (
            <span className={`fw-medium ${trend.isPositive ? 'text-success' : 'text-danger'}`}>
              {trend.value}
            </span>
          )}
        </div>
      )}
    </div>
  );
};
