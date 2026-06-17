package org.example.BenhAnDienTu;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Explicit ArchUnit architecture tests for the EMR application.
 *
 * <p>These tests <b>directly</b> use the ArchUnit API ({@code com.tngtech.archunit}) to enforce
 * coding conventions and layered-architecture rules across every module. They complement the
 * Spring-Modulith-based {@link ModularityTests} which validate inter-module boundaries.
 *
 * <h3>Test categories</h3>
 *
 * <ul>
 *   <li><b>Layered Architecture</b> — verifies that {@code domain} never depends on {@code
 *       infrastructure}, and {@code api} never depends on {@code infrastructure}.
 *   <li><b>Naming Conventions</b> — {@code @RestController} classes must end with "Controller",
 *       {@code @Service} classes must live in {@code application} packages.
 *   <li><b>Dependency Constraints</b> — {@code domain} layer must not import Spring Framework or
 *       JPA/Hibernate classes, keeping it framework-agnostic.
 * </ul>
 */
@DisplayName("ArchUnit — Architecture Rules")
class ArchitectureTests {

  private static JavaClasses appClasses;

  @BeforeAll
  static void importClasses() {
    appClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("org.example.BenhAnDienTu");
  }

  // ─────────────────────────────────────────────────────────────────────
  // 1. Layered Architecture
  // ─────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("1 · Layered Architecture")
  class LayeredArchitectureTests {

    @Test
    @DisplayName("Domain layer must not depend on Infrastructure layer")
    void domainShouldNotDependOnInfrastructure() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..domain..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..infrastructure..")
              .because(
                  "Domain layer chứa business logic thuần túy, "
                      + "không được phụ thuộc vào chi tiết triển khai trong Infrastructure");

      rule.check(appClasses);
    }

    @Test
    @DisplayName("Domain layer must not depend on Application layer")
    void domainShouldNotDependOnApplication() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..domain..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..application..")
              .because(
                  "Domain layer là lõi nghiệp vụ, "
                      + "không được phụ thuộc ngược lên Application layer");

      rule.check(appClasses);
    }

    @Test
    @DisplayName("API layer must not depend on Infrastructure layer")
    void apiShouldNotDependOnInfrastructure() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..api..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("..infrastructure..")
              .because(
                  "API layer chứa contracts công khai (interface, DTO, event), "
                      + "không được phụ thuộc vào Infrastructure");

      rule.check(appClasses);
    }

    @Test
    @DisplayName("Layered architecture is respected across all modules")
    void layeredArchitectureIsRespected() {
      layeredArchitecture()
          .consideringAllDependencies()
          .layer("API")
          .definedBy("..api..")
          .layer("Application")
          .definedBy("..application..")
          .layer("Domain")
          .definedBy("..domain..")
          .layer("Infrastructure")
          .definedBy("..infrastructure..")
          .whereLayer("Infrastructure")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Application")
          .mayOnlyBeAccessedByLayers("Infrastructure")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers("Application", "Infrastructure")
          .whereLayer("API")
          .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Domain")
          .because(
              "Kiến trúc phân tầng: Infrastructure → Application → Domain ← API, "
                  + "không cho phép phụ thuộc ngược chiều")
          .check(appClasses);
    }
  }

  // ─────────────────────────────────────────────────────────────────────
  // 2. Naming Conventions
  // ─────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("2 · Naming Conventions")
  class NamingConventionTests {

    @Test
    @DisplayName("Classes annotated with @RestController should have name ending with 'Controller'")
    void controllerNamingConvention() {
      ArchRule rule =
          classes()
              .that()
              .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
              .should()
              .haveSimpleNameEndingWith("Controller")
              .because("Convention: mọi REST controller phải có tên kết thúc bằng 'Controller'");

      rule.check(appClasses);
    }

    @Test
    @DisplayName("@RestController classes should reside in 'infrastructure' packages")
    void controllersShouldResideInInfrastructure() {
      ArchRule rule =
          classes()
              .that()
              .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
              .should()
              .resideInAPackage("..infrastructure..")
              .orShould()
              .resideInAPackage("org.example.BenhAnDienTu")
              .because(
                  "Controllers là chi tiết triển khai HTTP, "
                      + "thuộc tầng Infrastructure theo Hexagonal Architecture");

      rule.check(appClasses);
    }

    @Test
    @DisplayName("@Service classes should reside in 'application' packages")
    void servicesShouldResideInApplicationLayer() {
      ArchRule rule =
          classes()
              .that()
              .areAnnotatedWith("org.springframework.stereotype.Service")
              .should()
              .resideInAPackage("..application..")
              .because(
                  "Service classes chứa use-case logic, "
                      + "phải nằm trong tầng Application theo kiến trúc Hexagonal");

      rule.check(appClasses);
    }
  }

  // ─────────────────────────────────────────────────────────────────────
  // 3. Domain Purity
  // ─────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("3 · Domain Layer Purity")
  class DomainPurityTests {

    @Test
    @DisplayName("Domain classes must not import Spring Framework")
    void domainShouldNotUseSpringFramework() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..domain..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("org.springframework..")
              .because(
                  "Domain layer phải framework-agnostic, "
                      + "không được import bất kỳ class nào từ Spring Framework");

      rule.check(appClasses);
    }

    @Test
    @DisplayName("Domain classes must not import JPA/Hibernate")
    void domainShouldNotUseJpaOrHibernate() {
      ArchRule rule =
          noClasses()
              .that()
              .resideInAPackage("..domain..")
              .should()
              .dependOnClassesThat()
              .resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
              .because(
                  "Domain layer không được phụ thuộc vào JPA/Hibernate, "
                      + "đây là chi tiết persistence thuộc Infrastructure layer");

      rule.check(appClasses);
    }
  }
}
