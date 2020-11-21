package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/* package-local */
final class DaemonTaskSpec extends TaskSpec {
  private final Runnable runnable;

  private DaemonTaskSpec(final Runnable runnable) {
    this.runnable = Objects.requireNonNull(runnable);
  }

  @Override
  public <$Schema> Task<$Schema, FooResources<$Schema>> createTask() {
    return new Task<>() {
      @Override
      public void run(final FooResources<$Schema> resources) {
        DaemonTaskSpec.this.runnable.run();
      }
    };
  }

  @Override
  public Map<String, SerializedValue> getArguments() {
    return Map.of();
  }

  public static <$Schema> TaskSpecType<$Schema, TaskSpec> getDescriptor(final String name, final Runnable runnable) {
    final var instance = new DaemonTaskSpec(runnable);

    return new TaskSpecType<>() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Map<String, ValueSchema> getParameters() {
        return Map.of();
      }

      @Override
      public TaskSpec instantiateDefault() {
        return instance;
      }

      @Override
      public TaskSpec instantiate(final Map<String, SerializedValue> arguments)
      throws UnconstructableTaskSpecException
      {
        for (final var ignored : arguments.entrySet()) {
          throw new UnconstructableTaskSpecException();
        }

        return instance;
      }

      @Override
      public Map<String, SerializedValue> getArguments(final TaskSpec taskSpec) {
        return taskSpec.getArguments();
      }

      @Override
      public List<String> getValidationFailures(final TaskSpec taskSpec) {
        return taskSpec.getValidationFailures();
      }
    };
  }
}
