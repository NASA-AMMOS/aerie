package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.model;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public final class SettableStateApplicator implements Applicator<SettableEffect<SerializedParameter>, RegisterState<SerializedParameter>> {
  private final SerializedParameter initialValue;

  public SettableStateApplicator(final SerializedParameter initialValue) {
    this.initialValue = initialValue;
  }

  @Override
  public RegisterState<SerializedParameter> initial() {
    return new RegisterState<>(this.initialValue);
  }

  @Override
  public RegisterState<SerializedParameter> duplicate(final RegisterState<SerializedParameter> register) {
    return new RegisterState<>(register);
  }

  @Override
  public void step(final RegisterState<SerializedParameter> register, final Duration duration) {
    register.step(duration);
  }

  @Override
  public void apply(final RegisterState<SerializedParameter> register, final SettableEffect<SerializedParameter> change) {
    change.visit(new SettableEffect.Visitor<>() {
      @Override
      public Object empty() {
        // do nothing
        return null;
      }

      @Override
      public Object setTo(final SerializedParameter value) {
        register.set(value);
        return null;
      }

      @Override
      public Object conflict() {
        register.setConflicted();
        return null;
      }
    });
  }
}
