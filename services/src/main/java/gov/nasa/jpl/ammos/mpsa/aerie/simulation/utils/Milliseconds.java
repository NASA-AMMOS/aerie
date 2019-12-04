package gov.nasa.jpl.ammos.mpsa.aerie.simulation.utils;

public final class Milliseconds {
  public final long value;

  public Milliseconds(final long value) {
    this.value = value;
  }

  static public Milliseconds ms(final long value) {
    return new Milliseconds(value);
  }
}
