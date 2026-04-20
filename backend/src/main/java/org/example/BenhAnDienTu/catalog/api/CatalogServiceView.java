package org.example.BenhAnDienTu.catalog.api;

/** Catalog view exposed for cross-module read access. */
public record CatalogServiceView(
    String id, String serviceCode, String serviceName, String serviceType, boolean isActive) {}
