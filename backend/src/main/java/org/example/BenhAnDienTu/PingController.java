package org.example.BenhAnDienTu;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

  @GetMapping("/ping")
  public Map<String, Object> ping() {
    return Map.of(
        "status", "ok",
        "service", "EMR-backend",
        "timestamp", Instant.now().toString());
  }
}
