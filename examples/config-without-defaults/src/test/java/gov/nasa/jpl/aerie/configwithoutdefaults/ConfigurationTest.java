package gov.nasa.jpl.aerie.configwithoutdefaults;

import gov.nasa.jpl.aerie.configwithoutdefaults.generated.ActivityTypes;
import gov.nasa.jpl.aerie.configwithoutdefaults.generated.ConfigurationMapper;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinTestContext;
import gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.UnconstructableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(Lifecycle.PER_CLASS)
public final class ConfigurationTest {

  @RegisterExtension
  public static final MerlinExtension<ActivityTypes, Mission> ext = new MerlinExtension<>();

  private static final Integer a = 42;
  private static final Double b = 3.14;
  private static final String c = "JPL";

  private final Mission model;

  public ConfigurationTest(final MerlinTestContext<ActivityTypes, Mission> ctx)
  throws UnconstructableException
  {
    // Rely on config. defaults by instantiating config. with empty argument map
    final var config = new ConfigurationMapper().instantiate(Map.of(
        "a", SerializedValue.of(a),
        "b", SerializedValue.of(b),
        "c", SerializedValue.of(c)
    ));

    this.model = new Mission(ctx.registrar(), config);
    ctx.use(model, ActivityTypes::register);
  }

  @Test
  public void testDefaults() {
    assertThat(model.configuration.a()).isEqualTo(a);
    assertThat(model.configuration.b()).isEqualTo(b);
    assertThat(model.configuration.c()).isEqualTo(c);
  }
}
