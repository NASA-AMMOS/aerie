package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;

import java.util.HashMap;
import java.util.Map;

// TODO: Automatically generate at compile time.
public abstract class TaskSpec
    implements gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpec
{
  /* package-local */
  TaskSpec() {}

  public abstract <$Schema> Task<$Schema, FooResources<$Schema>> createTask();

  public static Map<String, TaskSpecType<TaskSpec>> getTaskSpecTypes() {
    final var types = new HashMap<String, TaskSpecType<TaskSpec>>();
    types.put(FooActivityTaskSpec.descriptor.getName(), FooActivityTaskSpec.descriptor);
    return types;
  }

  public static TaskSpecType<TaskSpec> createDaemonType(final String name, final Runnable daemon) {
    return DaemonTaskSpec.getDescriptor(name, daemon);
  }
}
