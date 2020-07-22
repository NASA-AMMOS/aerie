package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.events;

import org.junit.Test;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;

import static org.junit.Assert.assertEquals;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.concurrently;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.empty;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.atom;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph.sequentially;


public class EventsTest {
  @Test
  public void sampleEventGenerationAndUsage() {
    final EventGraph<SampleEvent> graph = sequentially(
      sequentially(
        atom(SampleEvent.log("start")),
        concurrently(
          atom(SampleEvent.log("banana")),
          atom(SampleEvent.log("apple")))),
      atom(SampleEvent.log("end")));
    StringEffectTrait st = new StringEffectTrait();
    String result = graph.evaluate(new SampleSampleEventProjection(st));
    assertEquals("start(apple | banana)end", result);
  }
}
