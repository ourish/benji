package com.benji.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "asset_symbol_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSymbolMapping {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String symbol;
}
