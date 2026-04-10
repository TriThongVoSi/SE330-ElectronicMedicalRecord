package org.example.BenhAnDienTu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "spring.main.lazy-initialization=true",
      "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
      "jwt.signer-key=test-jwt-signing-key-change-in-prod-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
      "otp.hash-secret=test-otp-hash-secret-change-in-prod-0123456789"
    })
class BenhAnDienTuApplicationTests {

  @Test
  void contextLoads() {}
}
