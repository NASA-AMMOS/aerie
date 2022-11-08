package gov.nasa.jpl.aerie.scheduler.server.services;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public final class GenerateSchedulingLibActionTest {

  @Test
  void testTypescriptResourcesLoaded() {
    assertTypescriptResourceLoaded("scheduler-edsl-fluent-api.ts");
    assertTypescriptResourceLoaded("scheduler-ast.ts");
    assertTypescriptResourceLoaded("constraints-edsl-fluent-api.ts");
    assertTypescriptResourceLoaded("constraints-ast.ts");
    assertTypescriptResourceLoaded("TemporalPolyfillTypes.ts");
  }

  private static void assertTypescriptResourceLoaded(final String basename) {
    final var res = GenerateSchedulingLibAction.getTypescriptResource(basename);
    assertThat(res.basename()).isEqualTo(basename);
    assertThat(res.source()).isNotEmpty();
  }
}
