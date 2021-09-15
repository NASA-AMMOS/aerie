package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TaskFactory<$Timeline, Model>
    implements Iterable<Pair<String, TaskInfo<$Timeline>>>
{
  private final Map<String, TaskSpecType<Model, ?>> taskTypes;

  private final Map<String, TaskInfo<$Timeline>> taskInfo = new HashMap<>();
  private int nextTaskId = 0;

  public TaskFactory(final Map<String, TaskSpecType<Model, ?>> taskTypes) {
    this.taskTypes = Objects.requireNonNull(taskTypes);
  }

  public TaskInfo<$Timeline>
  createTask(
      final Phantom<? super $Timeline, Model> model,
      final String typeName,
      final Map<String, SerializedValue> arguments,
      final Optional<String> parent)
  throws SimulationDriver.InstantiationException
  {
    final var taskId = Integer.toString(this.nextTaskId++);
    final var specType = this.taskTypes.get(typeName);
    final var reifiedTask = instantiate(model, specType, arguments);
    final var info = new TaskInfo<>(taskId, parent, reifiedTask.task(), Optional.of(new SerializedActivity(typeName, reifiedTask.arguments())));

    this.taskInfo.put(taskId, info);

    return info;
  }

  public TaskInfo<$Timeline>
  createAnonymousTask(final Task<$Timeline> task, final Optional<String> parent) {
    final var taskId = Integer.toString(this.nextTaskId++);
    final var info = new TaskInfo<>(taskId, parent, task, Optional.empty());

    this.taskInfo.put(taskId, info);

    return info;
  }

  private record ReifiedTask<$Timeline> (Task<$Timeline> task, Map<String, SerializedValue> arguments) {}

  private <Spec> ReifiedTask<$Timeline>
  instantiate(
      final Phantom<? super $Timeline, Model> model,
      final TaskSpecType<Model, Spec> specType,
      final Map<String, SerializedValue> arguments)
  throws SimulationDriver.InstantiationException
  {
    final Spec spec;
    try {
      spec = specType.instantiate(arguments);
    } catch (final TaskSpecType.UnconstructableTaskSpecException ex) {
      throw new SimulationDriver.InstantiationException(specType.getName(), arguments, ex);
    }

    return new ReifiedTask<>(specType.createTask(model, spec), specType.getArguments(spec));
  }

  public TaskInfo<$Timeline> get(final String id) {
    return this.taskInfo.get(id);
  }

  @Override
  public Iterator<Pair<String, TaskInfo<$Timeline>>> iterator() {
    final var iter = this.taskInfo.entrySet().iterator();

    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public Pair<String, TaskInfo<$Timeline>> next() {
        final var entry = iter.next();
        return Pair.of(entry.getKey(), entry.getValue());
      }
    };
  }
}
