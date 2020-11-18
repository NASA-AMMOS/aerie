package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationScope;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities.TaskSpec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

// TODO: Automatically generate at compile time.
public final class FooAdaptation implements Adaptation<TaskSpec> {
  private final FooSimulationScope<?> scope = FooSimulationScope.create();
  private final Map<String, TaskSpecType<TaskSpec>> daemonTypes;
  private final Map<String, TaskSpecType<TaskSpec>> allTaskSpecTypes;

  public FooAdaptation() {
    final var allTaskSpecTypes = new HashMap<>(TaskSpec.getTaskSpecTypes());
    final var daemonTypes = new HashMap<String, TaskSpecType<TaskSpec>>();

    getDaemons("/daemons", this.scope.getRootModule(), (name, daemon) -> {
      final var daemonType = TaskSpec.createDaemonType(name, daemon);

      daemonTypes.put(daemonType.getName(), daemonType);
      allTaskSpecTypes.put(daemonType.getName(), daemonType);
    });

    this.daemonTypes = Collections.unmodifiableMap(daemonTypes);
    this.allTaskSpecTypes = Collections.unmodifiableMap(allTaskSpecTypes);
  }

  private static void getDaemons(
      final String namespace,
      final Module<?, ?> module,
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
  public Map<String, TaskSpecType<TaskSpec>> getTaskSpecificationTypes() {
    return this.allTaskSpecTypes;
  }

  @Override
  public Iterable<TaskSpec> getDaemons() {
    return this.daemonTypes
        .values()
        .stream()
        .map(TaskSpecType::instantiateDefault)
        .collect(Collectors.toList());
  }

  @Override
  public SimulationScope<?, TaskSpec> createSimulationScope() {
    return this.scope;
  }
}
