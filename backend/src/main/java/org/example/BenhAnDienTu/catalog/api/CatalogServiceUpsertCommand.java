package org.example.BenhAnDienTu.catalog.api;

/** Service create/update command. */
public record CatalogServiceUpsertCommand(
    String serviceCode, String serviceName, String serviceType, boolean isActive) {}
