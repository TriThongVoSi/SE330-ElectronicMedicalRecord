package org.example.BenhAnDienTu.catalog.api;

import java.util.List;

/** Paged services response. */
public record CatalogServicePageView(
    List<CatalogServiceView> items, int page, int size, long totalItems, int totalPages) {}
