import React from 'react';
import { ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';

interface UnauthorizedStateProps {
  message?: string;
}

export const UnauthorizedState: React.FC<UnauthorizedStateProps> = ({
  message = 'You do not have the required role privileges to access this operational module.',
}) => {
  return (
    <div className="fx-state-container" role="alert">
      <ShieldAlert className="fx-state-icon text-warning" size={44} />
      <div className="fx-state-title">Access Restricted</div>
      <div className="fx-state-desc">{message}</div>
      <Link to="/login" className="btn btn-outline-primary btn-sm">
        Return to Login
      </Link>
    </div>
  );
};
