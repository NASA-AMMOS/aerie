package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.ecology;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public final class LotkaVolterraModel {
  private final LotkaVolterraParameters parameters;
  private double predatorDensity;
  private double preyDensity;

  public LotkaVolterraModel(final LotkaVolterraParameters parameters) {
    this.parameters = parameters;
    this.predatorDensity = parameters.initialPredatorDensity;
    this.preyDensity = parameters.initialPreyDensity;
  }

  public void step(final Duration duration) {
    final var dt = duration.durationInMicroseconds / 1000000.0;

    final var predatorDensityRate =
        ( (parameters.predatorGrowthRate * predatorDensity * preyDensity)
        - (parameters.preyDeathRate      * predatorDensity)
        );
    final var preyDensityRate =
        ( (parameters.preyGrowthRate * preyDensity)
        - (parameters.preyDeathRate * predatorDensity * preyDensity)
        );

    predatorDensity = predatorDensity + dt * predatorDensityRate;
    preyDensity = preyDensity + dt * preyDensityRate;
  }

  public double getPredatorDensity() {
    return this.predatorDensity;
  }

  public double getPreyDensity() {
    return this.preyDensity;
  }
}
