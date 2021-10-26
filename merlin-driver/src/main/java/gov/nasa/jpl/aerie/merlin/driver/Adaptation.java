package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.Directive;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Adaptation<$Schema, Model> {
  private final Phantom<$Schema, Model> model;
  private final LiveCells initialCells;
  private final List<ResourceFamily<$Schema, ?>> resourceFamilies;
  private final Map<String, TaskSpecType<Model, ?>> taskSpecTypes;
  private final List<Initializer.TaskFactory<$Schema>> daemons;

  public Adaptation(
      final Phantom<$Schema, Model> model,
      final LiveCells initialCells,
      final List<ResourceFamily<$Schema, ?>> resourceFamilies,
      final List<Initializer.TaskFactory<$Schema>> daemons,
      final Map<String, TaskSpecType<Model, ?>> taskSpecTypes)
  {
    this.model = Objects.requireNonNull(model);
    this.initialCells = Objects.requireNonNull(initialCells);
    this.resourceFamilies = Collections.unmodifiableList(resourceFamilies);
    this.taskSpecTypes = Collections.unmodifiableMap(taskSpecTypes);
    this.daemons = Collections.unmodifiableList(daemons);
  }

  public Phantom<$Schema, Model> getModel() {
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

  public <$Timeline extends $Schema> Task<$Timeline> getDaemon() {
    return new Task<>() {
      @Override
      public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
        Adaptation.this.daemons.forEach(daemon -> scheduler.spawn(daemon.create()));
        return TaskStatus.completed();
      }

      @Override
      public void reset() {
      }
    };
  }

  public Iterable<ResourceFamily<$Schema, ?>> getResourceFamilies() {
    return this.resourceFamilies;
  }

  public LiveCells getInitialCells() {
    return this.initialCells;
  }
}
