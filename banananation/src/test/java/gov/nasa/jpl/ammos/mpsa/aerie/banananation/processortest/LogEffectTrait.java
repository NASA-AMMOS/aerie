package gov.nasa.jpl.ammos.mpsa.aerie.banananation.processortest;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

public class LogEffectTrait implements EffectTrait<String> {
  @Override
  public String empty() {
    return "";
  }

  @Override
  public String sequentially(String prefix, String suffix) {
    return prefix + suffix;
  }

  @Override
  public String concurrently(String left, String right) {
    if (left.compareTo(right) < 0)
      return "(" + left + " | " + right + ")";
    else
      return "(" + right + " | " + left + ")";
  }
}
