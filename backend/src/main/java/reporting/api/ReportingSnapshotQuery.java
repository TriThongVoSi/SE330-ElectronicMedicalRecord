package org.example.BenhAnDienTu.reporting.api;

import java.time.Instant;

/** Boundary query for generating a reporting snapshot window. */
public record ReportingSnapshotQuery(Instant fromInclusive, Instant toExclusive) {}
