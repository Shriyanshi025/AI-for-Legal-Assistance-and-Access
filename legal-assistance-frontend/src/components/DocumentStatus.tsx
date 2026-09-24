import React from 'react';
import type { DocumentStatus as StatusEnum } from '../types/document';

interface DocumentStatusProps {
  status: StatusEnum;
}

export const DocumentStatus: React.FC<DocumentStatusProps> = ({ status }) => {
  switch (status) {
    case 'READY':
      return <span className="status-pill ready">READY</span>;
    case 'PROCESSING':
      return <span className="status-pill processing">PROCESSING</span>;
    case 'UPLOADED':
      return <span className="status-pill uploaded">UPLOADED</span>;
    case 'FAILED':
      return <span className="status-pill failed">FAILED</span>;
    default:
      return <span className="status-pill uploaded">{status}</span>;
  }
};
