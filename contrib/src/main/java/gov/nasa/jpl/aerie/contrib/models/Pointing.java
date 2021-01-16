package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.time.Duration;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * Model for a three-dimensional vector.
 * Each component contains a value and rate <code>Accumulator</code>, which exposes an underlying resource and
 * convenience methods.
 */
public final class Pointing<$Schema> extends Model {
  public final Component x, y, z;

  public Pointing(final Registrar<$Schema> registrar, final Vector3D initialVec) {
    this(registrar, initialVec, new Vector3D(0, 0, 0));
  }

  public Pointing(final Registrar<$Schema> registrar, final Vector3D initialVec, final Vector3D initialRate) {
    super(registrar);

    this.x = new Component(registrar, initialVec.getX(), initialRate.getX());
    this.y = new Component(registrar, initialVec.getY(), initialRate.getY());
    this.z = new Component(registrar, initialVec.getZ(), initialRate.getZ());
  }

  public Vector3D getVector() {
    return new Vector3D(this.x.value.get(), this.y.value.get(), this.z.value.get());
  }

  public Vector3D getRate() {
    return new Vector3D(this.x.rate.get(), this.y.rate.get(), this.z.rate.get());
  }

  public void addRate(final Vector3D delta) {
    this.x.rate.add(delta.getX());
    this.y.rate.add(delta.getY());
    this.z.rate.add(delta.getZ());
  }

  /** Slew to a target vector over a given duration. */
  public void slew(final Vector3D target, final Duration duration) {
    final var previousRate = getRate();
    addRate(previousRate.negate()); // Negate the previous rate to bring rate to <0, 0, 0>

    // rate = (v2 - v1) / duration
    final var desiredRate = target.subtract(getVector()).scalarMultiply(1/duration.ratioOver(Duration.SECOND));
    addRate(desiredRate);
    delay(duration);

    // Reset the rate post-slew
    addRate(desiredRate.negate()); // Negate the slew rate to bring rate back to <0, 0, 0>
    addRate(previousRate);         // Reset rate to previous rate
  }

  public final class Component {
    public final Accumulator<$Schema>.Volume value;
    public final Accumulator<$Schema>.Rate rate;

    public Component(final Registrar<$Schema> registrar, final double value, final double rate) {
      final var acc = new Accumulator<>(registrar, value, rate);
      this.value = acc.volume;
      this.rate = acc.rate;
    }
  }
}
