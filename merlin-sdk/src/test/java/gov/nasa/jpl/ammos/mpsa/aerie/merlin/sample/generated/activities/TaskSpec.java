package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;

import java.util.HashMap;
import java.util.Map;

// TODO: Automatically generate at compile time.
public final class TaskSpec {
  private TaskSpec() {}

  public static <$Schema> Map<String, TaskSpecType<$Schema, ?>> getTaskSpecTypes(
      final ProxyContext<$Schema> rootContext,
      final FooResources<$Schema> container)
  {
    final var types = new HashMap<String, TaskSpecType<$Schema, ?>>();
    final var descriptor = new FooActivityType<>(rootContext, container);
    types.put(descriptor.getName(), descriptor);
    return types;
  }

  public static <$Schema> TaskSpecType<$Schema, ?> createDaemonType(
      final String name,
      final Runnable daemon,
      final ProxyContext<$Schema> rootContext) {
    return new DaemonTaskType<>(name, daemon, rootContext);
  }
}
