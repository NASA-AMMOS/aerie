package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities.ActivityReactor;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities.ScheduleItem;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities.SchedulingEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.ecology.LotkaVolterraModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.ecology.LotkaVolterraParameters;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.EventGraphProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.ScanningProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SumEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.concurrently;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.empty;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.atom;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.sequentially;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.TreeLogger.displayTree;

public final class Main {
  private static <Var, Context>
  EventGraph<Pair<Context, Var>> scanOver(
      final EventGraph<Var> expression,
      final Projection<Var, Context> projection
  ) {
    return expression
        .evaluate(new ScanningProjection<>(projection))
        .step(projection.empty())
        .getRight()
        .getRight();
  }

  public static void expressionTransformationExample() {
    final var graph =
        sequentially(
            atom("a"),
            atom("b"),
            concurrently(atom("c"), atom("d")),
            atom("e"));

    final var copied = graph.evaluate(new EventGraphProjection<>());
    final var migrated = graph
        .substitute(ev ->
            (Objects.equals(ev, "a")) /* Drop 'a' atoms. */
              ? empty()
              : (Objects.equals(ev, "b")) /* Wrap 'b' atoms between two 'z' atoms. */
                ? sequentially(atom("z"), atom("b'"), atom("z"))
                : atom(ev));

    System.out.println(graph);
    System.out.println(displayTree(
        scanOver(graph, new EventGraphProjection<>()),
        p -> String.format("<{%s}, %s>", p.getLeft(), p.getRight())));
    System.out.println(displayTree(
        scanOver(graph, Projection.from(new SumEffectTrait(), x -> (double) x.length())),
        p -> String.format("<%s, %s>", p.getLeft(), p.getRight())));
    System.out.println(copied);
    System.out.println(migrated);
    System.out.println();
  }

  private static <T> void stepSimulationExampleHelper(final SimulationTimeline<T, Event> database) {
    final var projections = new Querier<>(database);
    final var reactor = new ActivityReactor<>(projections);

    final var id1 = UUID.randomUUID().toString();
    final var id2 = UUID.randomUUID().toString();
    final var id3 = UUID.randomUUID().toString();

    final var schedule = new PriorityQueue<Pair<Duration, EventGraph<SchedulingEvent<T>>>>(Comparator.comparing(Pair::getKey));
    final var completed = new HashSet<String>();
    final var conditioned = new HashMap<String, Set<Pair<String, PVector<Time<T, Event>>>>>();
    schedule.add(Pair.of(Duration.ZERO, sequentially(
        atom(new SchedulingEvent.InstantiateActivity<>(id1, "c")),
        atom(new SchedulingEvent.InstantiateActivity<>(id2, "b")),
        atom(new SchedulingEvent.InstantiateActivity<>(id3, "a")),
        concurrently(
            sequentially(atom(new SchedulingEvent.ResumeActivity<>(id1, TreePVector.empty()))),
            sequentially(atom(new SchedulingEvent.ResumeActivity<>(id2, TreePVector.empty()))),
            sequentially(atom(new SchedulingEvent.ResumeActivity<>(id3, TreePVector.empty())))))));

    var now = database.origin();
    var currentTime = Duration.ZERO;

    {
      var durationFromStart = Duration.ZERO;
      for (final var point : now.evaluate(new EventGraphProjection<>())) {
        durationFromStart = durationFromStart.plus(point.getKey());
        System.out.printf("%10s: %s\n", durationFromStart, point.getValue());
      }
      for (final var point : schedule) {
        System.out.printf("%10s: %s\n", point.getKey(), point.getValue());
      }
      System.out.println();
    }

    while (!schedule.isEmpty()) {
      // Get the current time, and any events occurring at this time.
      final Duration delta;
      EventGraph<SchedulingEvent<T>> events;
      {
        final var job = schedule.poll();
        events = job.getRight();

        delta = job.getKey().minus(currentTime);
        while (!schedule.isEmpty() && schedule.peek().getKey().equals(job.getKey())) {
          events = EventGraph.concurrently(events, schedule.poll().getValue());
        }
      }

      // Step up to the new time.
      now = now.wait(delta);
      currentTime = currentTime.plus(delta);

      // React to the events scheduled at this time.
      final var result = events.evaluate(reactor).apply(now);
      now = result.getLeft();
      final var newJobs = result.getRight();

      // Accumulate the freshly scheduled items into our scheduling timeline.
      for (final var entry : newJobs.entrySet()) {
        final var activityId = entry.getKey();
        final var rule = entry.getValue();
        if (rule instanceof ScheduleItem.Defer) {
          final var duration = ((ScheduleItem.Defer<T, Event>) rule).duration;
          final var milestones = ((ScheduleItem.Defer<T, Event>) rule).milestones;
          schedule.add(Pair.of(duration.plus(currentTime), EventGraph.atom(new SchedulingEvent.ResumeActivity<>(activityId, milestones))));
        } else if (rule instanceof ScheduleItem.OnCompletion) {
          final var waitId = ((ScheduleItem.OnCompletion<T, Event>) rule).waitOn;
          final var milestones = ((ScheduleItem.OnCompletion<T, Event>) rule).milestones;
          if (completed.contains(waitId)) {
            schedule.add(Pair.of(currentTime, EventGraph.atom(new SchedulingEvent.ResumeActivity<>(activityId, milestones))));
          } else {
            conditioned.computeIfAbsent(waitId, k -> new HashSet<>()).add(Pair.of(activityId, milestones));
          }
        } else if (rule instanceof ScheduleItem.Complete) {
          completed.add(activityId);

          final var conditionedActivities = conditioned.remove(entry.getKey());
          if (conditionedActivities == null) continue;

          for (final var conditionedTask : conditionedActivities) {
            final var conditionedId = conditionedTask.getKey();
            final var milestones = conditionedTask.getValue();
            schedule.add(Pair.of(currentTime, EventGraph.atom(new SchedulingEvent.ResumeActivity<>(conditionedId, milestones))));
          }
        }
      }

      // Display some debugging information.
      {
        var durationFromStart = Duration.ZERO;
        for (final var point : now.evaluate(new EventGraphProjection<>())) {
          durationFromStart = durationFromStart.plus(point.getKey());
          System.out.printf("%10s: %s\n", durationFromStart, point.getValue());
        }
        for (final var point : schedule) {
          System.out.printf("%10s: %s\n", point.getKey(), point.getValue());
        }
        System.out.println();
      }
    }
  }

