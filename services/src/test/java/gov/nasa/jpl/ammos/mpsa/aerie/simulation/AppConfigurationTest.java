package gov.nasa.jpl.ammos.mpsa.aerie.simulation;

import org.junit.jupiter.api.Test;

import javax.json.Json;

import static org.assertj.core.api.Assertions.assertThat;

public class AppConfigurationTest {
  @Test
  public void configurationDeserializationShouldSucceed() {
    // GIVEN a JSON object with configuration properties
    final var expected = new AppConfiguration(1234);
    final var json = Json.createObjectBuilder()
        .add("http_port", expected.http_port)
        .build();

    // WHEN the properties are loaded into an AppConfiguration
    final var observed = AppConfiguration.parseProperties(json);

    // THEN the deserialized configuration properties should be as expected
    assertThat(observed).isEqualTo(expected);
  }
}
