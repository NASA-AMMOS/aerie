package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.ecology;

public final class LotkaVolterraParameters {
  /* package */ final double predatorGrowthRate;
  /* package */ final double predatorDeathRate;
  /* package */ final double initialPredatorDensity;

  /* package */ final double preyGrowthRate;
  /* package */ final double preyDeathRate;
  /* package */ final double initialPreyDensity;

  public LotkaVolterraParameters(
      final double predatorGrowthRate,
      final double predatorDeathRate,
      final double initialPredatorDensity,
      final double preyGrowthRate,
      final double preyDeathRate,
      final double initialPreyDensity
  ) {
    this.predatorGrowthRate = predatorGrowthRate;
    this.predatorDeathRate = predatorDeathRate;
    this.initialPredatorDensity = initialPredatorDensity;

    this.preyGrowthRate = preyGrowthRate;
    this.preyDeathRate = preyDeathRate;
    this.initialPreyDensity = initialPreyDensity;
  }
}
