/** Reporting module - analytical reporting boundary. */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
      "appointment::api",
      "patient::api",
      "prescription::api",
      "catalog::api",
      "staff::api"
    })
package org.example.BenhAnDienTu.reporting;
