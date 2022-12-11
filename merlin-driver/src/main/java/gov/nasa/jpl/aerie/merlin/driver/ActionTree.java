package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A tree of simulation actions, each scheduled relative to (or "anchored to") the start or end of its parent.
 *
 * @param offset
 *   The relative offset at which to perform this action.
 * @param action
 *   An action to be performed.
 * @param startAnchor
 *   The set of actions anchored to the start of this action
 * @param endAnchor
 *   The set of actions anchored to the end of this action.
 */
public record ActionTree(Duration offset, Action action, List<ActionTree> startAnchor, List<ActionTree> endAnchor) {
  /**
   * A helper method for creating an action tree from a set of serialized activities anchored to the start of a plan.
   *
   * @param duration
   *   The length of time over which the plan extends.
   * @param model
   *   The model to instantiate activities from.
   * @param schedule
   *   The set of scheduled activities, together with their ID and offset.
   * @return
   *   An action tree containing the requested activities deserialized from the model.
   * @param <Model>
   *   The type of mission model acted upon by the given activities.
   * @throws InstantiationException
   *   when a scheduled activity could not be instantiated
   */
  public static <Model> ActionTree from(
      final Duration duration,
      final MissionModel<Model> model,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule
  ) throws InstantiationException {
    final var roots = new ArrayList<ActionTree>(schedule.size());

    for (final var entry : schedule.entrySet()) {
      final var directiveId = entry.getKey();
      final var startOffset = entry.getValue().getLeft();
      final var serializedDirective = entry.getValue().getRight();

      final var task = model.getTaskFactory(serializedDirective);

      roots.add(new ActionTree(
          startOffset,
          new Action.Directive<>(directiveId, Unit.UNIT, task),
          List.of(),
          List.of()));
    }

    return new ActionTree(
        Duration.ZERO,
        new Action.AnonymousEvent(duration),
        Collections.unmodifiableList(roots),
        List.of());
  }

  /**
   * Compile this action tree to a simulable task which spawns its immediate children at their scheduled times.
   * Equivalent to calling {@link #toTask(Topic, Duration)} with {@code Duration.ZERO}.
   *
   * @param activityTopic
   *   The topic on which to emit each constituent activity's ID when it spawns.
   * @return
   *   A simulable task that spawns the plan's immediate children at their scheduled times.
   */
  public TaskFactory<Unit, Unit> toTask(final Topic<ActivityInstanceId> activityTopic) {
    return this.toTask(activityTopic, Duration.ZERO);
  }

  /**
   * Compile this action tree to a simulable task which spawns its immediate children at their scheduled times, all
   * shifted relative to a given initial offset.
   *
   * @param activityTopic
   *   The topic on which to emit each constituent activity's ID when it spawns.
   * @param offset
   *   The initial offset to apply to the whole tree.
   * @return
   *   A simulable task that spawns the plan's immediate children at their scheduled times.
   */
  public TaskFactory<Unit, Unit> toTask(final Topic<ActivityInstanceId> activityTopic, final Duration offset) {
    return $ -> {
      // Move each field into a local to avoid closing over `this` in the subtasks.
      final var startAnchor = this.startAnchor;
      final var action = this.action;
      final var endAnchor = this.endAnchor;

      // Spawn everything whose start time is known when this action begins.
      final var startOffset = offset.plus(this.offset);
      final Task<Unit, Unit> before = (scheduler, unit) -> {
        for (final var child : startAnchor) {
          scheduler.spawn(child.toTask(activityTopic, startOffset), Unit.UNIT);
        }

        return TaskStatus.completed(Unit.UNIT);
      };

      // Spawn the action itself.
      final Duration endOffset;
      final Task<Unit, Unit> during;
      {
        final var duration$ = action.getDuration();
        if (duration$.isEmpty()) {
          // If we don't know how long the action will take, block on the action,
          // so that when we regain control we're ready to spawn the end-anchored actions immediately.
          endOffset = Duration.ZERO;
          during = Task
              .calling(action.toTask(activityTopic))
              .butFirst(Task.delaying(startOffset))
              .andThen(Task.completed(Unit.UNIT));
        } else {
          // If we know how long the action will take, don't block on it before spawning end-anchored actions.
          // Just accumulate the action's duration so we can spawn the end-anchored actions early.
          endOffset = startOffset.plus(duration$.get());
          during = Task.spawning($$ -> Task
              .calling(action.toTask(activityTopic))
              .butFirst(Task.delaying(startOffset)));
        }
      }

      // Spawn everything whose start time is known when this action ends.
      final Task<Unit, Unit> after = (scheduler, unit) -> {
        for (final var child : endAnchor) {
          scheduler.spawn(child.toTask(activityTopic, endOffset), Unit.UNIT);
        }

        return TaskStatus.completed(Unit.UNIT);
      };

      return before.andThen(during).andThen(after);
    };
  }
}
