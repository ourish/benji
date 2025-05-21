package com.benji.coincap.service.dto;

import java.util.List;

public record CoinCapAssetsResponse(
        List<CoinCapData> data
) {
}
