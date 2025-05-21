package com.benji.controllers.dto;

import com.benji.models.entities.Asset;

import java.math.BigDecimal;
import java.util.List;

public record CreateUserWalletResponse(
     String walletId,
     String userEmail,
     List<UserAssets> assets
) {
    public record UserAssets(
            String symbol,
            String quantity,
            BigDecimal price,
            BigDecimal value
    ) {
        public static UserAssets fromAssetEntity(Asset assetEntity) {
            BigDecimal quantity = assetEntity.getQuantity();
            BigDecimal price = assetEntity.getPriceUsd();
            return new UserAssets(
                    assetEntity.getSymbol(),
                    quantity.toString(),
                    price,
                    quantity.multiply(price)
            );
        }
    }
}
