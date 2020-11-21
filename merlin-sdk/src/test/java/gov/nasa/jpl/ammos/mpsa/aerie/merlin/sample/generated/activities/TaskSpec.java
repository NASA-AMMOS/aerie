package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

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

  public static Map<String, TaskSpecType<TaskSpec>> getTaskSpecTypes() {
    final var types = new HashMap<String, TaskSpecType<TaskSpec>>();
    types.put(FooActivityTaskSpec.descriptor.getName(), FooActivityTaskSpec.descriptor);
    return types;
  }

  public static TaskSpecType<TaskSpec> createDaemonType(final String name, final Runnable daemon) {
    return DaemonTaskSpec.getDescriptor(name, daemon);
  }
}
