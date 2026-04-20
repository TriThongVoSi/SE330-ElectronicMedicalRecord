package org.example.BenhAnDienTu.catalog.api;

import java.util.Optional;

/** Public contract for querying catalog service definitions. */
public interface CatalogApi {

  CatalogServicePageView listServices(CatalogServiceListQuery query);

  CatalogDrugPageView listDrugs(CatalogDrugListQuery query);

  CatalogServiceView createService(CatalogServiceUpsertCommand command);

  CatalogServiceView updateService(String serviceId, CatalogServiceUpsertCommand command);

  CatalogDrugView createDrug(CatalogDrugUpsertCommand command);

  CatalogDrugView updateDrug(String drugId, CatalogDrugUpsertCommand command);

  Optional<CatalogServiceView> findService(String serviceCode);
}
