package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class MissionModel<Model> {
  private final Model model;
  private final LiveCells initialCells;
  private final Map<String, Resource<?>> resources;
  private final List<SerializableTopic<?>> topics;
  private final DirectiveTypeRegistry<Model> directiveTypes;
  private final List<Initializer.TaskFactory<?>> daemons;

  public MissionModel(
      final Model model,
      final LiveCells initialCells,
      final Map<String, Resource<?>> resources,
      final List<SerializableTopic<?>> topics,
      final List<Initializer.TaskFactory<?>> daemons,
      final DirectiveTypeRegistry<Model> directiveTypes)
  {
    this.model = Objects.requireNonNull(model);
    this.initialCells = Objects.requireNonNull(initialCells);
    this.resources = Collections.unmodifiableMap(resources);
    this.topics = Collections.unmodifiableList(topics);
    this.directiveTypes = Objects.requireNonNull(directiveTypes);
    this.daemons = Collections.unmodifiableList(daemons);
  }

  public Model getModel() {
    return this.model;
  }

  public DirectiveTypeRegistry<Model> getDirectiveTypes() {
    return this.directiveTypes;
  }

  public Task<?> createTask(final SerializedActivity specification) throws InstantiationException {
    return this.directiveTypes
        .directiveTypes()
        .get(specification.getTypeName())
        .createTask(this.model, specification.getArguments());
  }

  public Task<Unit> getDaemon() {
    return scheduler -> {
      MissionModel.this.daemons.forEach(daemon -> scheduler.spawn(daemon.create()));
      return TaskStatus.completed(Unit.UNIT);
    };
  }

  public Map<String, Resource<?>> getResources() {
    return this.resources;
  }

  public LiveCells getInitialCells() {
    return this.initialCells;
  }

  public Iterable<SerializableTopic<?>> getTopics() {
    return this.topics;
  }

  public record SerializableTopic<EventType> (
      String name,
      Topic<EventType> topic,
      OutputType<EventType> outputType
  ) {}
}
