package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.model;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events.DefaultIndependentStateEventHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events.IndependentStateEvent;

import java.util.Objects;

public final class CumulableEffectEvaluator
    implements Projection<IndependentStateEvent, Double>,
    DefaultIndependentStateEventHandler<Double>
{
  private final String name;

  public CumulableEffectEvaluator(final String name) {
    this.name = name;
  }

  @Override
  public Double unhandled() {
    return this.empty();
  }

  @Override
  public Double add(final String binName, final double amount) {
    return (Objects.equals(binName, this.name))
        ? amount
        : this.empty();
  }

  @Override
  public Double empty() {
    return 0.0;
  }

  @Override
  public Double atom(final IndependentStateEvent atom) {
    return atom.visit(this);
  }

  @Override
  public Double sequentially(final Double prefix, final Double suffix) {
    return prefix + suffix;
  }

  @Override
  public Double concurrently(final Double left, final Double right) {
    return left + right;
  }
}
