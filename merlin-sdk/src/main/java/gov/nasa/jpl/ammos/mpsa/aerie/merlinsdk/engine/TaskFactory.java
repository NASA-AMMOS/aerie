package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ActivityExecutor;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ReplayingTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Collections.unmodifiableMap;

public final class TaskFactory<T, Event, Activity> {
  private final Function<Activity, SerializedActivity> extractSpecification;
  private final ActivityExecutor<T, Event, Activity> executor;

  private final Map<String, Optional<String>> taskParents;
  private final Map<String, SerializedActivity> taskSpecifications;
  private final Optional<String> taskId;

  private TaskFactory(
      final Function<Activity, SerializedActivity> extractSpecification,
      final ActivityExecutor<T, Event, Activity> executor,
      final Map<String, Optional<String>> taskParents,
      final Map<String, SerializedActivity> taskSpecifications,
      final Optional<String> taskId)
  {
    this.extractSpecification = extractSpecification;
    this.executor = executor;
    this.taskParents = taskParents;
    this.taskSpecifications = taskSpecifications;
    this.taskId = taskId;
  }

  public TaskFactory(
      final Function<Activity, SerializedActivity> extractSpecification,
      final ActivityExecutor<T, Event, Activity> executor)
  {
    this(extractSpecification, executor, new HashMap<>(), new HashMap<>(), Optional.empty());
  }

  public void execute(final ReactionContext<T, Event, Activity> ctx, final String activityId, final Activity activity) {
    this.executor.execute(ctx, activityId, activity);
  }

  public Map<String, Optional<String>> getTaskParents() {
    return unmodifiableMap(this.taskParents);
  }

  public Map<String, SerializedActivity> getTaskSpecifications() {
    return unmodifiableMap(this.taskSpecifications);
  }


  public ReplayingTask<T, Event, Activity> createReplayingTask(final Activity activity) {
    return this.createReplayingTask(UUID.randomUUID().toString(), activity);
  }

  public ReplayingTask<T, Event, Activity> createReplayingTask(final String id, final Activity activity) {
    this.taskParents.putIfAbsent(id, this.taskId);
    this.taskSpecifications.put(id, this.extractSpecification.apply(activity));

    final var factory = new TaskFactory<>(
        this.extractSpecification,
        this.executor,
        this.taskParents,
        this.taskSpecifications,
        Optional.of(id));
    return new ReplayingTask<>(factory, id, activity);
  }
}
