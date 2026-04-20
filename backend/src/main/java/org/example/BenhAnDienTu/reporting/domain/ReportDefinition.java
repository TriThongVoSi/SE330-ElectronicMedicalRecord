package org.example.BenhAnDienTu.reporting.domain;

import java.time.Instant;
import java.util.List;

/** Domain root placeholder for reporting artifacts. */
public record ReportDefinition(String reportName, Instant generatedAt, List<String> highlights) {}
