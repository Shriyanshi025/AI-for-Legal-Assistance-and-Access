import React, { useState, useEffect } from 'react';
import { documentApi } from '../api/documentApi';
import { LanguageSelector } from './LanguageSelector';
import { useAuth } from '../context/AuthContext';

interface NavbarProps {
  isMobileMenuOpen?: boolean;
  onToggleMobileMenu?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ isMobileMenuOpen, onToggleMobileMenu }) => {
  const { user, logout } = useAuth();
  const [publicUserId, setPublicUserId] = useState<string>('USR-LOADING');

  useEffect(() => {
    let isMounted = true;
    documentApi.getUserProfile()
      .then((profile) => {
        if (isMounted && profile && profile.publicUserId) {
          setPublicUserId(profile.publicUserId);
        }
      })
      .catch(() => {
        if (isMounted) {
          setPublicUserId(user?.publicUserId || 'USR-DEFAULT');
        }
      });

    return () => {
      isMounted = false;
    };
  }, [user]);

  const displayName = user?.name || user?.email || publicUserId;

  return (
    <header className="header-card">
      <div className="header-brand">
        {onToggleMobileMenu && (
          <button
            type="button"
            className="mobile-menu-toggle-btn"
            onClick={onToggleMobileMenu}
            aria-label="Toggle navigation menu"
            aria-expanded={isMobileMenuOpen}
          >
            {isMobileMenuOpen ? (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            ) : (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </svg>
            )}
          </button>
        )}

        <div className="brand-icon-box">
          <img
            src="/Justice%20logo.png"
            alt="Legal Assist Logo"
            className="brand-justice-logo"
          />
        </div>
        <div className="brand-text-group">
          <span className="brand-title">Legal Assist</span>
          <span className="brand-tagline">Your Legal AI Companion</span>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
        <LanguageSelector />

        <div className="user-badge" title={user?.email || 'Authenticated User'}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
          <span className="notranslate">{displayName}</span>
        </div>

        <button
          type="button"
          onClick={() => logout()}
          className="auth-logout-btn"
          title="Sign out of Legal Assist"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>
          <span>Logout</span>
        </button>
      </div>
    </header>
  );
};
