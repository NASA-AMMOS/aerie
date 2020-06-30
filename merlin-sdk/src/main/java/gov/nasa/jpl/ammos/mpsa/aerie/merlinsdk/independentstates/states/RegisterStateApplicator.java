package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public final class RegisterStateApplicator implements Applicator<SettableEffect<Double, Double>, RegisterState> {
  private final double initialValue;

  public RegisterStateApplicator(final double initialValue) {
    this.initialValue = initialValue;
  }

  @Override
  public RegisterState initial() {
    return new RegisterState(initialValue);
  }

  @Override
  public RegisterState duplicate(final RegisterState register) {
    return new RegisterState(register);
  }

  @Override
  public void step(final RegisterState register, final Duration duration) {
    register.step(duration);
  }

  @Override
  public void apply(final RegisterState register, final SettableEffect<Double, Double> change) {
    change.visit(new SettableEffect.VoidVisitor<>() {
      @Override
        public void setTo(final Double value) {
          register.set(value);
        }

        @Override
        public void add(final Double delta) {
          final var conflicted = register.isConflicted();
          register.set(register.get() + delta);
          if (conflicted) register.setConflicted();;
        }

        @Override
        public void conflict() {
          register.setConflicted();
        }
    });
  }
}
