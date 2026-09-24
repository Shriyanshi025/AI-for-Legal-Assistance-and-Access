import React from 'react';
import { getDemoUserId } from '../api/documentApi';

export const Navbar: React.FC = () => {
  const userId = getDemoUserId();

  return (
    <header className="header-card">
      <div className="header-brand">
        <div className="brand-icon-box">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <path d="m9 12 2 2 4-4" />
          </svg>
        </div>
        <span className="brand-title">AI Legal Assistant</span>
        <span className="brand-tagline">Your Legal Documents, Smarter</span>
      </div>

      <div className="user-badge">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
        <span>User ID: {userId}</span>
      </div>
    </header>
  );
};
