package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.function.Supplier;

public final class DynamicReactionContext<T, Activity, Event> implements ReactionContext<T, Activity, Event> {
  private final Supplier<ReactionContext<T, Activity, Event>> activeContext;

  public DynamicReactionContext(final Supplier<ReactionContext<T, Activity, Event>> activeContext) {
    this.activeContext = activeContext;
  }

  @Override
  public Time<T, Event> now() {
    return this.activeContext.get().now();
  }

  @Override
  public void emit(final Event event) {
    this.activeContext.get().emit(event);
  }

  @Override
  public void delay(final Duration duration) {
    this.activeContext.get().delay(duration);
  }

  @Override
  public String spawn(final Activity activity) {
    return this.activeContext.get().spawn(activity);
  }

  @Override
  public String spawnAfter(final Duration delay, final Activity activity) {
    return this.activeContext.get().spawnAfter(delay, activity);
  }

  @Override
  public void waitForActivity(final String activityId) {
    this.activeContext.get().waitForActivity(activityId);
  }

  @Override
  public void waitForChildren() {
    this.activeContext.get().waitForChildren();
  }
}
