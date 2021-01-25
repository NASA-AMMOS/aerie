package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.linear.LinearAccumulationEffect;
import gov.nasa.jpl.aerie.contrib.cells.linear.LinearIntegrationCell;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;

public final class Accumulator implements RealResource {
  private final CellRef<LinearAccumulationEffect, LinearIntegrationCell> ref;

  public final Rate rate = new Rate();

  public Accumulator(final Registrar registrar) {
    this(registrar, 0.0, 0.0);
  }

  public Accumulator(final Registrar registrar, final double initialVolume, final double initialRate) {
    this.ref = registrar.cell(new LinearIntegrationCell(initialVolume, initialRate));

    registrar.resource("volume", this);
    registrar.resource("rate", this.rate);
  }

  @Override
  public RealDynamics getDynamics() {
    return this.ref.get().getVolume();
  }


  public final class Rate implements RealResource {
    @Override
    public RealDynamics getDynamics() {
      return Accumulator.this.ref.get().getRate();
    }

    public void add(final double delta) {
      Accumulator.this.ref.emit(LinearAccumulationEffect.addRate(delta));
    }
  }
}
