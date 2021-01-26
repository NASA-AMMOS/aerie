package gov.nasa.jpl.aerie.fooadaptation.models;

import java.util.List;

import gov.nasa.jpl.aerie.contrib.cells.linear.LinearAccumulationEffect;
import gov.nasa.jpl.aerie.contrib.cells.linear.LinearIntegrationCell;
import gov.nasa.jpl.aerie.fooadaptation.generated.Model;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;

/** Simple data model inspired by Clipper's example data system model diagrams. */
public final class SimpleData extends Model {

  private final CellRef<LinearAccumulationEffect, LinearIntegrationCell> queryA, queryB;
  private final LinearIntegrationCell cellInstrumentA, cellInstrumentB;

  public final RealResource volumeA, rateA, volumeB, rateB, totalVolume;

  public SimpleData(final Registrar registrar) {
    super(registrar);

    this.cellInstrumentA = new LinearIntegrationCell(0, 0);
    this.cellInstrumentB = new LinearIntegrationCell(0, 0);
    this.queryA = registrar.cell(this.cellInstrumentA);
    this.queryB = registrar.cell(this.cellInstrumentB);
    this.volumeA = registrar.resource("volume_a", RealResource.atom(this.queryA, LinearIntegrationCell::getVolume));
    this.rateA = registrar.resource("rate_a", RealResource.atom(this.queryA, LinearIntegrationCell::getRate));
    this.volumeB = registrar.resource("volume_b", RealResource.atom(this.queryB, LinearIntegrationCell::getVolume));
    this.rateB = registrar.resource("rate_b", RealResource.atom(this.queryB, LinearIntegrationCell::getRate));
    this.totalVolume = registrar.resource("total_volume", RealResource.add(volumeA, volumeB));
  }

  private void setInstrumentRate(final CellRef<LinearAccumulationEffect, LinearIntegrationCell> query, final double newRate) {
    // Emit an effect to set rate
    final var currRate = query.get().getRate().rate;
    query.emit(LinearAccumulationEffect.addRate(newRate - currRate));
  }

  public void toggleInstrumentA(final boolean on) {
    setInstrumentRate(this.queryA, on ? 10.0 : 0);
  }

  public void toggleInstrumentB(final boolean on) {
    setInstrumentRate(this.queryB, on ? 5.0 : 0);
  }

  public void downlinkData() {
    // Emit an effect to clear volume
    for (final var q : List.of(this.queryA, this.queryB))
      q.emit(LinearAccumulationEffect.clearVolume());
  }
}
