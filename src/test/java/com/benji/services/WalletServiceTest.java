package com.benji.services;

import com.benji.coincap.service.CoinCapAssetUpdateService;
import com.benji.coincap.service.dto.CoinCapAssetResponse;
import com.benji.coincap.service.dto.CoinCapData;
import com.benji.controllers.dto.AddAssetRequest;
import com.benji.controllers.dto.CreateUserWalletResponse;
import com.benji.exception.AssetDoesNotExistException;
import com.benji.exception.NoCoinCapApiResponseException;
import com.benji.exception.WalletAlreadyExistsException;
import com.benji.exception.WalletDoesNotExistException;
import com.benji.models.entities.Asset;
import com.benji.models.entities.AssetSymbolMapping;
import com.benji.models.entities.User;
import com.benji.models.entities.Wallet;
import com.benji.repositories.AssetRepository;
import com.benji.repositories.AssetSymbolMappingRepository;
import com.benji.repositories.UserRepository;
import com.benji.repositories.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetSymbolMappingRepository assetSymbolMappingRepository;

    @MockBean
    private CoinCapAssetUpdateService coinCapAssetUpdateService;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        walletRepository.deleteAll();
        assetRepository.deleteAll();
        assetSymbolMappingRepository.deleteAll();
    }

    @Test
    public void createWallet_whenCreatingNewUserWallet_thenUserWalletIsCreatedCorrectly() {
        String email = "newuser@example.com";

        CreateUserWalletResponse response = walletService.createWallet(email);
        assertNotNull(response);
        assertEquals("newuser@example.com", response.userEmail());
        assertEquals(0, response.assets().size());

        Optional<User> user = userRepository.findByEmail(email);
        assertTrue(user.isPresent(), "User should be saved in the database");
        User userEntity = user.get();
        assertNotNull(userEntity.getWallet(), "User should have an associated wallet");
        assertEquals(Long.valueOf(response.walletId()), userEntity.getWallet().getId());
    }

    @Test
    public void createWallet_whenCreatingNewUserWallet_andUserAlreadyExists_thenCrash() {
        String email = "newuser@example.com";
        User user = setupMockUser(email);
        userRepository.save(user);

        WalletAlreadyExistsException exception =
                assertThrows(WalletAlreadyExistsException.class, () -> walletService.createWallet(email));

        assertEquals("A wallet for this email already exists", exception.getMessage());
    }

    @Test
    public void addAssetToWallet_whenNewAssetAdded_thenPersistedCorrectly() {
        String email = "testuser@example.com";
        String assetSymbol = "ETH";
        BigDecimal quantity = BigDecimal.valueOf(5);

        User user = setupMockUser(email);
        userRepository.save(user);

        AssetSymbolMapping mapping = new AssetSymbolMapping();
        mapping.setId("ethereum");
        mapping.setSymbol(assetSymbol);
        assetSymbolMappingRepository.save(mapping);

        CoinCapData coinCapData = new CoinCapData("ethereum", "ETH", "2000.00");
        CoinCapAssetResponse mockResponse = new CoinCapAssetResponse(coinCapData);
        when(coinCapAssetUpdateService.fetchLatestPrice(mapping.getId()))
                .thenReturn(Mono.just(mockResponse));

        AddAssetRequest request = new AddAssetRequest(assetSymbol, quantity);
        CreateUserWalletResponse response = walletService.addAssetToWallet(user.getWallet().getId(), request);

        assertNotNull(response);
        assertEquals(email, response.userEmail());
        assertEquals(1, response.assets().size());
        assertEquals(assetSymbol, response.assets().get(0).symbol());
        assertEquals(quantity.toString(), response.assets().get(0).quantity());

        Wallet updatedWallet = walletRepository.findById(user.getWallet().getId()).orElseThrow();
        assertEquals(1, updatedWallet.getAssets().size());
    }

    @Test
    public void addAssetToWallet_whenExistingAssetUpdated_thenQuantityIncreases() {
        String email = "testuser@example.com";
        String assetId = "bitcoin";
        String assetSymbol = "BTC";
        String priceUsd = "40000.00";
        BigDecimal initialQuantity = BigDecimal.valueOf(2);
        BigDecimal additionalQuantity = BigDecimal.valueOf(3);

        User user = setupMockUser(email);
        userRepository.save(user);

        Asset existingAsset = new Asset();
        existingAsset.setName(assetId);
        existingAsset.setSymbol(assetSymbol);
        existingAsset.setQuantity(initialQuantity);
        existingAsset.setWallet(user.getWallet());
        existingAsset.setPriceUsd(new BigDecimal(priceUsd));
        user.getWallet().getAssets().add(existingAsset);
        walletRepository.save(user.getWallet());
        assetRepository.save(existingAsset);

        AssetSymbolMapping mapping = new AssetSymbolMapping();
        mapping.setId(assetId);
        mapping.setSymbol(assetSymbol);
        assetSymbolMappingRepository.save(mapping);

        CoinCapData coinCapData = new CoinCapData(assetId, assetSymbol, priceUsd);
        CoinCapAssetResponse mockResponse = new CoinCapAssetResponse(coinCapData);
        when(coinCapAssetUpdateService.fetchLatestPrice(mapping.getId()))
                .thenReturn(Mono.just(mockResponse));

        AddAssetRequest request = new AddAssetRequest(assetSymbol, additionalQuantity);
        CreateUserWalletResponse response = walletService.addAssetToWallet(user.getWallet().getId(), request);

        assertEquals(1, response.assets().size());
        assertEquals(initialQuantity.add(additionalQuantity).toString(), response.assets().get(0).quantity());

        Wallet updatedWallet = walletRepository.findById(user.getWallet().getId()).orElseThrow();
        assertEquals(initialQuantity.add(additionalQuantity), updatedWallet.getAssets().get(0).getQuantity());
    }

    @Test
    public void addAssetToWallet_whenWalletNotFound_thenExceptionThrown() {
        Long nonExistentWalletId = 999L;
        AddAssetRequest request = new AddAssetRequest("ETH", BigDecimal.valueOf(3));

        WalletDoesNotExistException exception = assertThrows(
                WalletDoesNotExistException.class, () -> walletService.addAssetToWallet(nonExistentWalletId, request));

        assertEquals("Wallet not found!", exception.getMessage());
    }

    @Test
    public void addAssetToWallet_whenSymbolNotFound_thenExceptionThrown() {
        String email = "testuser@example.com";
        String assetSymbol = "UNKNOWN";

        User user = setupMockUser(email);
        userRepository.save(user);

        AddAssetRequest request = new AddAssetRequest(assetSymbol, BigDecimal.valueOf(3));

        AssetDoesNotExistException exception = assertThrows(AssetDoesNotExistException.class,
                () -> walletService.addAssetToWallet(user.getWallet().getId(), request));

        assertEquals("Invalid incoming Symbol, no Asset exists for Symbol: " + assetSymbol, exception.getMessage());
    }

    @Test
    public void addAssetToWallet_whenTokenPriceNotFound_thenExceptionThrown() {
        String email = "pricefail@example.com";
        String assetSymbol = "ETH";
        User user = setupMockUser(email);
        userRepository.save(user);

        AssetSymbolMapping mapping = new AssetSymbolMapping();
        mapping.setId("ethereum");
        mapping.setSymbol(assetSymbol);
        assetSymbolMappingRepository.save(mapping);

        when(coinCapAssetUpdateService.fetchLatestPrice(mapping.getId()))
                .thenReturn(Mono.empty());

        AddAssetRequest request = new AddAssetRequest(assetSymbol, BigDecimal.valueOf(1));
        NoCoinCapApiResponseException exception =
                assertThrows(NoCoinCapApiResponseException.class, () -> walletService.addAssetToWallet(user.getWallet().getId(), request));

        assertEquals("CoinCap Data not retrived for : " + assetSymbol, exception.getMessage());
    }

    @Test
    public void getWalletInformation_whenWalletExists_thenReturnWalletInfo() {
        String email = "info@example.com";

        CreateUserWalletResponse createResponse = walletService.createWallet(email);
        Long walletId = Long.valueOf(createResponse.walletId());

        CreateUserWalletResponse response = walletService.getWalletInformation(walletId);
        assertNotNull(response);
        assertEquals(email, response.userEmail());
        assertEquals(walletId.toString(), response.walletId());
    }


    private static User setupMockUser(String email) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setAssets(new ArrayList<>());
        wallet.setUser(user);
        user.setWallet(wallet);
        return user;
    }
}
