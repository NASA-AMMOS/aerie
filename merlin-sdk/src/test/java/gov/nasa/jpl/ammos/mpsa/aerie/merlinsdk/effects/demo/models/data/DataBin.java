package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public final class DataBin {
  private double rate;
  private double volume;

  public DataBin() {
    this.rate = 0.0;
    this.volume = 0.0;
  }

  public DataBin(final DataBin other) {
    this.rate = other.rate;
    this.volume = other.volume;
  }

  public void step(final Duration duration) {
    final var dt = duration.durationInMicroseconds / 1000000.0;
    this.volume += dt * this.rate;
  }

  public void addRate(final double delta) {
    this.rate += delta;
  }

  public void addVolume(final double delta) {
    this.volume += delta;
  }

  public void setRate(final double rate) {
    this.rate = rate;
  }

  public void setVolume(final double volume) {
    this.volume = volume;
  }

  public double getRate() {
    return this.rate;
  }

  public double getVolume() {
    return this.volume;
  }

  @Override
  public String toString() {
    return String.format("(%s per second, %s)", rate, volume);
  }
}
