package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;

public final class SimulationDriver {
  public static void main(final String[] args) {
    try {
      bar(FooAdaptation.create());
    } catch (final ActivityType.UnconstructableActivityException ex) {
      ex.printStackTrace();
    }
  }

  private static <$Schema, Event, Activity extends ActivityInstance>
  void bar(final Adaptation<$Schema, Event, Activity> adaptation)
  throws ActivityType.UnconstructableActivityException
  {
    baz(SimulationTimeline.create(adaptation.getResources().getSchema()), adaptation);
  }

  private static <$Timeline, Event, Activity extends ActivityInstance>
  void
  baz(
      final SimulationTimeline<$Timeline, Event> timeline,
      final Adaptation<? super $Timeline, Event, Activity> adaptation)
  throws ActivityType.UnconstructableActivityException
  {
    final var activity = adaptation
        .getActivityTypes()
        .get("foo")
        .instantiate(Map.of());

    final var scheduler = new Scheduler<$Timeline, Event, Activity>() {
      // TODO: Track and reduce candelabras of spawned tasks
      public History<$Timeline, Event> now = timeline.origin();

      @Override
      public void emit(final Event event) {
        now = now.emit(event);
      }

      @Override
      public String spawn(final Activity activity) {
        // TODO: Register the spawned activity and give it a name.
        return "";
      }

      @Override
      public String defer(final Duration delay, final Activity activity) {
        // TODO: Register the spawned activity and give it a name.
        return "";
      }

      @Override
      public History<$Timeline, Event> now() {
        return this.now;
      }
    };

    final var visitor = new ActivityStatus.Visitor<Boolean>() {
      @Override
      public Boolean completed() {
        // TODO: Emit an "activity end" event.
        return false;
      }

      @Override
      public Boolean awaiting(final String s) {
        // TODO: Yield this task if the awaited activity is not yet complete.
        return true;
      }

      @Override
      public Boolean delayed(final Duration delay) {
        // TODO: Yield this task and perform any other tasks between now and the resumption point.
        scheduler.now = scheduler.now.wait(delay);
        return true;
      }
    };

    final var task = adaptation.<$Timeline>createActivityTask(activity);

    boolean running = true;
    while (running) {
      // TODO: Emit an "activity start" event.
      running = task.step(scheduler).match(visitor);
    }

    final var resources = adaptation.getResources();
    System.out.print(scheduler.now.getDebugTrace());
    resources.getRealResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getDynamics(scheduler.now()));
    });
    resources.getDiscreteResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getRight().getDynamics(scheduler.now()));
    });
  }
}
