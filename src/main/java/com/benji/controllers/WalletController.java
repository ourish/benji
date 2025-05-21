package com.benji.controllers;

import com.benji.controllers.dto.*;
import com.benji.services.WalletService;
import com.benji.services.WalletSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/wallets")
@Tag(name = "Wallet Management", description = "Endpoints for wallet operations")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletSimulationService walletSimulationService;

    @Operation(
            summary = "Create a new wallet",
            description = "Creates a wallet associated with the user's email.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Wallet successfully created",
                            content = @Content(schema = @Schema(implementation = CreateUserWalletResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload")
            }
    )
    @PostMapping
    public ResponseEntity<CreateUserWalletResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest request) {
        CreateUserWalletResponse responseBody = walletService.createWallet(request.email());
        return ResponseEntity.created(URI.create("/api/wallets")).body(responseBody);
    }

    @Operation(
            summary = "Retrieve wallet information",
            description = "Fetches wallet details using the wallet ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Wallet details retrieved",
                            content = @Content(schema = @Schema(implementation = CreateUserWalletResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Wallet not found")
            }
    )
    @GetMapping("/{walletId}")
    public ResponseEntity<CreateUserWalletResponse> getWalletInformation(
            @PathVariable("walletId") Long walletId) {
        CreateUserWalletResponse responseBody = walletService.getWalletInformation(walletId);
        return ResponseEntity.ok(responseBody);

    }

    @Operation(
            summary = "Add an asset to a wallet",
            description = "Adds a new asset to the wallet or increases its quantity if it already exists.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Asset added to wallet",
                            content = @Content(schema = @Schema(implementation = CreateUserWalletResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid asset symbol or quantity")
            }
    )
    @PostMapping("/{walletId}/assets")
    public ResponseEntity<CreateUserWalletResponse> addAssetToWallet(
            @PathVariable("walletId") Long walletId,
            @Valid @RequestBody AddAssetRequest request) {
        CreateUserWalletResponse createUserWalletResponse = walletService.addAssetToWallet(walletId, request);

        return ResponseEntity.ok(createUserWalletResponse);
    }

    @Operation(
            summary = "Simulate wallet performance",
            description = "Runs a wallet simulation based on asset price evolution.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Simulation completed",
                            content = @Content(schema = @Schema(implementation = WalletSimulationResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid simulation parameters")
            }
    )
    @PostMapping("/simulate")
    public ResponseEntity<WalletSimulationResponse> simulateWallet(
            @Valid @RequestBody WalletSimulationRequest request) {
        WalletSimulationResponse response = walletSimulationService.simulateWalletEvolution(request);

        return ResponseEntity.ok(response);
    }
}
