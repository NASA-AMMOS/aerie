package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.EventGraphProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.ScanningProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SumEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities.ActivityReactor;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.Querier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.ecology.LotkaVolterraModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.ecology.LotkaVolterraParameters;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Objects;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.concurrently;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.empty;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.atom;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.sequentially;

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
            (Objects.equals(ev, "a"))  /* Drop 'a' atoms. */
              ? empty()
              : (Objects.equals(ev, "b")) /* Wrap 'b' atoms between two 'z' atoms. */
                ? sequentially(atom("z"), atom("b'"), atom("z"))
                : atom(ev));

    System.out.println(graph);
    System.out.println(
        scanOver(graph, new EventGraphProjection<>())
            .map(p -> String.format("<{%s}, %s>", p.getLeft(), p.getRight())));
    System.out.println(
        scanOver(graph, Projection.from(new SumEffectTrait(), x -> (double) x.length()))
            .map(p -> String.format("<%s, %s>", p.getLeft(), p.getRight())));
    System.out.println(copied);
    System.out.println(migrated);
    System.out.println();
  }

  public static <T> ReactionContext<T> createSimulator(final SimulationTimeline<T, Event> timeline) {
    final var projections = new Querier<>(timeline);
    final var reactor = new MasterReactor<T, Event>();

    final var activityReactor = new ActivityReactor<>(projections, reactor);
    reactor.addReactor(event -> event.visit(activityReactor));

    return new ReactionContext<>(projections, reactor, timeline.origin());
  }

  public static void stepSimulationExample() {
    final var database = SimulationTimeline.<Event>create();
    final var simulator = createSimulator(database);

    final var next =
        concurrently(
            atom(Event.run("c")),
            atom(Event.run("b")),
            sequentially(
                atom(Event.log("z")),
                atom(Event.run("a"))));
    System.out.println(next);

    simulator.react(next);
    for (final var point : simulator.getCurrentTime().evaluate(new EventGraphProjection<>())) {
      System.out.printf("%8.8s: %s\n", point.getKey(), point.getValue());
    }

    System.out.println();
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
