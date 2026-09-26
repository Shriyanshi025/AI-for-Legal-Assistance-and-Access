import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LandingPage } from './views/LandingPage';
import { LoginPage } from './views/LoginPage';
import { RegisterPage } from './views/RegisterPage';
import { DocumentManager } from './views/DocumentManager';

type AppView = 'landing' | 'login' | 'register' | 'dashboard';

const AppRoutes: React.FC = () => {
  const { isAuthenticated, loading } = useAuth();
  const [view, setView] = useState<AppView>('landing');

  if (loading) {
    return (
      <div className="auth-loading-screen">
        <div className="auth-logo-box">
          <img
            src="/Justice%20logo.png"
            alt="Legal Assist Logo"
            className="auth-justice-logo"
          />
        </div>
        <div className="auth-spinner" style={{ width: '28px', height: '28px', borderWidth: '3px' }} />
        <p style={{ color: '#A0AEC0', fontSize: '0.9rem', fontWeight: 500 }}>Authenticating Legal Assist session...</p>
      </div>
    );
  }

  // Handle views for authenticated vs unauthenticated states
  if (!isAuthenticated) {
    if (view === 'login') {
      return (
        <LoginPage
          onSwitchToRegister={() => setView('register')}
          onNavigateToLanding={() => setView('landing')}
        />
      );
    }
    if (view === 'register') {
      return (
        <RegisterPage
          onSwitchToLogin={() => setView('login')}
          onNavigateToLanding={() => setView('landing')}
        />
      );
    }
    return <LandingPage onNavigate={(targetView) => setView(targetView)} />;
  }

  // Authenticated state
  if (view === 'landing') {
    return <LandingPage onNavigate={(targetView) => setView(targetView)} />;
  }

  return <DocumentManager onNavigateToLanding={() => setView('landing')} />;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
};

export default App;
