package org.example.BenhAnDienTu.catalog.api;

/** Service catalog pagination query. */
public record CatalogServiceListQuery(int page, int size, String search, Boolean isActive) {}
