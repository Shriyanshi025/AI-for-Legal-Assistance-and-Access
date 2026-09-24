package com.legalassist.service.storage;

import com.legalassist.config.SupabaseStorageProperties;
import com.legalassist.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.springframework.beans.factory.ObjectProvider;

@Service
public class SupabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final SupabaseStorageProperties properties;
    private final RestClient restClient;

    public SupabaseStorageService(SupabaseStorageProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SupabaseStorageService(SupabaseStorageProperties properties, ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
        this(properties, restClientBuilderProvider.getIfAvailable(RestClient::builder));
    }


    @Override
    public String uploadFile(String path, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content must not be empty");
        }

        String url = properties.url();
        String key = properties.key();
        String bucket = properties.storage() != null ? properties.storage().bucket() : "documents";

        if (url == null || url.isBlank() || key == null || key.isBlank()) {
            log.warn("Supabase Storage configuration (URL or Key) is missing. Upload failed for path: {}", path);
            throw new StorageException("Storage configuration is not configured");
        }

        String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String endpointUrl = String.format("%s/storage/v1/object/%s/%s", normalizedUrl, bucket, normalizedPath);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (Exception e) {
                log.debug("Unparseable contentType '{}', defaulting to application/octet-stream", contentType);
            }
        }

        try {
            restClient.post()
                    .uri(endpointUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .header("apikey", key)
                    .header("x-upsert", "true")
                    .contentType(mediaType)
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully uploaded file to Supabase Storage at bucket: {}, path: {}", bucket, normalizedPath);
            return normalizedPath;
        } catch (Exception e) {
            log.error("Failed to upload file to Supabase Storage at path: {}", normalizedPath, e);
            throw new StorageException("Failed to upload file to storage: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String path) {
        String url = properties.url();
        String key = properties.key();
        String bucket = properties.storage() != null ? properties.storage().bucket() : "documents";

        if (url == null || url.isBlank() || key == null || key.isBlank()) {
            log.warn("Supabase Storage configuration is missing. Cannot delete path: {}", path);
            return;
        }

        String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String endpointUrl = String.format("%s/storage/v1/object/%s/%s", normalizedUrl, bucket, normalizedPath);

        try {
            restClient.delete()
                    .uri(endpointUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .header("apikey", key)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully deleted file from Supabase Storage at bucket: {}, path: {}", bucket, normalizedPath);
        } catch (Exception e) {
            log.warn("Failed to delete file from Supabase Storage at path: {}", normalizedPath, e);
        }
    }

    @Override
    public byte[] downloadFile(String path) {
        String url = properties.url();
        String key = properties.key();
        String bucket = properties.storage() != null ? properties.storage().bucket() : "documents";

        if (url == null || url.isBlank() || key == null || key.isBlank()) {
            log.warn("Supabase Storage configuration is missing. Cannot download path: {}", path);
            throw new StorageException("Storage configuration is not configured");
        }

        String normalizedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String endpointUrl = String.format("%s/storage/v1/object/%s/%s", normalizedUrl, bucket, normalizedPath);

        try {
            byte[] body = restClient.get()
                    .uri(endpointUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .header("apikey", key)
                    .retrieve()
                    .body(byte[].class);

            if (body == null || body.length == 0) {
                throw new StorageException("Downloaded file from storage is empty at path: " + normalizedPath);
            }

            log.info("Successfully downloaded file from Supabase Storage at bucket: {}, path: {}", bucket, normalizedPath);
            return body;
        } catch (Exception e) {
            log.error("Failed to download file from Supabase Storage at path: {}", normalizedPath, e);
            throw new StorageException("Failed to download file from storage: " + e.getMessage(), e);
        }
    }
}
