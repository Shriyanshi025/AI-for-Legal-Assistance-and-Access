import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LoginPage } from './views/LoginPage';
import { RegisterPage } from './views/RegisterPage';
import { DocumentManager } from './views/DocumentManager';

const AppRoutes: React.FC = () => {
  const { isAuthenticated, loading } = useAuth();
  const [authView, setAuthView] = useState<'login' | 'register'>('login');

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

  if (!isAuthenticated) {
    if (authView === 'register') {
      return <RegisterPage onSwitchToLogin={() => setAuthView('login')} />;
    }
    return <LoginPage onSwitchToRegister={() => setAuthView('register')} />;
  }

  return <DocumentManager />;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
};

export default App;
