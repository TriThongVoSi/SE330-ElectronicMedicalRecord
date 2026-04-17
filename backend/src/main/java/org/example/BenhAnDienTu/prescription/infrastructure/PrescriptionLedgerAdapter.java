package org.example.BenhAnDienTu.prescription.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuanceCommand;
import org.example.BenhAnDienTu.prescription.api.PrescriptionItemCommand;
import org.example.BenhAnDienTu.prescription.api.PrescriptionItemView;
import org.example.BenhAnDienTu.prescription.api.PrescriptionListQuery;
import org.example.BenhAnDienTu.prescription.api.PrescriptionPageView;
import org.example.BenhAnDienTu.prescription.api.PrescriptionView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionLedgerAdapter {

  private static final String PRESCRIPTION_HEADER_SELECT_SQL =
      """
      SELECT pr.prescription_id, pr.prescription_code, pr.patient_id, p.full_name AS patient_name,
             pr.doctor_id, d.full_name AS doctor_name, pr.appointment_id,
             pr.status, pr.diagnosis, pr.advice, pr.created_at
      FROM prescriptions pr
      JOIN patients p ON p.patient_id = pr.patient_id
      JOIN user_accounts d ON d.id = pr.doctor_id
      """;

  private static final String PRESCRIPTION_COUNT_SQL_PREFIX =
      "SELECT COUNT(*) FROM prescriptions pr JOIN patients p ON p.patient_id = pr.patient_id "
          + "JOIN user_accounts d ON d.id = pr.doctor_id ";

  private static final RowMapper<PrescriptionView> HEADER_ROW_MAPPER =
      PrescriptionLedgerAdapter::mapPrescriptionHeader;

  private static final RowMapper<PrescriptionItemView> ITEM_ROW_MAPPER =
      (resultSet, rowNum) ->
          new PrescriptionItemView(
              resultSet.getString("drug_id"),
              resultSet.getString("drug_name"),
              resultSet.getInt("quantity"),
              resultSet.getString("instructions"));

  private final JdbcTemplate jdbcTemplate;

  public PrescriptionLedgerAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PrescriptionPageView list(PrescriptionListQuery query) {
    int page = normalizePage(query.page());
    int size = normalizeSize(query.size());
    int offset = (page - 1) * size;

    SqlFilter sqlFilter = buildListFilter(query);
    List<PrescriptionView> headerRows = fetchPrescriptionHeaders(sqlFilter, size, offset);
    List<PrescriptionView> prescriptions = attachItems(headerRows);

    long totalItems = countPrescriptions(sqlFilter);
    int totalPages = calculateTotalPages(totalItems, size);
    return new PrescriptionPageView(prescriptions, page, size, totalItems, totalPages);
  }

  public Optional<PrescriptionView> findById(String prescriptionId) {
    String sql = PRESCRIPTION_HEADER_SELECT_SQL + " WHERE pr.prescription_id = ? LIMIT 1";
    List<PrescriptionView> rows = jdbcTemplate.query(sql, HEADER_ROW_MAPPER, prescriptionId);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    PrescriptionView header = rows.getFirst();
    return Optional.of(withItems(header, findItems(prescriptionId)));
  }

  public PrescriptionView create(PrescriptionIssuanceCommand command) {
    String prescriptionId = UUID.randomUUID().toString();
    String headerSql =
        """
        INSERT INTO prescriptions (
            prescription_id, prescription_code, patient_id, doctor_id, appointment_id,
            diagnosis, advice, status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    jdbcTemplate.update(
        headerSql,
        prescriptionId,
        command.prescriptionCode(),
        command.patientId(),
        command.doctorId(),
        command.appointmentId(),
        command.diagnosis(),
        command.advice(),
        command.status());

    insertItems(prescriptionId, command.items());
    return findById(prescriptionId).orElseThrow();
  }

  public Optional<PrescriptionView> update(
      String prescriptionId, PrescriptionIssuanceCommand command) {
    String headerSql =
        """
        UPDATE prescriptions
        SET prescription_code = ?,
            patient_id = ?,
            doctor_id = ?,
            appointment_id = ?,
            diagnosis = ?,
            advice = ?,
            status = ?
        WHERE prescription_id = ?
        """;

    int updated =
        jdbcTemplate.update(
            headerSql,
            command.prescriptionCode(),
            command.patientId(),
            command.doctorId(),
            command.appointmentId(),
            command.diagnosis(),
            command.advice(),
            command.status(),
            prescriptionId);

    if (updated == 0) {
      return Optional.empty();
    }

    jdbcTemplate.update("DELETE FROM prescription_items WHERE prescription_id = ?", prescriptionId);
    insertItems(prescriptionId, command.items());
    return findById(prescriptionId);
  }

  private SqlFilter buildListFilter(PrescriptionListQuery query) {
    StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
    List<Object> queryArguments = new ArrayList<>();

    if (hasText(query.search())) {
      String searchPattern = "%" + query.search().trim() + "%";
      whereClause.append(
          " AND (pr.prescription_code LIKE ? OR p.full_name LIKE ? OR d.full_name LIKE ?) ");
      queryArguments.add(searchPattern);
      queryArguments.add(searchPattern);
      queryArguments.add(searchPattern);
    }

    if (hasText(query.status())) {
      whereClause.append(" AND pr.status = ? ");
      queryArguments.add(query.status().trim());
    }

    if (hasText(query.patientId())) {
      whereClause.append(" AND pr.patient_id = ? ");
      queryArguments.add(query.patientId().trim());
    }

    return new SqlFilter(whereClause.toString(), List.copyOf(queryArguments));
  }

  private List<PrescriptionView> fetchPrescriptionHeaders(
      SqlFilter sqlFilter, int pageSize, int offset) {
    String dataSql =
        sqlFilter.decorateSelectSql(PRESCRIPTION_HEADER_SELECT_SQL)
            + " ORDER BY pr.created_at DESC LIMIT ? OFFSET ?";
    List<Object> dataArguments = new ArrayList<>(sqlFilter.queryArguments());
    dataArguments.add(pageSize);
    dataArguments.add(offset);
    return jdbcTemplate.query(dataSql, HEADER_ROW_MAPPER, dataArguments.toArray());
  }

  private List<PrescriptionView> attachItems(List<PrescriptionView> headerRows) {
    // Items are loaded separately to keep list queries fast and avoid row multiplication from JOIN.
    return headerRows.stream().map(header -> withItems(header, findItems(header.id()))).toList();
  }

  private long countPrescriptions(SqlFilter sqlFilter) {
    String countSql = sqlFilter.decorateSelectSql(PRESCRIPTION_COUNT_SQL_PREFIX);
    Long totalItems =
        jdbcTemplate.queryForObject(countSql, Long.class, sqlFilter.queryArguments().toArray());
    return totalItems == null ? 0 : totalItems;
  }

  private void insertItems(String prescriptionId, List<PrescriptionItemCommand> items) {
    String sql =
        """
        INSERT INTO prescription_items (prescription_id, drug_id, quantity, instructions)
        VALUES (?, ?, ?, ?)
        """;
    for (PrescriptionItemCommand prescriptionItem : items) {
      jdbcTemplate.update(
          sql,
          prescriptionId,
          prescriptionItem.drugId(),
          prescriptionItem.quantity(),
          prescriptionItem.instructions());
    }
  }

  private List<PrescriptionItemView> findItems(String prescriptionId) {
    String sql =
        """
        SELECT pi.drug_id, d.drug_name, pi.quantity, pi.instructions
        FROM prescription_items pi
        JOIN drugs d ON d.drug_id = pi.drug_id
        WHERE pi.prescription_id = ?
        ORDER BY d.drug_name ASC
        """;
    return jdbcTemplate.query(sql, ITEM_ROW_MAPPER, prescriptionId);
  }

  private static PrescriptionView mapPrescriptionHeader(ResultSet resultSet, int rowNum)
      throws SQLException {
    return new PrescriptionView(
        resultSet.getString("prescription_id"),
        resultSet.getString("prescription_code"),
        resultSet.getString("patient_id"),
        resultSet.getString("patient_name"),
        resultSet.getString("doctor_id"),
        resultSet.getString("doctor_name"),
        resultSet.getString("appointment_id"),
        resultSet.getString("status"),
        resultSet.getString("diagnosis"),
        resultSet.getString("advice"),
        List.of(),
        resultSet.getTimestamp("created_at").toInstant());
  }

  private static PrescriptionView withItems(
      PrescriptionView prescriptionHeader, List<PrescriptionItemView> prescriptionItems) {
    return new PrescriptionView(
        prescriptionHeader.id(),
        prescriptionHeader.prescriptionCode(),
        prescriptionHeader.patientId(),
        prescriptionHeader.patientName(),
        prescriptionHeader.doctorId(),
        prescriptionHeader.doctorName(),
        prescriptionHeader.appointmentId(),
        prescriptionHeader.status(),
        prescriptionHeader.diagnosis(),
        prescriptionHeader.advice(),
        prescriptionItems,
        prescriptionHeader.createdAt());
  }

  private static boolean hasText(String candidate) {
    return candidate != null && !candidate.isBlank();
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

  private record SqlFilter(String whereClause, List<Object> queryArguments) {
    private String decorateSelectSql(String selectSql) {
      return selectSql + whereClause;
    }
  }
}
