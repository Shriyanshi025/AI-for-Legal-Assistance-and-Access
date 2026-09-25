import React, { useState, useEffect, useRef } from 'react';

export interface LanguageOption {
  code: string;
  name: string;
  group: 'RECOMMENDED' | 'INDIAN' | 'GLOBAL';
  flag: string;
}

export const LANGUAGES: LanguageOption[] = [
  // ⭐ Recommended
  { code: 'en', name: 'English (Recommended)', group: 'RECOMMENDED', flag: '🇺🇸' },
  { code: 'hi', name: 'Hindi (हिन्दी)', group: 'RECOMMENDED', flag: '🇮🇳' },

  // 🇮🇳 Indian Languages
  { code: 'bn', name: 'Bengali (বাংলা)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'te', name: 'Telugu (తెలుగు)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'mr', name: 'Marathi (मराठी)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'ta', name: 'Tamil (தமிழ்)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'ur', name: 'Urdu (اردو)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'gu', name: 'Gujarati (ગુજરાતી)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'kn', name: 'Kannada (கன்னட)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'ml', name: 'Malayalam (മലയാളം)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'pa', name: 'Punjabi (ਪੰਜਾਬੀ)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'or', name: 'Odia (ଓଡ଼ିଆ)', group: 'INDIAN', flag: '🇮🇳' },
  { code: 'as', name: 'Assamese (অসমীয়া)', group: 'INDIAN', flag: '🇮🇳' },

  // 🌍 Global Languages
  { code: 'es', name: 'Spanish (Español)', group: 'GLOBAL', flag: '🇪🇸' },
  { code: 'fr', name: 'French (Français)', group: 'GLOBAL', flag: '🇫🇷' },
  { code: 'de', name: 'German (Deutsch)', group: 'GLOBAL', flag: '🇩🇪' },
  { code: 'it', name: 'Italian (Italiano)', group: 'GLOBAL', flag: '🇮🇹' },
  { code: 'pt', name: 'Portuguese (Português)', group: 'GLOBAL', flag: '🇵🇹' },
  { code: 'ru', name: 'Russian (Русский)', group: 'GLOBAL', flag: '🇷🇺' },
  { code: 'ja', name: 'Japanese (日本語)', group: 'GLOBAL', flag: '🇯🇵' },
  { code: 'ko', name: 'Korean (한국어)', group: 'GLOBAL', flag: '🇰🇷' },
  { code: 'zh-CN', name: 'Chinese (Simplified)', group: 'GLOBAL', flag: '🇨🇳' },
  { code: 'ar', name: 'Arabic (العربية)', group: 'GLOBAL', flag: '🇸🇦' },
  { code: 'tr', name: 'Turkish (Türkçe)', group: 'GLOBAL', flag: '🇹🇷' },
  { code: 'nl', name: 'Dutch (Nederlands)', group: 'GLOBAL', flag: '🇳🇱' },
  { code: 'pl', name: 'Polish (Polski)', group: 'GLOBAL', flag: '🇵🇱' },
  { code: 'id', name: 'Indonesian (Bahasa)', group: 'GLOBAL', flag: '🇮🇩' },
  { code: 'vi', name: 'Vietnamese (Tiếng Việt)', group: 'GLOBAL', flag: '🇻🇳' },
  { code: 'th', name: 'Thai (ไทย)', group: 'GLOBAL', flag: '🇹🇭' },
];

const STORAGE_KEY = 'legal_assist_selected_lang';

export function setGoogleTranslateLanguage(langCode: string): void {
  localStorage.setItem(STORAGE_KEY, langCode);

  const hostname = window.location.hostname;
  document.cookie = `googtrans=/en/${langCode}; path=/; domain=${hostname}`;
  document.cookie = `googtrans=/en/${langCode}; path=/;`;

  // 1. Check if simple combo element exists
  const selectElem = document.querySelector('.goog-te-combo') as HTMLSelectElement | null;
  if (selectElem) {
    selectElem.value = langCode;
    selectElem.dispatchEvent(new Event('change'));
    return;
  }

  // 2. Target Google Translate iframe popup options
  const iframe = (document.querySelector('iframe.VIpgJd-ZVi9od-xl07Ob-OEVmcd') ||
    document.querySelector('iframe.goog-te-menu-frame')) as HTMLIFrameElement | null;
  if (iframe) {
    try {
      const doc = iframe.contentDocument || iframe.contentWindow?.document;
      if (doc) {
        const targetLangObj = LANGUAGES.find((l) => l.code === langCode);
        if (targetLangObj) {
          const searchName = targetLangObj.name.split(' ')[0];
          const spanList = Array.from(doc.querySelectorAll('span, a')) as HTMLElement[];
          const match = spanList.find((s) => s.innerText.trim().toLowerCase() === searchName.toLowerCase());
          if (match) {
            match.click();
            return;
          }
        }
      }
    } catch {
      // Fallback reload below
    }
  }

  // 3. Fallback: reload page so Google Translate reads updated googtrans cookie
  window.location.reload();
}

function getActiveLanguageCode(): string {
  try {
    const cookies = document.cookie.split(';');
    const googtransCookie = cookies.find((c) => c.trim().startsWith('googtrans='));
    if (googtransCookie) {
      const val = googtransCookie.split('=')[1];
      if (val) {
        const parts = val.split('/');
        const code = parts[parts.length - 1];
        if (code && code !== 'null' && code !== 'undefined') {
          return code;
        }
      }
    }
  } catch {
    // Ignore cookie read failure
  }
  return localStorage.getItem(STORAGE_KEY) || 'en';
}

export const LanguageSelector: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCode, setSelectedCode] = useState(() => getActiveLanguageCode());
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const syncActiveLang = () => {
      const activeCode = getActiveLanguageCode();
      setSelectedCode(activeCode);
    };

    syncActiveLang();
    window.addEventListener('focus', syncActiveLang);
    return () => window.removeEventListener('focus', syncActiveLang);
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelectLanguage = (code: string) => {
    setSelectedCode(code);
    setGoogleTranslateLanguage(code);
    setIsOpen(false);
    setSearchQuery('');
  };

  const filterLangs = (langs: LanguageOption[]) => {
    if (!searchQuery.trim()) return langs;
    const q = searchQuery.toLowerCase();
    return langs.filter((l) => l.name.toLowerCase().includes(q) || l.code.toLowerCase().includes(q));
  };

  const recommendedLangs = filterLangs(LANGUAGES.filter((l) => l.group === 'RECOMMENDED'));
  const indianLangs = filterLangs(LANGUAGES.filter((l) => l.group === 'INDIAN'));
  const globalLangs = filterLangs(LANGUAGES.filter((l) => l.group === 'GLOBAL'));

  const selectedLangObj = LANGUAGES.find((l) => l.code === selectedCode) || LANGUAGES[0];

  return (
    <div className="language-selector-wrapper" ref={dropdownRef} style={{ position: 'relative' }}>
      <button
        type="button"
        className="language-selector-btn"
        onClick={() => setIsOpen((prev) => !prev)}
        title="Select UI Display Language"
        aria-label="Select UI Display Language"
        aria-expanded={isOpen}
      >
        <span className="lang-icon">🌐</span>
        <span className="lang-flag">{selectedLangObj.flag}</span>
        <span className="lang-label">
          {selectedLangObj.code === 'en' ? 'English' : selectedLangObj.name.split(' ')[0]}
        </span>
        <span className="lang-arrow">{isOpen ? '▲' : '▼'}</span>
      </button>

      {isOpen && (
        <div className="language-dropdown-menu">
          {/* Search Box */}
          <div className="language-search-box">
            <input
              type="text"
              placeholder="Search language..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              autoFocus
            />
          </div>

          <div className="language-options-list">
            {/* Recommended */}
            {recommendedLangs.length > 0 && (
              <>
                <div className="language-group-header">⭐ RECOMMENDED</div>
                {recommendedLangs.map((lang) => (
                  <div
                    key={lang.code}
                    className={`language-option-item ${selectedCode === lang.code ? 'selected' : ''}`}
                    onClick={() => handleSelectLanguage(lang.code)}
                  >
                    <span className="opt-flag">{lang.flag}</span>
                    <span className="opt-name">{lang.name}</span>
                    {selectedCode === lang.code && <span className="opt-check">✓</span>}
                  </div>
                ))}
              </>
            )}

            {/* Indian Languages */}
            {indianLangs.length > 0 && (
              <>
                <div className="language-group-header">🇮🇳 INDIAN LANGUAGES</div>
                {indianLangs.map((lang) => (
                  <div
                    key={lang.code}
                    className={`language-option-item ${selectedCode === lang.code ? 'selected' : ''}`}
                    onClick={() => handleSelectLanguage(lang.code)}
                  >
                    <span className="opt-flag">{lang.flag}</span>
                    <span className="opt-name">{lang.name}</span>
                    {selectedCode === lang.code && <span className="opt-check">✓</span>}
                  </div>
                ))}
              </>
            )}

            {/* Global Languages */}
            {globalLangs.length > 0 && (
              <>
                <div className="language-group-header">🌍 GLOBAL LANGUAGES</div>
                {globalLangs.map((lang) => (
                  <div
                    key={lang.code}
                    className={`language-option-item ${selectedCode === lang.code ? 'selected' : ''}`}
                    onClick={() => handleSelectLanguage(lang.code)}
                  >
                    <span className="opt-flag">{lang.flag}</span>
                    <span className="opt-name">{lang.name}</span>
                    {selectedCode === lang.code && <span className="opt-check">✓</span>}
                  </div>
                ))}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
