import React from 'react';
import { Navbar } from './components/Navbar';
import { DocumentManager } from './views/DocumentManager';

export const App: React.FC = () => {
  return (
    <>
      <Navbar />
      <DocumentManager />
    </>
  );
};

export default App;
