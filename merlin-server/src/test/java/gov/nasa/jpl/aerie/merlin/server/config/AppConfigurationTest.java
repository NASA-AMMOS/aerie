package gov.nasa.jpl.aerie.merlin.server.config;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppConfigurationTest {
  @Test
  public void testParseProperties() {
    final var expected = new AppConfiguration(
        7654,
        JavalinLoggingState.Disabled,
        Path.of("merlin_file_store"),
        new MongoStore(
            URI.create("http://localhost.mongo.test"),
            "mongo_database_test",
            "mongo_plan_collection_test",
            "mongo_activity_collection_test",
            "mongo_adaptation_collection_test",
            "mongo_simulation_results_collection_test"));

    final var observed = AppConfigurationJsonMapper.fromJson(AppConfigurationJsonMapper.toJson(expected));

    assertEquals(expected, observed);
  }
}
