package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;

import java.util.HashMap;
import java.util.Map;

// TODO: Automatically generate at compile time.
public abstract class ActivityInstance
    implements gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityInstance
{
  /* package-local */
  ActivityInstance() {}

  public abstract <$Schema> Task<$Schema, FooEvent, ActivityInstance, FooResources<$Schema>> createTask();

  public static Map<String, ActivityType<ActivityInstance>> getActivityTypes() {
    final var types = new HashMap<String, ActivityType<ActivityInstance>>();
    types.put(FooActivityInstance.descriptor.getName(), FooActivityInstance.descriptor);
    return types;
  }

  public static ActivityType<ActivityInstance> createDaemonType(final String name, final Runnable daemon) {
    return DaemonActivityInstance.getDescriptor(name, daemon);
  }
}
