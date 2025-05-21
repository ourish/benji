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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final WalletRepository walletRepository;
    private final AssetSymbolMappingRepository assetSymbolMappingRepository;

    private final CoinCapAssetUpdateService coinCapAssetUpdateService;

    @Transactional
    public CreateUserWalletResponse createWallet(String email) {
        log.info("createWallet request received for email: {}", email);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            return newUser;
        });

        if (user.getWallet() != null) {
            log.error("A wallet for this email already exists");
            throw new WalletAlreadyExistsException("A wallet for this email already exists");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        user.setWallet(wallet);

        userRepository.save(user);

        log.info("Wallet created for user with email: {}", email);
        return new CreateUserWalletResponse(wallet.getId().toString(), user.getEmail(), List.of());
    }


    public CreateUserWalletResponse addAssetToWallet(Long walletId, AddAssetRequest request) {
        log.info("addAssetToWallet request received for walletId: {}", walletId);
        log.info("AddAssetRequest: {}", request);
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> {
            log.error("Wallet not found!");
            return new WalletDoesNotExistException("Wallet not found!");
        });

        String requestSymbol = request.symbol();
        log.info("Checking if asset symbol exists on assetName <-> assetSymbol map for symbol: {}", requestSymbol);
        AssetSymbolMapping assetSymbolMapping = assetSymbolMappingRepository.findBySymbol(requestSymbol)
                .orElseThrow(() -> {
                    log.error("Invalid incoming Symbol, no Asset exists for Symbol: " + requestSymbol);
                    return new AssetDoesNotExistException("Invalid incoming Symbol, no Asset exists for Symbol: " + requestSymbol);
                });

        log.info("Fetching Asset Symbol {}, latest price from CoinCap API...", assetSymbolMapping.getId());
        CoinCapAssetResponse coinCapAssetResponse = coinCapAssetUpdateService.fetchLatestPrice(assetSymbolMapping.getId())
                .blockOptional()
                .orElseThrow(() -> {
                    log.error("CoinCap Data not retrived for : " + requestSymbol);
                    return new NoCoinCapApiResponseException("CoinCap Data not retrived for : " + requestSymbol);
                });

        log.info("Updating Asset Data on User Wallet...");
        CoinCapData coinCapAssetData = coinCapAssetResponse.data();

        Optional<Asset> existingAsset = wallet.getAssets().stream()
                .filter(asset -> asset.getSymbol().equals(requestSymbol))
                .findFirst();

        if(existingAsset.isPresent()) {
            Asset assetToUpdate = existingAsset.get();
            assetToUpdate.setQuantity(assetToUpdate.getQuantity().add(request.quantity()));

            assetRepository.save(assetToUpdate);
        } else {
            Asset asset = new Asset();
            asset.setSymbol(requestSymbol);
            asset.setName(coinCapAssetData.id());
            asset.setPriceUsd(new BigDecimal(coinCapAssetData.priceUsd()));
            asset.setQuantity(request.quantity());
            asset.setWallet(wallet);

            assetRepository.save(asset);
            wallet.getAssets().add(asset);
            walletRepository.save(wallet);
        }

        log.info("Asset Updated on User Wallet!");
        return new CreateUserWalletResponse(wallet.getId().toString(), wallet.getUser().getEmail(), wallet.getAssets().stream().map(CreateUserWalletResponse.UserAssets::fromAssetEntity).toList());
    }


    public CreateUserWalletResponse getWalletInformation(Long walletId) {
        log.info("getWalletInformation request received for walletId {}", walletId);
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new WalletDoesNotExistException("Wallet not found!"));

        return new CreateUserWalletResponse(wallet.getId().toString(), wallet.getUser().getEmail(), wallet.getAssets().stream().map(CreateUserWalletResponse.UserAssets::fromAssetEntity).toList());
    }
}
