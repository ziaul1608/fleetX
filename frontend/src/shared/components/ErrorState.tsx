import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Operation Error',
  message = 'An unexpected error occurred while fetching system data.',
  onRetry,
}) => {
  return (
    <div className="fx-state-container" role="alert">
      <AlertCircle className="fx-state-icon text-danger" size={40} />
      <div className="fx-state-title">{title}</div>
      <div className="fx-state-desc">{message}</div>
      {onRetry && (
        <button className="btn btn-outline-secondary btn-sm d-flex align-items-center gap-2" onClick={onRetry}>
          <RefreshCw size={14} /> Retry Operation
        </button>
      )}
    </div>
  );
};
