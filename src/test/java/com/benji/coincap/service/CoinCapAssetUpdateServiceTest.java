package com.benji.coincap.service;

import com.benji.coincap.service.dto.CoinCapAssetResponse;
import com.benji.exception.InvalidCoinCapApiKeyException;
import com.benji.models.entities.AssetSymbolMapping;
import com.benji.repositories.AssetSymbolMappingRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CoinCapAssetUpdateServiceTest {

    @Autowired
    private CoinCapAssetUpdateService coinCapAssetUpdateService;

    @Autowired
    private AssetSymbolMappingRepository assetSymbolMappingRepository;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("coincap.api.url", () -> mockWebServer.url("/").toString());
        registry.add("coincap.api.key", () -> "test-key");
    }

    @BeforeEach
    void reset() {
        assetSymbolMappingRepository.deleteAll();
    }

    @Test
    public void init_WhenCalled_SavesAssetMappings() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                {
                    "data": [
                        {"id": "bitcoin", "symbol": "BTC"},
                        {"id": "ethereum", "symbol": "ETH"}
                    ]
                }
                """)
                .addHeader("Content-Type", "application/json"));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<AssetSymbolMapping> mappings = assetSymbolMappingRepository.findAll();
                    assertThat(mappings).hasSize(2);
                });
    }

    @Test
    public void init_WhenFetchFails_LogsError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<AssetSymbolMapping> mappings = assetSymbolMappingRepository.findAll();
                    assertThat(mappings).isEmpty();
                });
    }

    @Test
    public void fetchLatestPrice_whenSuccess_thenReturnResponse() {
        String tokenId = "bitcoin";
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                {
                    "data": {
                        "id": "bitcoin",
                        "symbol": "BTC",
                        "priceUsd": "40000.00"
                    }
                }
                """)
                .addHeader("Content-Type", "application/json"));

        CoinCapAssetResponse result = coinCapAssetUpdateService.fetchLatestPrice(tokenId).block();
        assertNotNull(result);
        assertEquals("40000.00", result.data().priceUsd());
    }

    @Test
    public void fetchLatestPrice_whenError_thenReturnsEmptyMono() {
        String tokenId = "invalidtoken";
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        CoinCapAssetResponse result = coinCapAssetUpdateService.fetchLatestPrice(tokenId).block();
        assertNull(result);
    }

    @Test
    public void fetchLatestPrice_whenForbidden_thenThrowsInvalidCoinCapApiKeyException() {
        String tokenId = "bitcoin";
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(403)
                        .setBody("{\"message\": \"API key is invalid or expired\"}")
                        .addHeader("Content-Type", "application/json")
        );

        assertThrows(
                InvalidCoinCapApiKeyException.class,
                () -> coinCapAssetUpdateService.fetchLatestPrice(tokenId).block()
        );
    }

    @Test
    public void fetchLatestPrice_whenServerError_thenThrowsRuntimeException() {
        String tokenId = "bitcoin";
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(500)
                        .setBody("{\"message\": \"Server error\"}")
                        .addHeader("Content-Type", "application/json")
        );

        assertThrows(
                RuntimeException.class,
                () -> coinCapAssetUpdateService.fetchLatestPrice(tokenId).block()
        );
    }

}