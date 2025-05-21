package com.benji.repositories;

import com.benji.models.entities.AssetSymbolMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetSymbolMappingRepository extends JpaRepository<AssetSymbolMapping, String> {

    Optional<AssetSymbolMapping> findBySymbol(String symbol);
}
