package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.PeelBananaActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers.PeelBananaActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ThreadedTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.List;
import java.util.Map;

public class PeelBananaActivityType<$Schema> implements TaskSpecType<$Schema, PeelBananaActivity> {
  private final PeelBananaActivityMapper mapper = new PeelBananaActivityMapper();

  private final ProxyContext<$Schema> rootContext;
  private final BanananationResources<$Schema> container;

  public PeelBananaActivityType(
      final ProxyContext<$Schema> rootContext,
      final BanananationResources<$Schema> container)
  {
    this.rootContext = rootContext;
    this.container = container;
  }

  @Override
  public String getName() {
    return this.mapper.getName();
  }

  @Override
  public Map<String, ValueSchema> getParameters() {
    return this.mapper.getParameters();
  }

  @Override
  public PeelBananaActivity instantiateDefault() {
    return this.mapper.instantiateDefault();
  }

  @Override
  public PeelBananaActivity instantiate(final Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException
  {
    return this.mapper.instantiate(arguments);
  }

  @Override
  public Map<String, SerializedValue> getArguments(final PeelBananaActivity activity) {
    return this.mapper.getArguments(activity);
  }

  @Override
  public List<String> getValidationFailures(final PeelBananaActivity activity) {
    // TODO: Extract validation messages from @Validation annotation at compile time.
    return this.mapper.getValidationFailures(activity);
  }

  @Override
  public <$Timeline extends $Schema> Task<$Timeline> createTask(final PeelBananaActivity activity) {
    final var task = activity.new EffectModel<$Schema>();
    task.setContext(this.rootContext);
    return new ThreadedTask<>(this.rootContext, () -> task.run(this.container));
  }
}
