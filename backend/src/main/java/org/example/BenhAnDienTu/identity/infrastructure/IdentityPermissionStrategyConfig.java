package org.example.BenhAnDienTu.identity.infrastructure;

import org.example.BenhAnDienTu.identity.domain.AdminPermissionStrategy;
import org.example.BenhAnDienTu.identity.domain.DefaultPermissionStrategy;
import org.example.BenhAnDienTu.identity.domain.DoctorPermissionStrategy;
import org.example.BenhAnDienTu.identity.domain.PatientPermissionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IdentityPermissionStrategyConfig {

  @Bean
  AdminPermissionStrategy adminPermissionStrategy() {
    return new AdminPermissionStrategy();
  }

  @Bean
  DefaultPermissionStrategy defaultPermissionStrategy() {
    return new DefaultPermissionStrategy();
  }

  @Bean
  DoctorPermissionStrategy doctorPermissionStrategy() {
    return new DoctorPermissionStrategy();
  }

  @Bean
  PatientPermissionStrategy patientPermissionStrategy() {
    return new PatientPermissionStrategy();
  }
}
