package gov.nasa.jpl.aerie.merlin.driver.timeline;

import gov.nasa.jpl.aerie.merlin.protocol.model.Aggregator;

import java.util.Optional;

public interface EventGraphEvaluator {
  <Effect> Optional<Effect> evaluate(Aggregator<Effect> aggregator, Selector<Effect> selector, EventGraph<Event> graph);
}
