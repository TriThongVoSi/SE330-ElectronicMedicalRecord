package org.example.BenhAnDienTu.catalog.api;

import java.util.List;

/** Paged drugs response. */
public record CatalogDrugPageView(
    List<CatalogDrugView> items, int page, int size, long totalItems, int totalPages) {}
