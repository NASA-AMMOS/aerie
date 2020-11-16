package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationScope;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SolvableDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SimulationDriver {
  public static void main(final String[] args) {
    try {
      foo(new FooAdaptation());
    } catch (final ActivityType.UnconstructableActivityException ex) {
      ex.printStackTrace();
    }
  }

  private static <Activity extends ActivityInstance>
  void foo(final Adaptation<Activity> adaptation)
  throws ActivityType.UnconstructableActivityException
  {
    final var activityTypes = adaptation.getActivityTypes();

    final var activities = new ArrayList<Activity>();
    adaptation.getDaemons().forEach(activities::add);
    activities.add(activityTypes.get("foo").instantiate(Map.of()));

    for (final var activity : activities) {
      final var validationFailures = activity.getValidationFailures();
      validationFailures.forEach(System.out::println);
    }

    bar(activities, adaptation.createSimulationScope());
  }

  private static <$Schema, Event, Activity extends ActivityInstance>
  void bar(
      final List<Activity> activities,
      final SimulationScope<$Schema, Event, Activity> scope)
  {
    baz(activities, scope, SimulationTimeline.create(scope.getSchema()));
  }

  private static <$Timeline, Event, Activity extends ActivityInstance>
  void baz(
      final List<Activity> activities,
      final SimulationScope<? super $Timeline, Event, Activity> scope,
      final SimulationTimeline<$Timeline, Event> timeline)
  {
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

      @Override
      public <Solution> Solution ask(final SolvableDynamics<Solution, ?> resource, final Duration offset) {
        return resource.solve(new SolvableDynamics.Visitor() {
          @Override
          public Double real(final RealDynamics dynamics) {
            return new RealSolver().valueAt(dynamics, offset);
          }

          @Override
          public <ResourceType> ResourceType discrete(final ResourceType fact) {
            return fact;
          }
        });
      }
    };

    final var visitor = new ActivityStatus.Visitor<$Timeline, Boolean>() {
      @Override
      public Boolean completed() {
        // TODO: Emit an "activity end" event.
        return false;
      }

      @Override
      public Boolean awaiting(final String s) {
        // TODO: Yield this task until the awaited activity is complete.
        return true;
      }

      @Override
      public Boolean delayed(final Duration delay) {
        // TODO: Yield this task and perform any other tasks between now and the resumption point.
        scheduler.now = scheduler.now.wait(delay);
        return true;
      }

      @Override
      public <ResourceType, ConditionType> Boolean awaiting(
          final Resource<History<$Timeline, ?>, SolvableDynamics<ResourceType, ConditionType>> resource,
          final ConditionType condition)
      {
        // TODO: Yield this task until the awaited condition is met.
        return true;
      }
    };

    for (final var activity : activities) {
      System.out.println("Performing: " + activity.serialize());
      final var task = scope.<$Timeline>createActivityTask(activity);

      boolean running = true;
      while (running) {
        // TODO: Emit an "activity start" event.
        running = task.step(scheduler).match(visitor);
      }
    }

    System.out.print(scheduler.now.getDebugTrace());
    scope.getRealResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getDynamics(scheduler.now()));
    });
    scope.getDiscreteResources().forEach((name, resource) -> {
      System.out.printf("%-12s%s%n", name, resource.getRight().getDynamics(scheduler.now()));
    });
  }
}
