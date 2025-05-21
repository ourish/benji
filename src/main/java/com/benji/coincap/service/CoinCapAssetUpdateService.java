package com.benji.coincap.service;

import com.benji.coincap.service.dto.CoinCapAssetResponse;
import com.benji.coincap.service.dto.CoinCapAssetsResponse;
import com.benji.exception.InvalidCoinCapApiKeyException;
import com.benji.models.entities.AssetSymbolMapping;
import com.benji.repositories.AssetRepository;
import com.benji.repositories.AssetSymbolMappingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinCapAssetUpdateService {

    private final WebClient coinCapClient;
    private final AssetRepository assetRepository;
    private final AssetSymbolMappingRepository assetSymbolMappingRepository;

    @Value("${coincap.api.url}")
    private String coinCapApiUrl;

    @Value("${coincap.api.key:}")
    private String coinCapApiKey;

    @Value("${coincap.api.max-threads:3}")
    private int maxThreads;

    @Value("${coincap.api.refresh-rate}")
    private String refreshRate;

    @PostConstruct
    public void init() {
        if (coinCapApiKey == null || coinCapApiKey.isBlank()) {
            throw new InvalidCoinCapApiKeyException("CoinCap API key is required but not configured.");
        }

        fetchAllAssets()
                .doOnNext(coinCapAssetsResponse -> {
                    List<AssetSymbolMapping> mappings = coinCapAssetsResponse.data().stream()
                            .map(assetData -> AssetSymbolMapping.builder()
                                    .id(assetData.id())
                                    .symbol(assetData.symbol())
                                    .build())
                            .toList();
                    assetSymbolMappingRepository.saveAll(mappings);
                    log.info("Saved {} asset mappings.", mappings.size());
                })
                .doOnError(e -> log.error("Error updating asset mappings on startup: {}", e.getMessage()))
                .subscribe();
    }

    public Mono<CoinCapAssetResponse> fetchLatestPrice(String tokenId) {
        log.info("Fetching CoinCapApi /assets/{} data...", tokenId);
        return coinCapClient.get()
                .uri(coinCapApiUrl + "/assets/{slug}", tokenId.toLowerCase())
                .header("accept", "application/json")
                .header("Authorization", "Bearer " + coinCapApiKey)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, CoinCapAssetUpdateService::handle4xxCoinCapError)
                .onStatus(HttpStatusCode::is5xxServerError, CoinCapAssetUpdateService::handle5xxCoinCapError)
                .bodyToMono(CoinCapAssetResponse.class)
                .doOnNext(response -> log.info("Received CoinCapResponse: {}", response))
                .doOnError(e -> log.error("Error fetching token price for {}: {}", tokenId, e.getMessage()));
    }

    public Mono<CoinCapAssetsResponse> fetchAllAssets() {
        log.info("Fetching CoinCapApi /assets data containing all asset types...");
        return coinCapClient.get()
                .uri(coinCapApiUrl + "/assets")
                .header("accept", "application/json")
                .header("Authorization", "Bearer " + coinCapApiKey)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, CoinCapAssetUpdateService::handle4xxCoinCapError)
                .onStatus(HttpStatusCode::is5xxServerError, CoinCapAssetUpdateService::handle5xxCoinCapError)
                .bodyToMono(CoinCapAssetsResponse.class)
                .doOnNext(response -> log.info("Fetched {} assets.", response.data().size()))
                .doOnError(e -> log.error("Error fetching assets: {}", e.getMessage()));
    }

    @Scheduled(fixedDelayString = "${coincap.api.refresh-rate}")
    public void updateAssetPrices() {
        log.info("Initiating Scheduled updateAssetPrices job, next run in {} s", Duration.ofMillis(Long.parseLong(refreshRate)).toSeconds());
        List<String> assetsToUpdate = assetRepository.findDistinctAssetNames();
        if (assetsToUpdate.isEmpty()) {
            log.info("No asset prices to update - skipping scheduled task.");
            return;
        }

        log.info("Updating prices for {} distinct assets: {}", assetsToUpdate.size(), assetsToUpdate);
        Flux.fromIterable(assetsToUpdate)
                .flatMap(this::fetchLatestPriceWithMapping, maxThreads)
                .flatMap(this::updateAssets)
                .collectList()
                .subscribe(
                        updatedCountList -> log.info("Updated asset prices for {} distinct tokens", updatedCountList.size()),
                        error -> log.error("Error updating asset prices: {}", error.getMessage())
                );
    }

    private Mono<Tuple2<String, BigDecimal>> fetchLatestPriceWithMapping(String assetName) {
        CoinCapAssetResponse response = fetchLatestPrice(assetName.toLowerCase())
                .blockOptional()
                .orElseThrow(() -> new IllegalArgumentException("Token price not found for: " + assetName));

        return Mono.just(Tuples.of(assetName, new BigDecimal(response.data().priceUsd())));
    }

    private Mono<Integer> updateAssets(Tuple2<String, BigDecimal> priceUpdate) {
        return Mono.fromCallable(() -> assetRepository.updatePriceUsdByName(priceUpdate.getT1(), priceUpdate.getT2()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Mono<Throwable> handle4xxCoinCapError(ClientResponse response) {
        if (response.statusCode().equals(HttpStatus.FORBIDDEN)) {
            return response.bodyToMono(String.class)
                    .flatMap(body ->
                            Mono.error(new InvalidCoinCapApiKeyException(
                                    "Invalid API Key: 403 Forbidden. Response: " + body)));
        } else {
            return response.bodyToMono(String.class)
                    .flatMap(body ->
                            Mono.error(new RuntimeException(
                                    "Client error: " + response.statusCode() + ". Body: " + body)));
        }
    }

    private static Mono<Throwable> handle5xxCoinCapError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body ->
                        Mono.error(new RuntimeException(
                                "Server error: " + response.statusCode() + ". Body: " + body)));
    }
}
