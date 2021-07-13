package gov.nasa.jpl.aerie.merlin.server.config;

import org.junit.Test;

import java.net.URI;
import java.util.Optional;

public class AppConfigurationTest {
  @Test
  public void testParseProperties() {
    final var expected = new AppConfiguration(
        7654,
        JavalinLoggingState.Disabled,
        Optional.empty(),
        new MongoStore(
            URI.create("http://localhost.mongo.test"),
            "mongo_database_test",
            "mongo_plan_collection_test",
            "mongo_activity_collection_test",
            "mongo_adaptation_collection_test",
            "mongo_simulation_results_collection_test"));

    final var observed = AppConfigurationJsonMapper.fromJson(AppConfigurationJsonMapper.toJson(expected));

    assertTypedEquals(Optional.of(expected), observed);
  }

  /**
   * A wrapper around {@link org.junit.jupiter.api.Assertions#assertEquals}
   * that ensures that the arguments given are of the same type.
   */
  private static <T> void assertTypedEquals(final T expected, final T observed) {
    // Don't use an import; it makes it too easy for test code to use `assertEquals` directly.
    org.junit.jupiter.api.Assertions.assertEquals(expected, observed);
  }
}
