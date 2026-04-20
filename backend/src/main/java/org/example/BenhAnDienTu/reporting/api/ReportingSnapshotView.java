package org.example.BenhAnDienTu.reporting.api;

import java.util.List;

/** Read-only reporting output shared to external modules. */
public record ReportingSnapshotView(String reportName, List<String> highlights) {}
