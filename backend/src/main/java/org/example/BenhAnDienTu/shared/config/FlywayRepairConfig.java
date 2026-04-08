package org.example.BenhAnDienTu.shared.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Automatically repairs Flyway schema history before running migrations. This fixes checksum
 * mismatches from previously failed or edited migrations.
 *
 * <p>Safe to keep in dev; consider removing or gating behind a profile for production.
 */
@Configuration
public class FlywayRepairConfig {

  @Bean
  public FlywayMigrationStrategy repairThenMigrate() {
    return flyway -> {
      flyway.repair();
      flyway.migrate();
    };
  }
}
