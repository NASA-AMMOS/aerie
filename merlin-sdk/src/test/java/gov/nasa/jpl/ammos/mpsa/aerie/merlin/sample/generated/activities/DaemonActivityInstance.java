package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.Map;
import java.util.Objects;

/* package-local */
final class DaemonActivityInstance extends ActivityInstance {
  private final String name;
  private final Runnable runnable;

  private DaemonActivityInstance(final String name, final Runnable runnable) {
    this.name = Objects.requireNonNull(name);
    this.runnable = Objects.requireNonNull(runnable);
  }

  @Override
  public <$Schema> Task<$Schema, FooEvent, ActivityInstance, FooResources<$Schema>> createTask() {
    return new Task<>() {
      @Override
      public void run(final FooResources<$Schema> resources) {
        DaemonActivityInstance.this.runnable.run();
      }
    };
  }

  @Override
  public SerializedActivity serialize() {
    return new SerializedActivity(this.name, Map.of());
  }

  public static ActivityType<ActivityInstance> getDescriptor(final String name, final Runnable runnable) {
    final var instance = new DaemonActivityInstance(name, runnable);

    return new ActivityType<>() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Map<String, ValueSchema> getParameters() {
        return Map.of();
      }

      @Override
      public ActivityInstance instantiateDefault() {
        return instance;
      }

      @Override
      public ActivityInstance instantiate(final Map<String, SerializedValue> arguments)
      throws UnconstructableActivityException
      {
        for (final var ignored : arguments.entrySet()) {
          throw new UnconstructableActivityException();
        }

        return instance;
      }
    };
  }
}
