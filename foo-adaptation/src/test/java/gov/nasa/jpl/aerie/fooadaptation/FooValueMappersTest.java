package gov.nasa.jpl.aerie.fooadaptation;

import javax.json.Json;

import gov.nasa.jpl.aerie.fooadaptation.mappers.FooValueMappers;
import gov.nasa.jpl.aerie.merlin.driver.json.JsonEncoding;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public final class FooValueMappersTest {
  @Test
  public void testConfigurationMapper() {
    final var stream = FooValueMappersTest.class.getResourceAsStream("mission_config.json");
    final var serializedConfig = JsonEncoding.decode(Json.createReader(stream).read());
    final var config = FooValueMappers.configuration().deserializeValue(serializedConfig).getSuccessOrThrow();

    assertThat(config.sinkRate).isCloseTo(42.0, within(1e-9));
  }
}
