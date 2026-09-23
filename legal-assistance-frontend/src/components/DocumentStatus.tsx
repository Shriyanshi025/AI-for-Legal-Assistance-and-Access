import React from 'react';
import type { DocumentStatus as StatusEnum } from '../types/document';

interface DocumentStatusProps {
  status: StatusEnum;
}

export const DocumentStatus: React.FC<DocumentStatusProps> = ({ status }) => {
  switch (status) {
    case 'UPLOADED':
      return <span className="badge badge-uploaded">Uploaded</span>;
    case 'PROCESSING':
      return <span className="badge badge-processing">Processing...</span>;
    case 'READY':
      return <span className="badge badge-ready">Ready for Q&A</span>;
    case 'FAILED':
      return <span className="badge badge-failed">Extraction Failed</span>;
    default:
      return <span className="badge">{status}</span>;
  }
};
