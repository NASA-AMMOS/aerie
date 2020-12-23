package gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.mappers.FooActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ThreadedTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

import java.util.List;
import java.util.Map;

// TODO: Automatically generate at compile time.
public final class FooActivityType<$Schema> implements TaskSpecType<$Schema, FooActivity> {
  private final FooActivityMapper mapper = new FooActivityMapper();

  private final DynamicCell<Context<$Schema>> rootContext;
  private final FooResources<$Schema> container;

  public FooActivityType(final DynamicCell<Context<$Schema>> rootContext, final FooResources<$Schema> container) {
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
  public FooActivity instantiateDefault() {
    return this.mapper.instantiateDefault();
  }

  @Override
  public FooActivity instantiate(final Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException
  {
    return this.mapper.instantiate(arguments);
  }

  @Override
  public Map<String, SerializedValue> getArguments(final FooActivity activity) {
    return this.mapper.getArguments(activity);
  }

  @Override
  public List<String> getValidationFailures(final FooActivity activity) {
    return this.mapper.getValidationFailures(activity);
  }

  @Override
  public <$Timeline extends $Schema> Task<$Timeline> createTask(final FooActivity activity) {
    return new ThreadedTask<>(
        this.rootContext,
        () -> activity
            .new EffectModel<$Schema>()
            .runWith(this.rootContext.get(), this.container));
  }
}
