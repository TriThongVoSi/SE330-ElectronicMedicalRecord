package org.example.BenhAnDienTu.catalog.application;

import java.util.Optional;
import org.example.BenhAnDienTu.catalog.api.CatalogApi;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugListQuery;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugPageView;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugUpsertCommand;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugView;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceListQuery;
import org.example.BenhAnDienTu.catalog.api.CatalogServicePageView;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceUpsertCommand;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceView;
import org.example.BenhAnDienTu.catalog.infrastructure.CatalogReadModelAdapter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogApplicationService implements CatalogApi {

  private final CatalogReadModelAdapter readModelAdapter;

  public CatalogApplicationService(CatalogReadModelAdapter readModelAdapter) {
    this.readModelAdapter = readModelAdapter;
  }

  @Override
  public CatalogServicePageView listServices(CatalogServiceListQuery query) {
    return readModelAdapter.listServices(query);
  }

  @Override
  public CatalogDrugPageView listDrugs(CatalogDrugListQuery query) {
    return readModelAdapter.listDrugs(query);
  }

  @Override
  public CatalogServiceView createService(CatalogServiceUpsertCommand command) {
    try {
      return readModelAdapter.createService(command);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Service code/name already exists.", exception);
    }
  }

  @Override
  public CatalogServiceView updateService(String serviceId, CatalogServiceUpsertCommand command) {
    try {
      return readModelAdapter
          .updateService(serviceId, command)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Service does not exist: " + serviceId));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Service code/name already exists.", exception);
    }
  }

  @Override
  public CatalogDrugView createDrug(CatalogDrugUpsertCommand command) {
    try {
      return readModelAdapter.createDrug(command);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Drug code/name already exists.", exception);
    }
  }

  @Override
  public CatalogDrugView updateDrug(String drugId, CatalogDrugUpsertCommand command) {
    try {
      return readModelAdapter
          .updateDrug(drugId, command)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Drug does not exist: " + drugId));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Drug code/name already exists.", exception);
    }
  }

  @Override
  public Optional<CatalogServiceView> findService(String serviceCode) {
    return readModelAdapter.findByCode(serviceCode);
  }
}
