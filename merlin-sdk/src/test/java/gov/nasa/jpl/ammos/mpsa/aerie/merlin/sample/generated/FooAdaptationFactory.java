package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;

// TODO: Automatically generate at compile time.
public final class FooAdaptationFactory implements AdaptationFactory {
  @Override
  public Adaptation<?> instantiate() {
    return new FooAdaptation<>(Schema.builder());
  }
}
