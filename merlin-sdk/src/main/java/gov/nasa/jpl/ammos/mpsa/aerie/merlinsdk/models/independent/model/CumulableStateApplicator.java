package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.model;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public final class CumulableStateApplicator implements Applicator<Double, RegisterState<Double>> {
  private final double initialValue;

  public CumulableStateApplicator(final double initialValue) {
    this.initialValue = initialValue;
  }

  @Override
  public RegisterState<Double> initial() {
    return new RegisterState<>(this.initialValue);
  }

  @Override
  public RegisterState<Double> duplicate(final RegisterState<Double> register) {
    return new RegisterState<>(register);
  }

  @Override
  public void step(final RegisterState<Double> register, final Duration duration) {
    register.step(duration);
  }

  @Override
  public void apply(final RegisterState<Double> register, final Double change) {
    register.set(register.get() + change);
  }
}
