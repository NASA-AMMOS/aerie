package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpec;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationScope;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SolvableDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.FooAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.SimulationTimeline;
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
    } catch (final TaskSpecType.UnconstructableTaskSpecException ex) {
      ex.printStackTrace();
    }
  }

  private static <AdaptationTaskSpec extends TaskSpec>
  void foo(final Adaptation<AdaptationTaskSpec> adaptation)
  throws TaskSpecType.UnconstructableTaskSpecException
  {
    final var activityTypes = adaptation.getTaskSpecificationTypes();

    final var taskSpecs = new ArrayList<AdaptationTaskSpec>();
    adaptation.getDaemons().forEach(taskSpecs::add);
    taskSpecs.add(activityTypes.get("foo").instantiate(Map.of()));

    for (final var taskSpec : taskSpecs) {
      final var validationFailures = taskSpec.getValidationFailures();
      validationFailures.forEach(System.out::println);
    }

    bar(taskSpecs, adaptation.createSimulationScope());
  }

  private static <$Schema, Event, AdaptationTaskSpec extends TaskSpec>
  void bar(
      final List<AdaptationTaskSpec> taskSpecs,
      final SimulationScope<$Schema, Event, AdaptationTaskSpec> scope)
  {
    baz(taskSpecs, scope, SimulationTimeline.create(scope.getSchema()));
  }

  private static <$Timeline, Event, AdaptationTaskSpec extends TaskSpec>
  void baz(
      final List<AdaptationTaskSpec> taskSpecs,
      final SimulationScope<? super $Timeline, Event, AdaptationTaskSpec> scope,
      final SimulationTimeline<$Timeline, Event> timeline)
  {
    final var scheduler = new Scheduler<$Timeline, Event, AdaptationTaskSpec>() {
      // TODO: Track and reduce candelabras of spawned tasks
      public History<$Timeline, Event> now = timeline.origin();

      @Override
      public void emit(final Event event, final Query<? super $Timeline, Event, ?> query) {
        now = now.emit(event, query);
      }

      @Override
      public String spawn(final AdaptationTaskSpec taskSpec) {
        // TODO: Register the spawned activity and give it a name.
        return "";
      }

      @Override
      public String defer(final Duration delay, final AdaptationTaskSpec taskSpec) {
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

    final var visitor = new TaskStatus.Visitor<$Timeline, Boolean>() {
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

    for (final var taskSpec : taskSpecs) {
      System.out.println("Performing " + taskSpec.getTypeName() + " with arguments " + taskSpec.getArguments());
      final var task = scope.<$Timeline>createTask(taskSpec);

      boolean running = true;
      while (running) {
        // TODO: Emit a "task start" event.
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
