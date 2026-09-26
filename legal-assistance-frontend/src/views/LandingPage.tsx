import React from 'react';
import { useAuth } from '../context/AuthContext';

interface LandingPageProps {
  onNavigate: (view: 'landing' | 'login' | 'register' | 'dashboard') => void;
}

export const LandingPage: React.FC<LandingPageProps> = ({ onNavigate }) => {
  const { isAuthenticated, logout } = useAuth();

  const handleScrollToSection = (id: string) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="landing-container">
      {/* Top Public Navigation Header */}
      <header className="landing-nav-header">
        <div className="landing-nav-content">
          <button
            type="button"
            className="landing-brand-btn"
            onClick={() => onNavigate('landing')}
            aria-label="Legal Assist Home"
          >
            <div className="landing-logo-box">
              <img
                src="/Justice%20logo.png"
                alt="Legal Assist Balance Scale Logo"
                className="landing-justice-logo"
              />
            </div>
            <div className="landing-brand-text">
              <span className="landing-brand-title">Legal Assist</span>
              <span className="landing-brand-tagline">Your Legal AI Companion</span>
            </div>
          </button>

          <nav className="landing-nav-links" aria-label="Main Navigation">
            <button
              type="button"
              className="landing-nav-link"
              onClick={() => handleScrollToSection('features')}
            >
              Features
            </button>
            <button
              type="button"
              className="landing-nav-link"
              onClick={() => handleScrollToSection('how-it-works')}
            >
              How It Works
            </button>
            <button
              type="button"
              className="landing-nav-link"
              onClick={() => handleScrollToSection('trust-disclaimer')}
            >
              About
            </button>
          </nav>

          <div className="landing-nav-actions">
            {isAuthenticated ? (
              <>
                <button
                  type="button"
                  className="landing-btn-primary"
                  onClick={() => onNavigate('dashboard')}
                >
                  Go to Dashboard
                </button>
                <button
                  type="button"
                  className="landing-btn-secondary"
                  onClick={() => logout()}
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <button
                  type="button"
                  className="landing-btn-secondary"
                  onClick={() => onNavigate('login')}
                >
                  Sign In
                </button>
                <button
                  type="button"
                  className="landing-btn-primary"
                  onClick={() => onNavigate('register')}
                >
                  Get Started
                </button>
              </>
            )}
          </div>
        </div>
      </header>

      <main>
        {/* HERO SECTION */}
        <section className="landing-hero-section" aria-labelledby="hero-heading">
          <div className="landing-hero-content landing-content-container">
            <div className="landing-hero-badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5z" />
                <path d="M2 17l10 5 10-5" />
                <path d="M2 12l10 5 10-5" />
              </svg>
              <span>Plain Language Legal Intelligence</span>
            </div>

            <h1 id="hero-heading" className="landing-hero-title">
              Understand Your Legal Documents. <br className="hero-br" />
              <span className="hero-gradient-text">Get Clearer Answers.</span>
            </h1>

            <p className="landing-hero-subtitle">
              Legal Assist helps you understand your legal documents and explore legal questions in simple language.
            </p>

            <div className="landing-hero-actions">
              <button
                type="button"
                className="landing-cta-primary"
                onClick={() => onNavigate(isAuthenticated ? 'dashboard' : 'register')}
              >
                {isAuthenticated ? 'Open Application' : 'Get Started'}
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
              </button>

              <button
                type="button"
                className="landing-cta-secondary"
                onClick={() => handleScrollToSection('features')}
              >
                Learn More
              </button>
            </div>

            {/* Visual Feature Card Preview */}
            <div className="landing-hero-preview-card" aria-hidden="true">
              <div className="preview-card-header">
                <div className="preview-card-dots">
                  <span className="dot red" />
                  <span className="dot yellow" />
                  <span className="dot green" />
                </div>
                <span className="preview-card-title">Legal Assist Workspace</span>
              </div>
              <div className="preview-card-body">
                <div className="preview-sample-box">
                  <div className="preview-icon-badge">📄</div>
                  <div>
                    <div className="preview-sample-title">Employment_Agreement_2026.pdf</div>
                    <div className="preview-sample-status">Grounded &amp; Ready for Analysis</div>
                  </div>
                </div>
                <div className="preview-chat-bubble">
                  <strong>Question:</strong> What are the termination clause notice periods in this agreement?
                </div>
                <div className="preview-answer-bubble">
                  <strong>Answer:</strong> Section 8.2 specifies a 30-day written notice requirement for either party, with full severance eligibility described in Clause 9.1.
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* HOW LEGAL ASSIST HELPS (FEATURES) */}
        <section id="features" className="landing-section" aria-labelledby="features-heading">
          <div className="landing-content-container">
            <div className="landing-section-header">
              <span className="landing-section-kicker">Core Capabilities</span>
              <h2 id="features-heading" className="landing-section-title">How Legal Assist Helps</h2>
              <p className="landing-section-desc">
                Designed to bring clarity, organization, and peace of mind when working with legal information.
              </p>
            </div>

            <div className="landing-features-grid">
              <article className="landing-feature-card">
                <div className="feature-icon-box blue">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" y1="13" x2="8" y2="13" />
                    <line x1="16" y1="17" x2="8" y2="17" />
                    <polyline points="10 9 9 9 8 9" />
                  </svg>
                </div>
                <h3 className="feature-card-title">Understand Your Documents</h3>
                <p className="feature-card-desc">
                  Upload legal documents and explore their contents in a simpler, easier-to-understand way.
                </p>
              </article>

              <article className="landing-feature-card">
                <div className="feature-icon-box cyan">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                </div>
                <h3 className="feature-card-title">Ask Questions Naturally</h3>
                <p className="feature-card-desc">
                  Ask questions about your documents using everyday language.
                </p>
              </article>

              <article className="landing-feature-card">
                <div className="feature-icon-box purple">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="11" cy="11" r="8" />
                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                  </svg>
                </div>
                <h3 className="feature-card-title">Explore Legal Information</h3>
                <p className="feature-card-desc">
                  Explore relevant information and compare what your documents say.
                </p>
              </article>

              <article className="landing-feature-card">
                <div className="feature-icon-box green">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                  </svg>
                </div>
                <h3 className="feature-card-title">Stay Organized</h3>
                <p className="feature-card-desc">
                  Keep your important legal documents and research sessions together in one place.
                </p>
              </article>
            </div>
          </div>
        </section>

        {/* HOW IT WORKS */}
        <section id="how-it-works" className="landing-section dark-bg" aria-labelledby="how-heading">
          <div className="landing-content-container">
            <div className="landing-section-header">
              <span className="landing-section-kicker">Simple 4-Step Process</span>
              <h2 id="how-heading" className="landing-section-title">How It Works</h2>
              <p className="landing-section-desc">
                Get answers and insights from your documents in four easy steps.
              </p>
            </div>

            <ol className="landing-steps-grid">
              <li className="landing-step-card">
                <div className="step-number">1</div>
                <h3 className="step-title">Upload</h3>
                <p className="step-desc">Add your legal document.</p>
              </li>

              <li className="landing-step-card">
                <div className="step-number">2</div>
                <h3 className="step-title">Ask</h3>
                <p className="step-desc">Ask a question in your own words.</p>
              </li>

              <li className="landing-step-card">
                <div className="step-number">3</div>
                <h3 className="step-title">Understand</h3>
                <p className="step-desc">Explore information grounded in your documents.</p>
              </li>

              <li className="landing-step-card">
                <div className="step-number">4</div>
                <h3 className="step-title">Research</h3>
                <p className="step-desc">Compare and explore relevant information.</p>
              </li>
            </ol>
          </div>
        </section>

        {/* TRUST & CLARITY SECTION */}
        <section id="trust-disclaimer" className="landing-section" aria-labelledby="trust-heading">
          <div className="landing-content-container">
            <div className="landing-trust-card">
              <div className="trust-icon-box">
                <img
                  src="/Justice%20logo.png"
                  alt="Justice Balance Scale Icon"
                  className="trust-justice-logo"
                />
              </div>
              <h2 id="trust-heading" className="trust-title">
                Designed to make legal information easier to understand.
              </h2>
              <p className="trust-text">
                Legal Assist is designed to help you explore your documents and legal questions more clearly. It is an information and research tool, not a substitute for professional legal advice.
              </p>
            </div>
          </div>
        </section>

        {/* FINAL CTA SECTION */}
        <section className="landing-final-cta-section" aria-labelledby="final-cta-heading">
          <div className="landing-content-container">
            <div className="landing-final-cta-card">
              <h2 id="final-cta-heading" className="final-cta-title">
                Ready to explore your legal documents?
              </h2>
              <p className="final-cta-subtitle">
                Get started today with plain-language document analysis and legal Q&amp;A.
              </p>
              <div className="final-cta-actions">
                <button
                  type="button"
                  className="landing-cta-primary"
                  onClick={() => onNavigate(isAuthenticated ? 'dashboard' : 'register')}
                >
                  {isAuthenticated ? 'Open Application' : 'Get Started'}
                </button>

                {!isAuthenticated && (
                  <button
                    type="button"
                    className="landing-cta-secondary"
                    onClick={() => onNavigate('login')}
                  >
                    Already have an account? Log in
                  </button>
                )}
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* FOOTER */}
      <footer className="landing-footer">
        <div className="landing-footer-content landing-content-container">
          <div className="footer-brand">
            <div className="landing-logo-box small">
              <img
                src="/Justice%20logo.png"
                alt="Legal Assist Logo"
                className="landing-justice-logo"
              />
            </div>
            <span className="footer-brand-name">Legal Assist</span>
          </div>
          <p className="footer-copyright">
            &copy; {new Date().getFullYear()} Legal Assist. Your Legal AI Companion. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
};
