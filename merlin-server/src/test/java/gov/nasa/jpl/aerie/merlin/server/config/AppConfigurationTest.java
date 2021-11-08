package gov.nasa.jpl.aerie.merlin.server.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppConfigurationTest {
  @Test
  public void testParseProperties() {
    final var expected = new AppConfiguration(
        7654,
        JavalinLoggingState.Disabled,
        Path.of("merlin_file_store"),
        new PostgresStore("postgres",
                          "aerie",
                          5432,
                          "aerie",
                          "aerie" )
    );

    final var observed = AppConfigurationJsonMapper.fromJson(AppConfigurationJsonMapper.toJson(expected));

    assertEquals(expected, observed);
  }
}