  public static void stepSimulationExample() {
    stepSimulationExampleHelper(SimulationTimeline.create());
  }

  public static void dataModelExample() {
    // Prepare events
    final var graph =
        sequentially(
            atom(Event.addDataRate("bin A", 10)),
            atom(Event.addDataRate("bin B", 15)),
            concurrently(
                atom(Event.clearDataRate("bin A")),
                atom(Event.addDataRate("bin B", -5))));
    System.out.println(graph);

    // (Show the effect this graph would have)
    {
      final var trait = new DataEffectEvaluator();

      System.out.println(graph.evaluate(trait));

      // The above is semantically equivalent to having done this from the start:
      System.out.println(
          trait.sequentially(
              trait.sequentially(
                  Map.of("bin A", SettableEffect.add(10.0)),
                  Map.of("bin B", SettableEffect.add(15.0))),
              trait.concurrently(
                  Map.of("bin A", SettableEffect.setTo(0.0)),
                  Map.of("bin B", SettableEffect.add(-5.0)))));
    }

    // Apply the graph to the model.
    final var evaluator = new DataEffectEvaluator();
    final var applicator = new DataModelApplicator();

    final var model = applicator.initial();
    System.out.println(model);
    applicator.step(model, Duration.of(5, TimeUnit.SECONDS));
    System.out.println(model);
    applicator.apply(model, graph.evaluate(evaluator));
    System.out.println(model);
    applicator.step(model, Duration.of(5, TimeUnit.SECONDS));
    System.out.println(model);

    System.out.println();
  }

  public static void dynamicsExample() {
    final var model = new LotkaVolterraModel(new LotkaVolterraParameters(
        0.1, 0.4, 10.0,
        1.1, 0.4, 10.0));

    System.out.printf("%10.7f\t%10.7f\n", model.getPredatorDensity(), model.getPreyDensity());
    for (var i = 0; i < 500; i += 1) {
      model.step(Duration.of(100, TimeUnit.MILLISECONDS));
      System.out.printf("%10.7f\t%10.7f\n", model.getPredatorDensity(), model.getPreyDensity());
    }
    System.out.println();
  }

  public static void main(final String[] args) {
    if (true) expressionTransformationExample();
    if (true) stepSimulationExample();
    if (true) dataModelExample();
    if (true) dynamicsExample();
  }
}
