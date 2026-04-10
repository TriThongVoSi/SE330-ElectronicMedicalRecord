package org.example.BenhAnDienTu.identity.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdentityTokenStore {

  private final JdbcTemplate jdbcTemplate;

  public IdentityTokenStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean isInvalidated(String tokenId) {
    if (tokenId == null || tokenId.isBlank()) {
      return false;
    }
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM invalidated_tokens WHERE token_id = ?", Integer.class, tokenId);
    return count != null && count > 0;
  }

  public void invalidate(String tokenId, Instant expiryTime) {
    if (tokenId == null || tokenId.isBlank() || expiryTime == null) {
      return;
    }
    jdbcTemplate.update(
        """
            INSERT INTO invalidated_tokens (token_id, expiry_time)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE expiry_time = VALUES(expiry_time)
            """,
        tokenId,
        Timestamp.from(expiryTime));
  }

  public int purgeExpired(Instant now) {
    return jdbcTemplate.update(
        "DELETE FROM invalidated_tokens WHERE expiry_time < ?", Timestamp.from(now));
  }
}
