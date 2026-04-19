package org.example.BenhAnDienTu.catalog.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Drug catalog projection. */
public record CatalogDrugView(
    String id,
    String drugCode,
    String drugName,
    String manufacturer,
    LocalDate expiryDate,
    String unit,
    BigDecimal price,
    int stockQuantity,
    boolean isActive) {}
