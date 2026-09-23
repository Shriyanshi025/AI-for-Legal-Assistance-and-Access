import React, { useState, useRef } from 'react';
import { documentApi } from '../api/documentApi';
import type { DocumentResponse } from '../types/document';

interface DocumentUploadProps {
  onUploadSuccess: (newDocument: DocumentResponse) => void;
}

export const DocumentUpload: React.FC<DocumentUploadProps> = ({ onUploadSuccess }) => {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFile = async (file: File) => {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      setError('Please select a valid PDF document file.');
      return;
    }

    setError(null);
    setIsUploading(true);

    try {
      const doc = await documentApi.uploadDocument(file);
      onUploadSuccess(doc);
    } catch (err: any) {
      setError(err.message || 'Failed to upload document');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  return (
    <div className="glass-panel">
      <h3 className="panel-title">
        <span>📄</span> Upload Legal Document
      </h3>

      <div
        className={`upload-dropzone ${dragActive ? 'dragging' : ''}`}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf"
          style={{ display: 'none' }}
          onChange={handleChange}
          disabled={isUploading}
        />

        <div className="upload-icon">📥</div>
        <div className="upload-text">
          {isUploading ? 'Uploading PDF...' : 'Drag & drop PDF here, or click to browse'}
        </div>
        <div className="upload-hint">Supported format: PDF documents only</div>
      </div>

      {error && (
        <div style={{ color: 'var(--status-failed)', fontSize: '0.85rem', marginTop: '0.75rem' }}>
          ⚠️ {error}
        </div>
      )}
    </div>
  );
};
