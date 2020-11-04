package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import java.util.Objects;

public final class FooEvent {
  public final double d;

  public FooEvent(double d) {
    this.d = d;
  }

  @Override
  public String toString() {
    return Objects.toString(this.d);
  }
}
