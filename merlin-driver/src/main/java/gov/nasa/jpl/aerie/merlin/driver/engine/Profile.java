package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

/*package-local*/ record Profile<Dynamics>(List<Segment<Dynamics>> segments) {
  public record Segment<Dynamics>(Duration startOffset, Dynamics dynamics) {}

  public void append(final Duration currentTime, final Dynamics dynamics) {
    this.segments.add(new Segment<>(currentTime, dynamics));
  }
}
