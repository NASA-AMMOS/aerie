package gov.nasa.jpl.ammos.mpsa.aerie.banananation.processortest;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

public class LogProjection extends AnnotationTestEventProjection<String> {
  LogProjection(EffectTrait<String> trait) {
    super(trait);
  }

  @Override
  public String log(final String message) {
    return message;
  }
}
