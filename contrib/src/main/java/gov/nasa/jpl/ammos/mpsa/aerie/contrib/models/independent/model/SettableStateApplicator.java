package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.model;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.traits.SettableEffect;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public final class SettableStateApplicator implements Applicator<SettableEffect<SerializedValue>, RegisterState<SerializedValue>> {
  private final SerializedValue initialValue;

  public SettableStateApplicator(final SerializedValue initialValue) {
    this.initialValue = initialValue;
  }

  @Override
  public RegisterState<SerializedValue> initial() {
    return new RegisterState<>(this.initialValue);
  }

  @Override
  public RegisterState<SerializedValue> duplicate(final RegisterState<SerializedValue> register) {
    return new RegisterState<>(register);
  }

  @Override
  public void step(final RegisterState<SerializedValue> register, final Duration duration) {
    register.step(duration);
  }

  @Override
  public void apply(final RegisterState<SerializedValue> register, final SettableEffect<SerializedValue> change) {
    change.visit(new SettableEffect.Visitor<>() {
      @Override
      public Object empty() {
        // do nothing
        return null;
      }

      @Override
      public Object setTo(final SerializedValue value) {
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
