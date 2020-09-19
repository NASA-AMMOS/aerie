package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ActivityExecutor;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ReplayingTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.unmodifiableMap;

public final class TaskFactory<T, Event, Activity> {
  private final ActivityExecutor<T, Event, Activity> executor;
  private final Map<String, Optional<String>> taskParents;
  private final Map<String, Activity> taskSpecifications;
  private final Optional<String> taskId;

  private TaskFactory(
      final ActivityExecutor<T, Event, Activity> executor,
      final Map<String, Optional<String>> taskParents,
      final Map<String, Activity> taskSpecifications,
      final Optional<String> taskId)
  {
    this.executor = executor;
    this.taskParents = taskParents;
    this.taskSpecifications = taskSpecifications;
    this.taskId = taskId;
  }

  public TaskFactory(final ActivityExecutor<T, Event, Activity> executor) {
    this(executor, new HashMap<>(), new HashMap<>(), Optional.empty());
  }

  public void execute(final ReactionContext<T, Event, Activity> ctx, final String activityId, final Activity activity) {
    this.executor.execute(ctx, activityId, activity);
  }

  public Map<String, Optional<String>> getTaskParents() {
    return unmodifiableMap(this.taskParents);
  }

  public Map<String, Activity> getTaskSpecifications() {
    return unmodifiableMap(this.taskSpecifications);
  }


  public ReplayingTask<T, Event, Activity> createReplayingTask(final Activity activity) {
    return this.createReplayingTask(UUID.randomUUID().toString(), activity);
  }

  public ReplayingTask<T, Event, Activity> createReplayingTask(final String id, final Activity activity) {
    this.taskParents.putIfAbsent(id, this.taskId);
    this.taskSpecifications.put(id, activity);

    final var factory = new TaskFactory<>(this.executor, this.taskParents, this.taskSpecifications, Optional.of(id));
    return new ReplayingTask<>(factory, id, activity);
  }
}
