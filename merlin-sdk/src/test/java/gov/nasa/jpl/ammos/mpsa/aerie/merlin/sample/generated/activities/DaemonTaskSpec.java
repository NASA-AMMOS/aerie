package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ReplayingTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.List;
import java.util.Map;

/* package-local */
final class DaemonTaskSpec {
  private DaemonTaskSpec() {}

  public static <$Schema> TaskSpecType<$Schema, ?> getDescriptor(
      final String name,
      final Runnable runnable,
      final ProxyContext<$Schema> rootContext)
  {
    return new TaskSpecType<$Schema, Runnable>() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Map<String, ValueSchema> getParameters() {
        return Map.of();
      }

      @Override
      public Runnable instantiateDefault() {
        return runnable;
      }

      @Override
      public Runnable instantiate(final Map<String, SerializedValue> arguments)
      throws UnconstructableTaskSpecException
      {
        for (final var ignored : arguments.entrySet()) {
          throw new UnconstructableTaskSpecException();
        }

        return runnable;
      }

      @Override
      public Map<String, SerializedValue> getArguments(final Runnable runnable) {
        return Map.of();
      }

      @Override
      public List<String> getValidationFailures(final Runnable runnable) {
        return List.of();
      }

      @Override
      public <$Timeline extends $Schema>
      gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task<$Timeline>
      createTask(final Runnable runnable) {
        return new ReplayingTask<>(rootContext, runnable);
      }
    };
  }
}
