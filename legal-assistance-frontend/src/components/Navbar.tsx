import React from 'react';
import { DEFAULT_USER_ID } from '../api/documentApi';

export const Navbar: React.FC = () => {
  return (
    <header className="app-header">
      <div className="header-container">
        <div className="brand-logo">
          <div className="brand-icon">§</div>
          <span>LegalAssist AI</span>
        </div>
        <div className="session-badge">
          <span className="session-dot"></span>
          <span>Session: {DEFAULT_USER_ID.substring(0, 8)}...</span>
        </div>
      </div>
    </header>
  );
};
