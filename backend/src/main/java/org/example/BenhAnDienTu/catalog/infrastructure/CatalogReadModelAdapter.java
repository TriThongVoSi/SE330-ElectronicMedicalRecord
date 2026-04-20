package org.example.BenhAnDienTu.catalog.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugListQuery;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugPageView;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugUpsertCommand;
import org.example.BenhAnDienTu.catalog.api.CatalogDrugView;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceListQuery;
import org.example.BenhAnDienTu.catalog.api.CatalogServicePageView;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceUpsertCommand;
import org.example.BenhAnDienTu.catalog.api.CatalogServiceView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class CatalogReadModelAdapter {

  private static final RowMapper<CatalogServiceView> SERVICE_ROW_MAPPER =
      CatalogReadModelAdapter::mapService;
  private static final RowMapper<CatalogDrugView> DRUG_ROW_MAPPER =
      CatalogReadModelAdapter::mapDrug;

  private final JdbcTemplate jdbcTemplate;

  public CatalogReadModelAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public CatalogServicePageView listServices(CatalogServiceListQuery query) {
    int page = normalizePage(query.page());
    int size = normalizeSize(query.size());
    int offset = (page - 1) * size;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (query.search() != null && !query.search().isBlank()) {
      String pattern = "%" + query.search().trim() + "%";
      where.append(" AND (service_code LIKE ? OR service_name LIKE ? OR service_type LIKE ?) ");
      params.add(pattern);
      params.add(pattern);
      params.add(pattern);
    }

    if (query.isActive() != null) {
      where.append(" AND is_active = ? ");
      params.add(query.isActive());
    }

    String dataSql =
        """
        SELECT id, service_code, service_name, service_type, is_active
        FROM services
        """
            + where
            + " ORDER BY service_name ASC LIMIT ? OFFSET ?";
    List<Object> dataParams = new ArrayList<>(params);
    dataParams.add(size);
    dataParams.add(offset);
    List<CatalogServiceView> items =
        jdbcTemplate.query(dataSql, SERVICE_ROW_MAPPER, dataParams.toArray());

    String countSql = "SELECT COUNT(*) FROM services " + where;
    long totalItems = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    int totalPages = calculateTotalPages(totalItems, size);

    return new CatalogServicePageView(items, page, size, totalItems, totalPages);
  }

  public Optional<CatalogServiceView> findByCode(String serviceCode) {
    String sql =
        """
        SELECT id, service_code, service_name, service_type, is_active
        FROM services
        WHERE service_code = ?
        LIMIT 1
        """;
    List<CatalogServiceView> rows = jdbcTemplate.query(sql, SERVICE_ROW_MAPPER, serviceCode);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public Optional<CatalogServiceView> findServiceById(String serviceId) {
    String sql =
        """
        SELECT id, service_code, service_name, service_type, is_active
        FROM services
        WHERE id = ?
        LIMIT 1
        """;
    List<CatalogServiceView> rows = jdbcTemplate.query(sql, SERVICE_ROW_MAPPER, serviceId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public CatalogServiceView createService(CatalogServiceUpsertCommand command) {
    String serviceId = UUID.randomUUID().toString();
    String sql =
        """
        INSERT INTO services (id, service_code, service_name, service_type, is_active)
        VALUES (?, ?, ?, ?, ?)
        """;
    jdbcTemplate.update(
        sql,
        serviceId,
        command.serviceCode(),
        command.serviceName(),
        command.serviceType(),
        command.isActive());

    return findServiceById(serviceId).orElseThrow();
  }

  public Optional<CatalogServiceView> updateService(
      String serviceId, CatalogServiceUpsertCommand command) {
    String sql =
        """
        UPDATE services
        SET service_code = ?, service_name = ?, service_type = ?, is_active = ?
        WHERE id = ?
        """;
    int updated =
        jdbcTemplate.update(
            sql,
            command.serviceCode(),
            command.serviceName(),
            command.serviceType(),
            command.isActive(),
            serviceId);
    if (updated == 0) {
      return Optional.empty();
    }
    return findServiceById(serviceId);
  }

  public CatalogDrugPageView listDrugs(CatalogDrugListQuery query) {
    int page = normalizePage(query.page());
    int size = normalizeSize(query.size());
    int offset = (page - 1) * size;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (query.search() != null && !query.search().isBlank()) {
      String pattern = "%" + query.search().trim() + "%";
      where.append(" AND (drug_code LIKE ? OR drug_name LIKE ? OR manufacturer LIKE ?) ");
      params.add(pattern);
      params.add(pattern);
      params.add(pattern);
    }

    if (query.isActive() != null) {
      where.append(" AND is_active = ? ");
      params.add(query.isActive());
    }

    String dataSql =
        """
        SELECT drug_id, drug_code, drug_name, manufacturer, expiry_date, unit, price, stock_quantity, is_active
        FROM drugs
        """
            + where
            + " ORDER BY drug_name ASC LIMIT ? OFFSET ?";
    List<Object> dataParams = new ArrayList<>(params);
    dataParams.add(size);
    dataParams.add(offset);
    List<CatalogDrugView> items =
        jdbcTemplate.query(dataSql, DRUG_ROW_MAPPER, dataParams.toArray());

    String countSql = "SELECT COUNT(*) FROM drugs " + where;
    long totalItems = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    int totalPages = calculateTotalPages(totalItems, size);

    return new CatalogDrugPageView(items, page, size, totalItems, totalPages);
  }

  public Optional<CatalogDrugView> findDrugById(String drugId) {
    String sql =
        """
        SELECT drug_id, drug_code, drug_name, manufacturer, expiry_date, unit, price, stock_quantity, is_active
        FROM drugs
        WHERE drug_id = ?
        LIMIT 1
        """;
    List<CatalogDrugView> rows = jdbcTemplate.query(sql, DRUG_ROW_MAPPER, drugId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public CatalogDrugView createDrug(CatalogDrugUpsertCommand command) {
    String drugId = UUID.randomUUID().toString();
    String sql =
        """
        INSERT INTO drugs (
            drug_id, drug_code, drug_name, manufacturer, expiry_date, unit, price, stock_quantity, is_active
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    jdbcTemplate.update(
        sql,
        drugId,
        command.drugCode(),
        command.drugName(),
        command.manufacturer(),
        command.expiryDate(),
        command.unit(),
        command.price(),
        command.stockQuantity(),
        command.isActive());
    return findDrugById(drugId).orElseThrow();
  }

  public Optional<CatalogDrugView> updateDrug(String drugId, CatalogDrugUpsertCommand command) {
    String sql =
        """
        UPDATE drugs
        SET drug_code = ?, drug_name = ?, manufacturer = ?, expiry_date = ?, unit = ?,
            price = ?, stock_quantity = ?, is_active = ?
        WHERE drug_id = ?
        """;
    int updated =
        jdbcTemplate.update(
            sql,
            command.drugCode(),
            command.drugName(),
            command.manufacturer(),
            command.expiryDate(),
            command.unit(),
            command.price(),
            command.stockQuantity(),
            command.isActive(),
            drugId);
    if (updated == 0) {
      return Optional.empty();
    }
    return findDrugById(drugId);
  }

  private static CatalogServiceView mapService(ResultSet resultSet, int rowNum)
      throws SQLException {
    return new CatalogServiceView(
        resultSet.getString("id"),
        resultSet.getString("service_code"),
        resultSet.getString("service_name"),
        resultSet.getString("service_type"),
        resultSet.getBoolean("is_active"));
  }

  private static CatalogDrugView mapDrug(ResultSet resultSet, int rowNum) throws SQLException {
    return new CatalogDrugView(
        resultSet.getString("drug_id"),
        resultSet.getString("drug_code"),
        resultSet.getString("drug_name"),
        resultSet.getString("manufacturer"),
        resultSet.getDate("expiry_date").toLocalDate(),
        resultSet.getString("unit"),
        resultSet.getBigDecimal("price"),
        resultSet.getInt("stock_quantity"),
        resultSet.getBoolean("is_active"));
  }

  private static int normalizePage(int page) {
    return page <= 0 ? 1 : page;
  }

  private static int normalizeSize(int size) {
    if (size <= 0) {
      return 10;
    }
    return Math.min(size, 200);
  }

  private static int calculateTotalPages(long totalItems, int size) {
    return (int) Math.max(1, Math.ceil((double) totalItems / size));
  }
}
