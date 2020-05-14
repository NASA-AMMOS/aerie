package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EventGraph;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections.ReactingProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.DefaultEventHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;

import java.util.function.Function;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.TreeLogger.displayTree;

public final class ActivityReactor
    extends ReactingProjection<Event>
    implements DefaultEventHandler<Function<EventGraph<Event>, EventGraph<Event>>>
{
  @Override
  protected final EventGraph<Event> react(final EventGraph<Event> context, final Event event) {
    return event.visit(this).apply(context);
  }

  @Override
  public final Function<EventGraph<Event>, EventGraph<Event>> run(final String activityType) {
    switch (activityType) {
      case "a":
        return past -> EventGraph.sequentially(
            EventGraph.atom(Event.log("Hello, from Activity A!")),
            EventGraph.atom(Event.log(displayTree(past))));
      case "b":
        return past -> EventGraph.sequentially(
            EventGraph.atom(Event.log("Before B")),
            EventGraph.atom(Event.run("a")),
            EventGraph.atom(Event.log("After B")));
      default:
        return this.unhandled();
    }
  }

  @Override
  public final Function<EventGraph<Event>, EventGraph<Event>> unhandled() {
    return past -> EventGraph.empty();
  }
}
