package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.Directive;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MissionModel<Model> {
  private final Model model;
  private final LiveCells initialCells;
  private final Map<String, Resource<?>> resources;
  private final Map<String, TaskSpecType<Model, ?>> taskSpecTypes;
  private final List<Initializer.TaskFactory> daemons;

  public MissionModel(
      final Model model,
      final LiveCells initialCells,
      final Map<String, Resource<?>> resources,
      final List<Initializer.TaskFactory> daemons,
      final Map<String, TaskSpecType<Model, ?>> taskSpecTypes)
  {
    this.model = Objects.requireNonNull(model);
    this.initialCells = Objects.requireNonNull(initialCells);
    this.resources = Collections.unmodifiableMap(resources);
    this.taskSpecTypes = Collections.unmodifiableMap(taskSpecTypes);
    this.daemons = Collections.unmodifiableList(daemons);
  }

  public Model getModel() {
    return this.model;
  }

  public Map<String, TaskSpecType<Model, ?>> getTaskSpecificationTypes() {
    return this.taskSpecTypes;
  }

  public Directive<Model, ?> instantiateDirective(final SerializedActivity specification)
  throws TaskSpecType.UnconstructableTaskSpecException
  {
    return Directive.instantiate(this.taskSpecTypes.get(specification.getTypeName()), specification.getParameters());
  }

  public Task getDaemon() {
    return new Task() {
      @Override
      public TaskStatus step(final Scheduler scheduler) {
        MissionModel.this.daemons.forEach(daemon -> scheduler.spawn(daemon.create()));
        return TaskStatus.completed();
      }

      @Override
      public void reset() {
      }
    };
  }

  public Map<String, Resource<?>> getResources() {
    return this.resources;
  }

  public LiveCells getInitialCells() {
    return this.initialCells;
  }
}
