package org.example.BenhAnDienTu.catalog.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Drug create/update command. */
public record CatalogDrugUpsertCommand(
    String drugCode,
    String drugName,
    String manufacturer,
    LocalDate expiryDate,
    String unit,
    BigDecimal price,
    int stockQuantity,
    boolean isActive) {}
