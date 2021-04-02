package gov.nasa.jpl.aerie.fooadaptation;

import static org.junit.Assert.assertEquals;

import javax.json.Json;

import gov.nasa.jpl.aerie.fooadaptation.mappers.FooValueMappers;
import gov.nasa.jpl.aerie.merlin.driver.json.JsonEncoding;
import org.junit.Test;

public class FooValueMappersTest {

  @Test
  public void testConfigurationMapper() {
    final var stream = FooValueMappersTest.class.getResourceAsStream("mission_config.json");
    final var serializedConfig = JsonEncoding.decode(Json.createReader(stream).read());
    final var config = FooValueMappers.configuration().deserializeValue(serializedConfig).getSuccessOrThrow();

    assertEquals(42.0, config.sinkRate, 1e-9);
  }

}
