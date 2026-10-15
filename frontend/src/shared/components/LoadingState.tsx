import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoadingStateProps {
  message?: string;
}

export const LoadingState: React.FC<LoadingStateProps> = ({ message = 'Loading fleet telemetry...' }) => {
  return (
    <div className="fx-state-container" role="status" aria-live="polite">
      <Loader2 className="fx-state-icon animate-spin text-primary" size={36} />
      <div className="fx-state-title">Please wait</div>
      <div className="fx-state-desc mb-0">{message}</div>
    </div>
  );
};
