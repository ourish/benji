package com.benji.repositories;

import com.benji.models.entities.Asset;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    @Query("SELECT DISTINCT a.name FROM Asset a")
    List<String> findDistinctAssetNames();

    @Modifying
    @Transactional
    @Query("UPDATE Asset a SET a.priceUsd = :price WHERE a.name = :name")
    int updatePriceUsdByName(@Param("name") String name, @Param("price") BigDecimal price);

}
