package org.example.BenhAnDienTu.catalog.api;

/** Drug catalog pagination query. */
public record CatalogDrugListQuery(int page, int size, String search, Boolean isActive) {}
