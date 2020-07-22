package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

public class SampleSampleEventProjection extends SampleEventProjection<String> {
  SampleSampleEventProjection(EffectTrait<String> trait) {
    super(trait);
  }

  @Override
  public String log(final String message) {
    return message;
  }
}
