package com.legalassist.service.storage;

public interface StorageService {

    /**
     * Uploads file content to storage at the specified path.
     *
     * @param path        target storage path/key
     * @param content     file bytes
     * @param contentType MIME type of the file
     * @return storage path or resource identifier
     */
    String uploadFile(String path, byte[] content, String contentType);

    /**
     * Deletes a file from storage if present.
     *
     * @param path storage path/key
     */
    void deleteFile(String path);

    /**
     * Downloads file content from storage at the specified path.
     *
     * @param path storage path/key
     * @return file bytes
     */
    byte[] downloadFile(String path);
}
