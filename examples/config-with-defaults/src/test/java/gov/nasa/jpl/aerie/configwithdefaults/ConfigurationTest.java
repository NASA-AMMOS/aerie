package gov.nasa.jpl.aerie.configwithdefaults;

import gov.nasa.jpl.aerie.configwithdefaults.generated.ActivityTypes;
import gov.nasa.jpl.aerie.configwithdefaults.generated.ConfigurationMapper;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinTestContext;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
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

  private final Mission model;

  public ConfigurationTest(final MerlinTestContext<ActivityTypes, Mission> ctx)
  throws InstantiationException
  {
    // Rely on config. defaults by instantiating config. with empty argument map
    final var config = new ConfigurationMapper().instantiate(Map.of());

    this.model = new Mission(ctx.registrar(), config);
    ctx.use(model);
  }

  @Test
  public void testDefaults() {
    assertThat(model.configuration.a()).isEqualTo(Configuration.Defaults.a);
    assertThat(model.configuration.b()).isEqualTo(Configuration.Defaults.b);
    assertThat(model.configuration.c()).isEqualTo(Configuration.Defaults.c);
  }
}
