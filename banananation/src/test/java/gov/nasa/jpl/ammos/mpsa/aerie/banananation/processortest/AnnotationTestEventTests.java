package gov.nasa.jpl.ammos.mpsa.aerie.banananation.processortest;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import org.junit.Test;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.atom;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.concurrently;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.sequentially;
import static org.junit.Assert.assertEquals;

public final class AnnotationTestEventTests {
  @Test
  public void eventClassGenerationAndUsage() {
    final EventGraph<AnnotationTestEvent> graph = sequentially(
      sequentially(
        atom(AnnotationTestEvent.log("start")),
        concurrently(
          atom(AnnotationTestEvent.log("banana")),
          atom(AnnotationTestEvent.log("apple")))),
      atom(AnnotationTestEvent.log("end")));

    LogEffectTrait st = new LogEffectTrait();
    String result = graph.evaluate(new LogProjection(st));
    assertEquals("start(apple | banana)end", result);
  }
}
