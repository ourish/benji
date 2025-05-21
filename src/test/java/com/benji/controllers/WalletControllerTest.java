package com.benji.controllers;

import com.benji.controllers.dto.*;
import com.benji.services.WalletService;
import com.benji.services.WalletSimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@WebMvcTest(WalletController.class)
public class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private WalletSimulationService walletSimulationService;

    @Test
    public void createWallet_ValidRequest_ReturnsCreatedWallet() throws Exception {
        CreateWalletRequest req = new CreateWalletRequest("test@example.com");
        CreateUserWalletResponse res = new CreateUserWalletResponse("1", "test@example.com", List.of());

        when(walletService.createWallet("test@example.com")).thenReturn(res);

        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/wallets"))
                .andExpect(jsonPath("$.walletId").value("1"))
                .andExpect(jsonPath("$.userEmail").value("test@example.com"));
    }

    @Test
    public void createWallet_InvalidRequest_ReturnsBadRequest() throws Exception {
        CreateWalletRequest req = new CreateWalletRequest("");

        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getWalletInformation_ExistingWallet_ReturnsWalletInformation() throws Exception {
        CreateUserWalletResponse res = new CreateUserWalletResponse("1", "test@example.com", List.of());
        when(walletService.getWalletInformation(1L)).thenReturn(res);

        mockMvc.perform(get("/api/wallets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value("1"))
                .andExpect(jsonPath("$.userEmail").value("test@example.com"));
    }

    @Test
    public void addAssetToWallet_ValidRequest_ReturnsUpdatedWallet() throws Exception {
        AddAssetRequest req = new AddAssetRequest("ETH", BigDecimal.valueOf(5));
        CreateUserWalletResponse res = new CreateUserWalletResponse("1", "test@example.com", List.of());
        when(walletService.addAssetToWallet(anyLong(), any(AddAssetRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/wallets/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value("1"))
                .andExpect(jsonPath("$.userEmail").value("test@example.com"));
    }

    @Test
    public void addAssetToWallet_InvalidRequest_ReturnsBadRequest() throws Exception {
        AddAssetRequest req = new AddAssetRequest("", BigDecimal.valueOf(-5));

        mockMvc.perform(post("/api/wallets/1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void simulateWallet_ValidRequest_ReturnsSimulationResponse() throws Exception {
        WalletSimulationRequest.SimulatedAsset asset = new WalletSimulationRequest.SimulatedAsset("BTC", new BigDecimal("2"), new BigDecimal("10000"));
        WalletSimulationRequest req = new WalletSimulationRequest(List.of(asset));
        WalletSimulationResponse res = new WalletSimulationResponse(
                new BigDecimal("50000"), "BTC", new BigDecimal("10.0"), "ETH", new BigDecimal("-5.0")
        );
        when(walletSimulationService.simulateWalletEvolution(any(WalletSimulationRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/wallets/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(50000))
                .andExpect(jsonPath("$.bestAsset").value("BTC"))
                .andExpect(jsonPath("$.bestPerformance").value(10.0))
                .andExpect(jsonPath("$.worstAsset").value("ETH"))
                .andExpect(jsonPath("$.worstPerformance").value(-5.0));
    }

    @Test
    public void simulateWallet_InvalidRequest_ReturnsBadRequest() throws Exception {
        WalletSimulationRequest req = new WalletSimulationRequest(List.of());

        mockMvc.perform(post("/api/wallets/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
