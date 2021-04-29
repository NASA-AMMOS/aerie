package gov.nasa.jpl.aerie.merlin.protocol;

import java.util.Optional;

public interface Condition<$Schema> {
  Optional<Duration> nextSatisfied(Querier<? extends $Schema> now, Duration atLatest);
}
