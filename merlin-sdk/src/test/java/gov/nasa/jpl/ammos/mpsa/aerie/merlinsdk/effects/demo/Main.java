package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities.ActivityReactor;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data.DataModelProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.ecology.LotkaVolterraModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.ecology.LotkaVolterraParameters;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.EventGraphProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.ScanningProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits.SumEffectTrait;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.*;

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

    System.out.println(graph);
    System.out.println();

    final var copied = graph.evaluate(new EventGraphProjection<>());
    final var migrated = graph
        .substitute(ev ->
            (Objects.equals(ev, "a"))  /* Drop 'a' atoms. */
              ? empty()
              : (Objects.equals(ev, "b")) /* Wrap 'b' atoms between two 'z' atoms. */
                ? sequentially(atom("z"), atom("b'"), atom("z"))
                : atom(ev));

    System.out.println(migrated);


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

  public static void stepSimulationExample() {
    final var next =
        concurrently(
            atom(Event.run("c")),
            atom(Event.run("b")),
            sequentially(
                atom(Event.log("z")),
                atom(Event.run("a"))));
    System.out.println(next);

    final var effects = next
        .evaluate(new ActivityReactor())
        .step(empty())
        .getLeft();
    System.out.println(effects);

    System.out.println();
  }

  public static void dataModelExample() {
    // Set up data model
    final var model = new DataModel();
    model.getDataBin("bin A").addRate(1.0);
  //  model.getDataBin("bin B").setVolume(5.0);

    // Prepare events
    final var graph =
        sequentially(
            atom(Event.addDataRate("bin A", 10)),
           atom(Event.addDataRate("bin A", 15))
           // atom(Event.addDataRate("bin B", 15)),
        /*    concurrently(
                atom(Event.clearBin("bin A")),
                atom(Event.addDataRate("bin B", -5)))*/
        );


    System.out.println("graph: " + graph);


    // Apply the graph to the model.
    final var projection = new DataModelProjection();

    graph.evaluate(projection).apply(model);

    System.out.println("model is " + model);

    System.out.println("data rate " + model.getDataBin("bin 1").getRate());

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
    //if (true) expressionTransformationExample();
    //if (true) stepSimulationExample();
    if (true) dataModelExample();
    //if (true) dynamicsExample();
  }
}
