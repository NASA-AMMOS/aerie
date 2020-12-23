package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class ActivityType<$Schema, Activity>
    implements TaskSpecType<$Schema, Activity>
{
  private final ActivityMapper<Activity> mapper;

  protected ActivityType(final ActivityMapper<Activity> mapper) {
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Override
  public final String getName() {
    return this.mapper.getName();
  }

  @Override
  public final Map<String, ValueSchema> getParameters() {
    return this.mapper.getParameters();
  }

  @Override
  public final Activity instantiateDefault() {
    return this.mapper.instantiateDefault();
  }

  @Override
  public final Activity instantiate(final Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException
  {
    return this.mapper.instantiate(arguments);
  }

  @Override
  public final Map<String, SerializedValue> getArguments(final Activity activity) {
    return this.mapper.getArguments(activity);
  }

  @Override
  public final List<String> getValidationFailures(final Activity activity) {
    return this.mapper.getValidationFailures(activity);
  }
}
