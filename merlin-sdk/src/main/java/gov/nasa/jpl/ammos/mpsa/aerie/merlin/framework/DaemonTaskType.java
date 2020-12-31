package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueSchema;

import java.util.List;
import java.util.Map;

public final class DaemonTaskType<$Schema> implements TaskSpecType<$Schema, Runnable> {
  private final String name;
  private final Runnable runnable;
  private final DynamicCell<Context<$Schema>> rootContext;

  public DaemonTaskType(final String name, final Runnable runnable, final DynamicCell<Context<$Schema>> rootContext) {
    this.name = name;
    this.runnable = runnable;
    this.rootContext = rootContext;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Map<String, ValueSchema> getParameters() {
    return Map.of();
  }

  @Override
  public Runnable instantiateDefault() {
    return this.runnable;
  }

  @Override
  public Runnable instantiate(final Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException
  {
    for (final var ignored : arguments.entrySet()) {
      throw new UnconstructableTaskSpecException();
    }

    return this.runnable;
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
  public <$Timeline extends $Schema> Task<$Timeline> createTask(final Runnable runnable) {
    return new ThreadedTask<>(this.rootContext, runnable);
  }
}
