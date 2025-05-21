package com.benji.services;

import com.benji.coincap.service.CoinCapAssetUpdateService;
import com.benji.coincap.service.dto.CoinCapAssetResponse;
import com.benji.controllers.dto.WalletSimulationRequest;
import com.benji.controllers.dto.WalletSimulationResponse;
import com.benji.exception.AssetDoesNotExistException;
import com.benji.exception.NoCoinCapApiResponseException;
import com.benji.models.entities.AssetSymbolMapping;
import com.benji.repositories.AssetSymbolMappingRepository;
import com.benji.services.models.SimulatedAssetEvaluation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletSimulationService {

    private final CoinCapAssetUpdateService coinCapAssetUpdateService;
    private final AssetSymbolMappingRepository assetSymbolMappingRepository;

    public WalletSimulationResponse simulateWalletEvolution(WalletSimulationRequest request) {
        log.info("simulateWalletEvolution request received");
        List<SimulatedAssetEvaluation> evaluatedAssets = new ArrayList<>();

        for (WalletSimulationRequest.SimulatedAsset simulatedAsset : request.assets()) {
            String simulationSymbol = simulatedAsset.symbol();
            BigDecimal quantity = simulatedAsset.quantity();
            BigDecimal simulatedValue = simulatedAsset.value();

            log.info("Fetching coinCap asset data for {}...", simulationSymbol);
            AssetSymbolMapping assetSymbolMapping = assetSymbolMappingRepository.findBySymbol(simulationSymbol)
                    .orElseThrow(() -> {
                        log.error("Invalid incoming Symbol, no Asset exists for Symbol: " + simulationSymbol);
                        return new AssetDoesNotExistException("Invalid incoming Symbol, no Asset exists for Symbol: " + simulationSymbol);
                    });

            String requestSymbol = assetSymbolMapping.getId();
            CoinCapAssetResponse coinCapAssetResponse = coinCapAssetUpdateService.fetchLatestPrice(requestSymbol)
                    .blockOptional()
                    .orElseThrow(() -> {
                        log.error("CoinCap Data not retrieved for : " + requestSymbol);
                        return new NoCoinCapApiResponseException("CoinCap Data not retrieved for : " + requestSymbol);
                    });

            log.info("Calculating asset performance...");
            BigDecimal currentValue = new BigDecimal(coinCapAssetResponse.data().priceUsd());

            // performance = (currentValue - simulatedValue) / simulatedValue * 100
            BigDecimal performance = currentValue.subtract(simulatedValue)
                    .divide(simulatedValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal currentWorth = currentValue.multiply(quantity);

            log.info("Asset performance calculated for {}", simulationSymbol);
            evaluatedAssets.add(new SimulatedAssetEvaluation(simulationSymbol, currentValue, performance, currentWorth));
        }

        log.info("Comparing all asset performances...");
        BigDecimal totalCurrentWorth = evaluatedAssets.stream()
                .map(SimulatedAssetEvaluation::currentWorth)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SimulatedAssetEvaluation bestAsset = evaluatedAssets.stream()
                .max(Comparator.comparing(SimulatedAssetEvaluation::performance))
                .orElse(null);

        SimulatedAssetEvaluation worstAsset = evaluatedAssets.stream()
                .min(Comparator.comparing(SimulatedAssetEvaluation::performance))
                .orElse(null);

        BigDecimal bestPerf = bestAsset != null ? bestAsset.performance().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal worstPerf = worstAsset != null ? worstAsset.performance().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new WalletSimulationResponse(
                totalCurrentWorth.setScale(2, RoundingMode.HALF_UP),
                bestAsset != null ? bestAsset.symbol() : "",
                bestPerf,
                worstAsset != null ? worstAsset.symbol() : "",
                worstPerf
        );
    }
}