package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.timeline.History;

import java.util.Optional;

public record EngineQuery<$Schema, Event, State>(
    gov.nasa.jpl.aerie.merlin.timeline.Query<$Schema, Event, State> query
) implements Query<$Schema, Event, State> {
  public Optional<Duration> getCurrentExpiry(final History<? extends $Schema> now) {
    return query.getCurrentExpiry(now.ask(query));
  }
}
