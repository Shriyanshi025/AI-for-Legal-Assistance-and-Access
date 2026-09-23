package com.legalassist.service.storage;

import com.legalassist.config.SupabaseStorageProperties;
import com.legalassist.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseStorageServiceTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    @DisplayName("uploadFile should send POST request to Supabase Storage endpoint with headers and return path")
    void uploadFileShouldSendCorrectHttpRequest() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties(
                "https://test.supabase.co",
                "test-api-key",
                new SupabaseStorageProperties.StorageProperties("documents")
        );
        SupabaseStorageService storageService = new SupabaseStorageService(properties, restClientBuilder);

        mockServer.expect(requestTo("https://test.supabase.co/storage/v1/object/documents/user1/doc.pdf"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(header("apikey", "test-api-key"))
                .andRespond(withSuccess());

        byte[] content = "test content".getBytes();
        String result = storageService.uploadFile("user1/doc.pdf", content, MediaType.APPLICATION_PDF_VALUE);

        assertThat(result).isEqualTo("user1/doc.pdf");
        mockServer.verify();
    }

    @Test
    @DisplayName("uploadFile should throw IllegalArgumentException when content is empty or null")
    void uploadFileShouldThrowOnEmptyContent() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties(
                "https://test.supabase.co",
                "test-api-key",
                new SupabaseStorageProperties.StorageProperties("documents")
        );
        SupabaseStorageService storageService = new SupabaseStorageService(properties, restClientBuilder);

        assertThatThrownBy(() -> storageService.uploadFile("doc.pdf", new byte[0], "text/plain"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("uploadFile should throw StorageException when Supabase configuration is missing")
    void uploadFileShouldThrowWhenConfigIsMissing() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties(
                "",
                "",
                new SupabaseStorageProperties.StorageProperties("documents")
        );
        SupabaseStorageService storageService = new SupabaseStorageService(properties, restClientBuilder);

        assertThatThrownBy(() -> storageService.uploadFile("doc.pdf", "content".getBytes(), "text/plain"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Storage configuration is not configured");
    }
}
