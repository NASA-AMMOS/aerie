package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Iterator;

public record Profile<Dynamics>(SlabList<Segment<Dynamics>> segments)
implements Iterable<Profile.Segment<Dynamics>> {
  public record Segment<Dynamics>(Duration startOffset, Dynamics dynamics) {}

  public Profile() {
    this(new SlabList<>());
  }

  public void append(final Duration currentTime, final Dynamics dynamics) {
    this.segments.append(new Segment<>(currentTime, dynamics));
  }

  @Override
  public Iterator<Segment<Dynamics>> iterator() {
    return this.segments.iterator();
  }
}
