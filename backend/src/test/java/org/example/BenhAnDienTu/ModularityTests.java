package org.example.BenhAnDienTu;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Architecture verification tests that enforce module independence at build time.
 *
 * <p>These tests leverage Spring Modulith (backed by ArchUnit) to scan every class in the
 * application and validate that cross-module imports only traverse the public {@code api}
 * sub-packages marked with {@link org.springframework.modulith.NamedInterface @NamedInterface}.
 *
 * <p>If any module imports a class from the {@code application}, {@code domain}, or {@code
 * infrastructure} layer of another module, the test will <b>fail</b> and the build will be
 * <b>rejected</b>.
 *
 * <h3>What {@code verify()} checks:</h3>
 *
 * <ul>
 *   <li>Each module only depends on modules listed in its {@code allowedDependencies}.
 *   <li>Cross-module access is restricted to {@code NamedInterface("api")} packages.
 *   <li>No cyclic dependencies exist between modules.
 *   <li>Every {@code @ApplicationModule}'s internal packages ({@code application}, {@code domain},
 *       {@code infrastructure}) are encapsulated.
 * </ul>
 *
 * @see org.springframework.modulith.ApplicationModule
 * @see org.springframework.modulith.NamedInterface
 */
class ModularityTests {

  private final ApplicationModules modules = ApplicationModules.of(BenhAnDienTuApplication.class);

  /**
   * Core verification — fails the build when module boundaries are violated.
   *
   * <p>This single method enforces ALL the following rules simultaneously:
   *
   * <ol>
   *   <li><b>Dependency whitelist</b>: e.g. {@code patient} may only depend on {@code
   *       identity::api}. If a class in {@code patient.infrastructure} imports anything from {@code
   *       staff.application}, this test fails.
   *   <li><b>Named-interface encapsulation</b>: only classes inside the {@code api} sub-package of
   *       each module are reachable from the outside. Internal layers ({@code application}, {@code
   *       domain}, {@code infrastructure}) are treated as module-private.
   *   <li><b>Cycle detection</b>: circular dependencies (e.g. A → B → C → A) are rejected.
   * </ol>
   */
  @Test
  void shouldVerifyModuleBoundariesAreRespected() {
    modules.verify();
  }

  /**
   * Prints the detected module arrangement to stdout for debugging and documentation.
   *
   * <p>Useful during development to quickly see which modules Spring Modulith has discovered and
   * which named interfaces each module exposes.
   */
  @Test
  void shouldPrintModuleArrangement() {
    modules.forEach(System.out::println);
  }

  /**
   * Generates module documentation as PlantUML diagrams and an Asciidoc «canvases» document.
   *
   * <p>Output is written to {@code target/spring-modulith-docs}. The generated artifacts include:
   *
   * <ul>
   *   <li>A C4-component-level diagram showing all modules and their dependencies.
   *   <li>Individual module «canvas» pages listing exposed interfaces, published events, and
   *       consumed events.
   * </ul>
   *
   * <p>This test never fails on its own; it only produces documentation files.
   */
  @Test
  void shouldGenerateModuleDocumentation() {
    new Documenter(modules)
        .writeModulesAsPlantUml()
        .writeIndividualModulesAsPlantUml()
        .writeModuleCanvases();
  }
}
