package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class MissionModel<Model> {
  private final Model model;
  private final LiveCells initialCells;
  private final Map<String, Resource<?>> resources;
  private final Map<Topic<?>, SerializableTopic<?>> topics;
  public final Topic<Topic<?>> queryTopic = new Topic<>();
  private final DirectiveTypeRegistry<Model> directiveTypes;
  private final Map<String, TaskFactory<?>> daemons;
  private final Map<TaskFactory<?>, String> daemonIds;

  public MissionModel(
      final Model model,
      final LiveCells initialCells,
      final Map<String, Resource<?>> resources,
      final Map<Topic<?>, SerializableTopic<?>> topics,
      final Map<String, TaskFactory<?>> daemons,
      final DirectiveTypeRegistry<Model> directiveTypes)
  {
    this.model = Objects.requireNonNull(model);
    this.initialCells = Objects.requireNonNull(initialCells);
    this.resources = Collections.unmodifiableMap(resources);
    this.topics = Collections.unmodifiableMap(topics);
    this.directiveTypes = Objects.requireNonNull(directiveTypes);
    this.daemons = Collections.unmodifiableMap(new HashMap<>(daemons));
    this.daemonIds = Collections.unmodifiableMap(daemons.entrySet().stream()
                                                        .collect(Collectors.toMap(t -> t.getValue(),
                                                                                  t -> t.getKey(),
                                                                                  (v1, v2) -> v1,
                                                                                  HashMap::new)));
  }

  public Model getModel() {
    return this.model;
  }

  public DirectiveTypeRegistry<Model> getDirectiveTypes() {
    return this.directiveTypes;
  }

  public TaskFactory<?> getTaskFactory(final SerializedActivity specification) throws InstantiationException {
    return this.directiveTypes
        .directiveTypes()
        .get(specification.getTypeName())
        .getTaskFactory(this.model, specification.getArguments());
  }

  public TaskFactory<Unit> getDaemon() {
    return executor -> new Task<>() {
      @Override
      public TaskStatus<Unit> step(final Scheduler scheduler) {
        MissionModel.this.daemonIds.keySet().forEach($ -> scheduler.spawn(InSpan.Fresh, $));
        return TaskStatus.completed(Unit.UNIT);
      }

      @Override
      public Task<Unit> duplicate(final Executor executor) {
        return this;
      }
    };
  }
  public String getDaemonId(TaskFactory<?> taskFactory) {
    return daemonIds.get(taskFactory);
  }

  public TaskFactory<?> getDaemon(String id) {
    return daemons.get(id);
  }

  public boolean isDaemon(TaskFactory<?> state) {
    return MissionModel.this.daemonIds.keySet().contains(state);
  }

  /**
   * @return whether daemons should be rerun when reusing a past simulation.
   */
  public boolean rerunDaemons() {
    return false;  // TODO: This should be specified in the adaptation somehow.
                   //       Default should be false, but unit tests need it true.
  }

  public Map<String, Resource<?>> getResources() {
    return this.resources;
  }

  public LiveCells getInitialCells() {
    return this.initialCells;
  }

  public Map<Topic<?>, SerializableTopic<?>> getTopics() {
    return this.topics;
  }

  public boolean hasDaemons(){
    return !this.daemons.isEmpty();
  }

  public record SerializableTopic<EventType> (
      String name,
      Topic<EventType> topic,
      OutputType<EventType> outputType
  ) {}
}
