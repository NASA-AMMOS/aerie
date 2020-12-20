package gov.nasa.jpl.ammos.mpsa.aerie.banananation2.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;

// TODO: Automatically generate at compile time.
public final class BanananationAdaptationFactory implements AdaptationFactory {
  @Override
  public Adaptation<?> instantiate() {
    return new Banananation<>(Schema.builder());
  }
}
