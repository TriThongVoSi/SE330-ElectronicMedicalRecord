package org.example.BenhAnDienTu.catalog.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.BenhAnDienTu.catalog.api.CatalogApi;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugListQuery;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugPageView;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugUpsertCommand;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugView;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceListQuery;
import org.example.BenhAnDienTu.catalog.api.CatalogServicePageView;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceUpsertCommand;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceView;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class CatalogController {

  private final CatalogApi catalogApi;

  public CatalogController(CatalogApi catalogApi) {
    this.catalogApi = catalogApi;
  }

  @GetMapping("/api/services")
  public CatalogServicePageView listServices(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean isActive) {
    return catalogApi.listServices(new CatalogServiceListQuery(page, size, search, isActive));
  }

  @PostMapping("/api/services")
  public CatalogServiceView createService(@Valid @RequestBody ServiceUpsertRequest request) {
    return catalogApi.createService(request.toCommand());
  }

  @PutMapping("/api/services/{id}")
  public CatalogServiceView updateService(
      @PathVariable("id") String id, @Valid @RequestBody ServiceUpsertRequest request) {
    return catalogApi.updateService(id, request.toCommand());
  }

  @GetMapping("/api/drugs")
  public CatalogDrugPageView listDrugs(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean isActive) {
    return catalogApi.listDrugs(new CatalogDrugListQuery(page, size, search, isActive));
  }

  @PostMapping("/api/drugs")
  public CatalogDrugView createDrug(@Valid @RequestBody DrugUpsertRequest request) {
    return catalogApi.createDrug(request.toCommand());
  }

  @PutMapping("/api/drugs/{id}")
  public CatalogDrugView updateDrug(
      @PathVariable("id") String id, @Valid @RequestBody DrugUpsertRequest request) {
    return catalogApi.updateDrug(id, request.toCommand());
  }

  public record ServiceUpsertRequest(
      @NotBlank String serviceCode,
      @NotBlank String serviceName,
      @NotBlank String serviceType,
      boolean isActive) {

    private CatalogServiceUpsertCommand toCommand() {
      return new CatalogServiceUpsertCommand(serviceCode, serviceName, serviceType, isActive);
    }
  }

  public record DrugUpsertRequest(
      @NotBlank String drugCode,
      @NotBlank String drugName,
      @NotBlank String manufacturer,
      LocalDate expiryDate,
      @NotBlank String unit,
      @DecimalMin("0.01") BigDecimal price,
      @Min(0) int stockQuantity,
      boolean isActive) {

    private CatalogDrugUpsertCommand toCommand() {
      return new CatalogDrugUpsertCommand(
          drugCode, drugName, manufacturer, expiryDate, unit, price, stockQuantity, isActive);
    }
  }
}
