package org.example.BenhAnDienTu.identity.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityTimeConfig {

  @Bean
  Clock systemClock() {
    return Clock.systemUTC();
  }
}
