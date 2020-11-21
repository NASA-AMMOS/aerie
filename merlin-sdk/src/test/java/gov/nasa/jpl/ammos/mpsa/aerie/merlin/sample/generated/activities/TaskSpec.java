package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Automatically generate at compile time.
public abstract class TaskSpec {
  /* package-local */
  TaskSpec() {}

  public abstract <$Schema> Task<$Schema, FooResources<$Schema>> createTask();
  public abstract Map<String, SerializedValue> getArguments();

  public List<String> getValidationFailures() {
    return List.of();
  }

  public static <$Schema> Map<String, TaskSpecType<$Schema, TaskSpec>> getTaskSpecTypes(
      final ProxyContext<$Schema> rootContext,
      final FooResources<$Schema> container)
  {
    final var types = new HashMap<String, TaskSpecType<$Schema, TaskSpec>>();
    final var descriptor = FooActivityTaskSpec.<$Schema>getDescriptor(rootContext, container);
    types.put(descriptor.getName(), descriptor);
    return types;
  }

  public static <$Schema> TaskSpecType<$Schema, TaskSpec> createDaemonType(
      final String name,
      final Runnable daemon,
      final ProxyContext<$Schema> rootContext) {
    return DaemonTaskSpec.getDescriptor(name, daemon, rootContext);
  }
}
