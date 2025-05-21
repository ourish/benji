package com.benji.services;

import com.benji.coincap.service.CoinCapAssetUpdateService;
import com.benji.coincap.service.dto.CoinCapAssetResponse;
import com.benji.coincap.service.dto.CoinCapData;
import com.benji.controllers.dto.WalletSimulationRequest;
import com.benji.controllers.dto.WalletSimulationResponse;
import com.benji.exception.AssetDoesNotExistException;
import com.benji.exception.NoCoinCapApiResponseException;
import com.benji.models.entities.AssetSymbolMapping;
import com.benji.repositories.AssetSymbolMappingRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WalletSimulationServiceTest {

    @Autowired
    private WalletSimulationService walletSimulationService;

    @Autowired
    private AssetSymbolMappingRepository assetSymbolMappingRepository;

    @MockBean
    private CoinCapAssetUpdateService coinCapAssetUpdateService;

    @Test
    public void simulateWalletEvolution_whenDataValid_thenReturnCorrectResponse() {
        AssetSymbolMapping mappingEth = new AssetSymbolMapping();
        mappingEth.setId("eth-mapping");
        mappingEth.setSymbol("ETH");
        assetSymbolMappingRepository.save(mappingEth);

        AssetSymbolMapping mappingBtc = new AssetSymbolMapping();
        mappingBtc.setId("btc-mapping");
        mappingBtc.setSymbol("BTC");
        assetSymbolMappingRepository.save(mappingBtc);

        CoinCapData coinCapDataEth = new CoinCapData("eth-mapping", "ETH", "2200.00");
        CoinCapAssetResponse responseEth = new CoinCapAssetResponse(coinCapDataEth);
        when(coinCapAssetUpdateService.fetchLatestPrice("eth-mapping")).thenReturn(Mono.just(responseEth));

        CoinCapData coinCapDataBtc = new CoinCapData("btc-mapping", "BTC", "38000.00");
        CoinCapAssetResponse responseBtc = new CoinCapAssetResponse(coinCapDataBtc);
        when(coinCapAssetUpdateService.fetchLatestPrice("btc-mapping")).thenReturn(Mono.just(responseBtc));

        List<WalletSimulationRequest.SimulatedAsset> simulatedAssets = new ArrayList<>();
        simulatedAssets.add(new WalletSimulationRequest.SimulatedAsset("ETH", BigDecimal.valueOf(2), BigDecimal.valueOf(2000)));
        simulatedAssets.add(new WalletSimulationRequest.SimulatedAsset("BTC", new BigDecimal("0.1"), new BigDecimal("40000")));
        WalletSimulationRequest request = new WalletSimulationRequest(simulatedAssets);

        WalletSimulationResponse simulationResponse = walletSimulationService.simulateWalletEvolution(request);

        assertNotNull(simulationResponse);
        assertEquals(new BigDecimal("8200.00"), simulationResponse.total());
        assertEquals("ETH", simulationResponse.bestAsset());
        assertEquals(new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP), simulationResponse.bestPerformance());
        assertEquals("BTC", simulationResponse.worstAsset());
        assertEquals(new BigDecimal("-5.00").setScale(2, RoundingMode.HALF_UP), simulationResponse.worstPerformance());
    }

    @Test
    public void simulateWalletEvolution_whenAssetSymbolMappingNotFound_thenThrowException() {
        List<WalletSimulationRequest.SimulatedAsset> simulatedAssets = new ArrayList<>();
        String nonExistent = "NON_EXISTENT";
        simulatedAssets.add(new WalletSimulationRequest.SimulatedAsset(nonExistent, BigDecimal.ONE, BigDecimal.valueOf(1000)));
        WalletSimulationRequest request = new WalletSimulationRequest(simulatedAssets);

        AssetDoesNotExistException exception = assertThrows(AssetDoesNotExistException.class, () -> {
            walletSimulationService.simulateWalletEvolution(request);
        });
        assertEquals("Invalid incoming Symbol, no Asset exists for Symbol: " + nonExistent, exception.getMessage());
    }

    @Test
    public void simulateWalletEvolution_whenTokenPriceNotFound_thenThrowException() {
        String tokenId = "eth-mapping";
        String tokenSymbol = "ETH";
        AssetSymbolMapping mapping = new AssetSymbolMapping();
        mapping.setId(tokenId);
        mapping.setSymbol(tokenSymbol);
        assetSymbolMappingRepository.save(mapping);

        when(coinCapAssetUpdateService.fetchLatestPrice(tokenId)).thenReturn(Mono.empty());

        List<WalletSimulationRequest.SimulatedAsset> simulatedAssets = new ArrayList<>();
        simulatedAssets.add(new WalletSimulationRequest.SimulatedAsset(tokenSymbol, BigDecimal.ONE, BigDecimal.valueOf(2000)));
        WalletSimulationRequest request = new WalletSimulationRequest(simulatedAssets);

        NoCoinCapApiResponseException exception = assertThrows(NoCoinCapApiResponseException.class, () -> {
            walletSimulationService.simulateWalletEvolution(request);
        });
        assertEquals("CoinCap Data not retrieved for : " + tokenId, exception.getMessage());
    }

    @Test
    public void simulateWalletEvolution_whenSimulatedValueZero_thenThrowException() {
        AssetSymbolMapping mapping = new AssetSymbolMapping();
        mapping.setId("eth-mapping");
        mapping.setSymbol("ETH");
        assetSymbolMappingRepository.save(mapping);

        CoinCapData coinCapData = new CoinCapData("eth-mapping", "ETH", "2000.00");
        when(coinCapAssetUpdateService.fetchLatestPrice("eth-mapping"))
                .thenReturn(Mono.just(new CoinCapAssetResponse(coinCapData)));

        List<WalletSimulationRequest.SimulatedAsset> simulatedAssets = List.of(
                new WalletSimulationRequest.SimulatedAsset("ETH", BigDecimal.ONE, BigDecimal.ZERO)
        );
        WalletSimulationRequest request = new WalletSimulationRequest(simulatedAssets);

        assertThrows(ArithmeticException.class, () -> walletSimulationService.simulateWalletEvolution(request));
    }
}
