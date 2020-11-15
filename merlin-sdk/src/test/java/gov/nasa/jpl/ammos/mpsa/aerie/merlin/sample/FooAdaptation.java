package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationScope;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities.ActivityInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

// TODO: Automatically generate at compile time.
public final class FooAdaptation implements Adaptation<ActivityInstance> {
  private final FooSimulationScope<?> scope = FooSimulationScope.create();
  private final Map<String, ActivityType<ActivityInstance>> daemonTypes;
  private final Map<String, ActivityType<ActivityInstance>> allActivityTypes;

  public FooAdaptation() {
    final var allActivityTypes = new HashMap<>(ActivityInstance.getActivityTypes());
    final var daemonTypes = new HashMap<String, ActivityType<ActivityInstance>>();

    getDaemons("/daemons", this.scope.getRootModule(), (name, daemon) -> {
      final var daemonType = ActivityInstance.createDaemonType(name, daemon);

      daemonTypes.put(daemonType.getName(), daemonType);
      allActivityTypes.put(daemonType.getName(), daemonType);
    });

    this.daemonTypes = Collections.unmodifiableMap(daemonTypes);
    this.allActivityTypes = Collections.unmodifiableMap(allActivityTypes);
  }

  private static void getDaemons(
      final String namespace,
      final Module<?, ?, ?> module,
      final BiConsumer<String, Runnable> receiver)
  {
    for (final var daemon : module.getDaemons().entrySet()) {
      receiver.accept(namespace + "/" + daemon.getKey(), daemon.getValue());
    }

    for (final var submodule : module.getSubmodules().entrySet()) {
      getDaemons(namespace + "/" + submodule.getKey(), submodule.getValue(), receiver);
    }
  }

  @Override
  public Map<String, ActivityType<ActivityInstance>> getActivityTypes() {
    return this.allActivityTypes;
  }

  @Override
  public Iterable<ActivityInstance> getDaemons() {
    return this.daemonTypes
        .values()
        .stream()
        .map(ActivityType::instantiateDefault)
        .collect(Collectors.toList());
  }

  @Override
  public SimulationScope<?, ?, ActivityInstance> createSimulationScope() {
    return this.scope;
  }
}
