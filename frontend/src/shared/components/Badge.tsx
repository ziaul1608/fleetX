import React from 'react';

interface BadgeProps {
  status: string;
}

export const Badge: React.FC<BadgeProps> = ({ status }) => {
  const normalized = status.toUpperCase();

  let badgeClass = 'fx-badge-secondary';
  if (['AVAILABLE', 'DELIVERED', 'COMPLETED', 'ACTIVE', 'UP'].includes(normalized)) {
    badgeClass = 'fx-badge-success';
  } else if (['IN_TRIP', 'IN_TRANSIT', 'ASSIGNED', 'ON_DUTY'].includes(normalized)) {
    badgeClass = 'fx-badge-info';
  } else if (['WARNING', 'SERVICE_DUE', 'SUSPENDED'].includes(normalized)) {
    badgeClass = 'fx-badge-warning';
  } else if (['CRITICAL', 'MAINTENANCE', 'CANCELLED', 'RETIRED'].includes(normalized)) {
    badgeClass = 'fx-badge-danger';
  }

  return <span className={`fx-badge ${badgeClass}`}>{status}</span>;
};
