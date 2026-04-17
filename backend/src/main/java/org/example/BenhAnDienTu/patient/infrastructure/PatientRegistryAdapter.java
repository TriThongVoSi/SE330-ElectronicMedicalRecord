package org.example.BenhAnDienTu.patient.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.BenhAnDienTu.patient.api.PatientListQuery;
import org.example.BenhAnDienTu.patient.api.PatientPageView;
import org.example.BenhAnDienTu.patient.api.PatientSelfProfileUpdateCommand;
import org.example.BenhAnDienTu.patient.api.PatientUpsertCommand;
import org.example.BenhAnDienTu.patient.api.PatientView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class PatientRegistryAdapter {

  private static final RowMapper<PatientView> PATIENT_ROW_MAPPER =
      PatientRegistryAdapter::mapPatient;

  private final JdbcTemplate jdbcTemplate;

  public PatientRegistryAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PatientPageView list(PatientListQuery query) {
    int page = normalizePage(query.page());
    int size = normalizeSize(query.size());
    int offset = (page - 1) * size;

    SqlFilter sqlFilter = buildListFilter(query);
    List<PatientView> items = fetchPatients(sqlFilter, size, offset);
    long totalItems = countPatients(sqlFilter);
    int totalPages = calculateTotalPages(totalItems, size);

    return new PatientPageView(items, page, size, totalItems, totalPages);
  }

  public Optional<PatientView> findById(String patientId) {
    String sql =
        """
        SELECT patient_id, patient_code, full_name, date_of_birth, email, gender, phone, address,
               diagnosis, drug_allergies, height_cm, weight_kg, created_at, updated_at
        FROM patients
        WHERE patient_id = ?
        LIMIT 1
        """;
    List<PatientView> rows = jdbcTemplate.query(sql, PATIENT_ROW_MAPPER, patientId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public Optional<String> findPatientIdByActorId(String actorId) {
    String sql =
        """
        SELECT linked_patient_id
        FROM user_accounts
        WHERE id = ?
        LIMIT 1
        """;
    List<String> rows =
        jdbcTemplate.query(sql, (resultSet, rowNum) -> resultSet.getString("linked_patient_id"), actorId);
    if (rows.isEmpty()) {
      return Optional.empty();
    }

    String linkedPatientId = rows.getFirst();
    if (linkedPatientId == null || linkedPatientId.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(linkedPatientId);
  }

  public Optional<PatientView> findByActorId(String actorId) {
    Optional<String> patientId = findPatientIdByActorId(actorId);
    return patientId.flatMap(this::findById);
  }

  public PatientView create(PatientUpsertCommand command) {
    String patientId = UUID.randomUUID().toString();
    String sql =
        """
        INSERT INTO patients (
            patient_id, patient_code, full_name, date_of_birth, email, gender, phone, address,
            diagnosis, drug_allergies, height_cm, weight_kg
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    jdbcTemplate.update(
        sql,
        patientId,
        command.patientCode(),
        command.fullName(),
        command.dateOfBirth(),
        command.email(),
        command.gender(),
        command.phone(),
        command.address(),
        command.diagnosis(),
        command.drugAllergies(),
        command.heightCm(),
        command.weightKg());

    return findById(patientId).orElseThrow();
  }

  public Optional<PatientView> update(String patientId, PatientUpsertCommand command) {
    String sql =
        """
        UPDATE patients
        SET patient_code = ?,
            full_name = ?,
            date_of_birth = ?,
            email = ?,
            gender = ?,
            phone = ?,
            address = ?,
            diagnosis = ?,
            drug_allergies = ?,
            height_cm = ?,
            weight_kg = ?
        WHERE patient_id = ?
        """;

    int updated =
        jdbcTemplate.update(
            sql,
            command.patientCode(),
            command.fullName(),
            command.dateOfBirth(),
            command.email(),
            command.gender(),
            command.phone(),
            command.address(),
            command.diagnosis(),
            command.drugAllergies(),
            command.heightCm(),
            command.weightKg(),
            patientId);

    if (updated == 0) {
      return Optional.empty();
    }
    return findById(patientId);
  }

  public Optional<PatientView> updateCurrentPatientProfile(
      String actorId, PatientSelfProfileUpdateCommand command) {
    Optional<String> patientId = findPatientIdByActorId(actorId);
    if (patientId.isEmpty()) {
      return Optional.empty();
    }

    int updated =
        jdbcTemplate.update(
            """
            UPDATE patients
            SET phone = ?,
                address = ?,
                height_cm = ?,
                weight_kg = ?,
                drug_allergies = ?
            WHERE patient_id = ?
            """,
            command.phone(),
            normalizeNullable(command.address()),
            command.heightCm(),
            command.weightKg(),
            normalizeNullable(command.drugAllergies()),
            patientId.get());

    if (updated == 0) {
      return Optional.empty();
    }
    return findById(patientId.get());
  }

  private static PatientView mapPatient(ResultSet resultSet, int rowNum) throws SQLException {
    return new PatientView(
        resultSet.getString("patient_id"),
        resultSet.getString("patient_code"),
        resultSet.getString("full_name"),
        resultSet.getDate("date_of_birth") == null
            ? null
            : resultSet.getDate("date_of_birth").toLocalDate(),
        resultSet.getString("email"),
        resultSet.getString("gender"),
        resultSet.getString("phone"),
        resultSet.getString("address"),
        resultSet.getString("diagnosis"),
        resultSet.getString("drug_allergies"),
        resultSet.getBigDecimal("height_cm"),
        resultSet.getBigDecimal("weight_kg"),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant());
  }

  private SqlFilter buildListFilter(PatientListQuery query) {
    StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
    List<Object> queryArguments = new ArrayList<>();

    if (hasText(query.search())) {
      whereClause.append(" AND (patient_code LIKE ? OR full_name LIKE ? OR phone LIKE ?) ");
      String searchPattern = "%" + query.search().trim() + "%";
      queryArguments.add(searchPattern);
      queryArguments.add(searchPattern);
      queryArguments.add(searchPattern);
    }

    if (hasText(query.phone())) {
      whereClause.append(" AND phone LIKE ? ");
      queryArguments.add("%" + query.phone().trim() + "%");
    }

    if (hasText(query.code())) {
      whereClause.append(" AND patient_code LIKE ? ");
      queryArguments.add("%" + query.code().trim() + "%");
    }

    return new SqlFilter(whereClause.toString(), List.copyOf(queryArguments));
  }

  private List<PatientView> fetchPatients(SqlFilter sqlFilter, int size, int offset) {
    String dataSql =
        sqlFilter.decorateSelectSql(
                """
                SELECT patient_id, patient_code, full_name, date_of_birth, email, gender, phone, address,
                       diagnosis, drug_allergies, height_cm, weight_kg, created_at, updated_at
                FROM patients
                """)
            + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
    List<Object> dataArguments = new ArrayList<>(sqlFilter.queryArguments());
    dataArguments.add(size);
    dataArguments.add(offset);

    return jdbcTemplate.query(dataSql, PATIENT_ROW_MAPPER, dataArguments.toArray());
  }

  private long countPatients(SqlFilter sqlFilter) {
    String countSql = sqlFilter.decorateSelectSql("SELECT COUNT(*) FROM patients ");
    Long totalItems =
        jdbcTemplate.queryForObject(countSql, Long.class, sqlFilter.queryArguments().toArray());
    return totalItems == null ? 0 : totalItems;
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

  private static boolean hasText(String candidate) {
    return candidate != null && !candidate.isBlank();
  }

  private static String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private record SqlFilter(String whereClause, List<Object> queryArguments) {
    private String decorateSelectSql(String selectSql) {
      return selectSql + whereClause;
    }
  }
}
