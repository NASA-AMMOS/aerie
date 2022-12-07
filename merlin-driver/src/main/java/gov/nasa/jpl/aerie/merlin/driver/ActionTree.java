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
 * The root of the the tree, representing the plan as a whole, is the only absolutely-scheduled entity.
 *
 * @param action
 *   An action to be performed.
 * @param startAnchor
 *   The set of actions anchored to the start of this action
 * @param endAnchor
 *   The set of actions anchored to the end of this action.
 */
public record ActionTree(
    Action action,
    List<Pair<Duration, ActionTree>> startAnchor,
    List<Pair<Duration, ActionTree>> endAnchor
) {
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
    final var roots = new ArrayList<Pair<Duration, ActionTree>>(schedule.size());

    for (final var entry : schedule.entrySet()) {
      final var directiveId = entry.getKey();
      final var startOffset = entry.getValue().getLeft();
      final var serializedDirective = entry.getValue().getRight();

      final var task = model.getTaskFactory(serializedDirective);

      roots.add(Pair.of(startOffset, new ActionTree(
          new Action.Directive<>(directiveId, Unit.UNIT, task),
          List.of(),
          List.of())));
    }

    return new ActionTree(new Action.AnonymousEvent(duration), Collections.unmodifiableList(roots), List.of());
  }

  /**
   * Compile this action tree to a simulable task which spawns its immediate children at their scheduled times..
   *
   * @param activityTopic
   *   The topic on which to emit each constituent activity's ID when it spawns.
   * @return
   *   A simulable task that spawns the plan's immediate children at their scheduled times.
   */
  public TaskFactory<Unit, Unit> toTask(final Topic<ActivityInstanceId> activityTopic) {
    return $ -> ActionTree.spawnAll(activityTopic, this.startAnchor)
        .andThen(this.action.toTask(activityTopic))
        .andThen(ActionTree.spawnAll(activityTopic, this.endAnchor));
  }

  private static Task<Unit, Unit> spawnAll(final Topic<ActivityInstanceId> activityTopic, final List<Pair<Duration, ActionTree>> actions) {
    return (scheduler, unit) -> {
      for (final var entry : actions) {
        final var startOffset = entry.getLeft();
        final var action = entry.getRight();

        scheduler.spawn(
            $$ -> Task
                .spawning(action.toTask(activityTopic))
                .butFirst(Task.delaying(startOffset)),
            Unit.UNIT);
      }

      return TaskStatus.completed(Unit.UNIT);
    };
  }

  /**
   * Recursively re-schedule each action relative to the earliest resolvable point in the plan.
   *
   * <p> The following transformations are repeatedly made to the plan until no further transformation
   * is possible. </p>
   *
   * <ul>
   *   <li> If an action (the "child") is anchored to the start of another action (its "parent"), and its parent itself
   *   has a parent, then the child is re-anchored to its parent's parent, and the offset of its parent is added to
   *   its own offset. </li>
   *
   *   <li> If an action is anchored to the end of a plan or event, it is re-anchored to the start of that plan
   *   or event, and the duration of the plan or event is added to the action's offset. </li>
   * </ul>
   *
   * <p> Neither of these transformations affects the absolute time at which an action is scheduled. Instead, these
   * transformations cause an action to become anchored the earliest point at which we know its precise start time.
   * Notably, any actions anchored to the end of an activity cannot be migrated any earlier, as we do not know the
   * duration of an activity until it has actually terminated. </p>
   *
   * @return
   *   A new, equivalent plan with all actions re-anchored to the first point at which its absolute start time
   *   becomes known.
   */
  public ActionTree resolveOffsets() {
    final var startAnchor = new ArrayList<Pair<Duration, ActionTree>>(this.startAnchor.size());

    // Recursively resolve each start-anchored child, then add it and its own start-anchored children to ours.
    // But, instead of doing this in two steps, we pass to each child its own offset and the accumulating list of
    // start-anchored actions, so that it can add itself and any descendants directly.
    final var self = this.liftInto(startAnchor, Duration.ZERO);

    // `self.startAnchor` should always be empty after lifting, but to be robust against future changes,
    // there's no harm in accumulating it anyway.
    startAnchor.addAll(self.startAnchor);

    return new ActionTree(self.action(), startAnchor, self.endAnchor());
  }

  /**
   * Lift as many descendant actions as possible into the parent's anchor, and return an ActionTree with all remaining
   * actions lifted as high as possible.
   *
   * @param anchor
   *   The anchor of the parent which this action tree is scheduled relative to.
   * @param offset
   *   The offset from the parent's anchor that this action tree is scheduled at.
   * @return
   *   A residual ActionTree with all actions stripped that could be re-scheduled against the parent.
   */
  private ActionTree liftInto(final List<Pair<Duration, ActionTree>> anchor, final Duration offset) {
    // Our start-anchored actions can always be lifted into the parent. Just add their offset to ours.
    for (final var entry : this.startAnchor) {
      final var childOffset = entry.getLeft().plus(offset);
      anchor.add(Pair.of(childOffset, entry.getRight().liftInto(anchor, childOffset)));
    }

    final var duration$ = this.action.getDuration();
    final List<Pair<Duration, ActionTree>> endAnchor;
    if (duration$.isPresent()) {
      endAnchor = List.of();

      // If we know how long this action will take, we can re-schedule our end-anchored actions
      // against our start anchor, and from there lift them into the parent as well. Just add their offset
      // to our own offset and duration.
      for (final var entry : this.endAnchor) {
        final var childOffset = entry.getLeft().plus(duration$.get()).plus(offset);
        anchor.add(Pair.of(childOffset, entry.getRight().liftInto(anchor, childOffset)));
      }
    } else {
      endAnchor = new ArrayList<>(this.endAnchor.size());

      // If we don't know how long this action takes, we can't re-schedule our immediate dependents.
      // However, we can at least lift *their* descendants as high as possible.
      for (final var entry : this.endAnchor) {
        final var childOffset = entry.getLeft();
        anchor.add(Pair.of(childOffset, entry.getRight().liftInto(endAnchor, childOffset)));
      }
    }

    // Return this action as the root of the residual tree of actions.
    return new ActionTree(this.action, List.of(), Collections.unmodifiableList(endAnchor));
  }
}
