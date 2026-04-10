package org.example.BenhAnDienTu.identity.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.identity.domain.OtpPurpose;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class IdentityOtpStore {

  private static final RowMapper<OtpVerificationRow> OTP_ROW_MAPPER =
      (resultSet, rowNum) ->
          new OtpVerificationRow(
              resultSet.getString("otp_id"),
              resultSet.getString("user_id"),
              resultSet.getString("email"),
              OtpPurpose.valueOf(resultSet.getString("purpose")),
              resultSet.getString("otp_hash"),
              resultSet.getTimestamp("created_at").toLocalDateTime(),
              resultSet.getTimestamp("expires_at").toLocalDateTime(),
              resultSet.getInt("attempts"),
              resultSet.getInt("max_attempts"),
              nullableDateTime(resultSet, "consumed_at"),
              nullableDateTime(resultSet, "last_sent_at"),
              resultSet.getInt("resend_count"));

  private final JdbcTemplate jdbcTemplate;

  public IdentityOtpStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<OtpVerificationRow> findLatestActive(String email, OtpPurpose purpose) {
    List<OtpVerificationRow> rows =
        jdbcTemplate.query(
            """
            SELECT otp_id, user_id, email, purpose, otp_hash, created_at, expires_at, attempts,
                   max_attempts, consumed_at, last_sent_at, resend_count
            FROM otp_verifications
            WHERE email = ? AND purpose = ? AND consumed_at IS NULL
            ORDER BY created_at DESC
            LIMIT 1
            """,
            OTP_ROW_MAPPER,
            email,
            purpose.name());
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  public void saveNewChallenge(NewOtpChallenge challenge) {
    jdbcTemplate.update(
        """
            INSERT INTO otp_verifications (
                otp_id, user_id, email, purpose, otp_hash, created_at, expires_at, attempts,
                max_attempts, consumed_at, last_sent_at, resend_count, metadata
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)
            """,
        challenge.otpId(),
        challenge.userId(),
        challenge.email(),
        challenge.purpose().name(),
        challenge.otpHash(),
        Timestamp.valueOf(challenge.createdAt()),
        Timestamp.valueOf(challenge.expiresAt()),
        challenge.attempts(),
        challenge.maxAttempts(),
        Timestamp.valueOf(challenge.lastSentAt()),
        challenge.resendCount(),
        challenge.metadata());
  }

  public void markConsumed(String otpId, LocalDateTime consumedAt) {
    jdbcTemplate.update(
        """
            UPDATE otp_verifications
            SET consumed_at = ?
            WHERE otp_id = ?
            """,
        Timestamp.valueOf(consumedAt),
        otpId);
  }

  public void updateAttempts(String otpId, int attempts) {
    jdbcTemplate.update(
        """
            UPDATE otp_verifications
            SET attempts = ?
            WHERE otp_id = ?
            """,
        attempts,
        otpId);
  }

  private static LocalDateTime nullableDateTime(ResultSet resultSet, String column)
      throws SQLException {
    Timestamp timestamp = resultSet.getTimestamp(column);
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  public record OtpVerificationRow(
      String otpId,
      String userId,
      String email,
      OtpPurpose purpose,
      String otpHash,
      LocalDateTime createdAt,
      LocalDateTime expiresAt,
      int attempts,
      int maxAttempts,
      LocalDateTime consumedAt,
      LocalDateTime lastSentAt,
      int resendCount) {}

  public record NewOtpChallenge(
      String otpId,
      String userId,
      String email,
      OtpPurpose purpose,
      String otpHash,
      LocalDateTime createdAt,
      LocalDateTime expiresAt,
      int attempts,
      int maxAttempts,
      LocalDateTime lastSentAt,
      int resendCount,
      String metadata) {}
}
