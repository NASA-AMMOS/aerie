package gov.nasa.jpl.aerie.fooadaptation.models;

import gov.nasa.jpl.aerie.contrib.cells.linear.LinearAccumulationEffect;
import gov.nasa.jpl.aerie.contrib.cells.linear.LinearIntegrationCell;
import gov.nasa.jpl.aerie.fooadaptation.generated.Model;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;

import java.util.List;

/** Simple data model inspired by Clipper's example data system model diagrams. */
public final class SimpleData extends Model {
  public final InstrumentData a, b;
  public final RealResource totalVolume;

  public SimpleData(final Registrar registrar) {
    super(registrar);

    this.a = new InstrumentData(10.0, registrar.cell(new LinearIntegrationCell(0, 0)));
    this.b = new InstrumentData(5.0, registrar.cell(new LinearIntegrationCell(0, 0)));
    this.totalVolume = RealResource.add(this.a.volume, this.b.volume);

    registrar.descend("a").resource("volume", this.a.volume);
    registrar.descend("a").resource("rate", this.a.rate);
    registrar.descend("b").resource("volume", this.b.volume);
    registrar.descend("b").resource("rate", this.b.rate);
    registrar.resource("total_volume", this.totalVolume);
  }

  public void downlinkData() {
    List.of(this.a, this.b)
        .forEach(InstrumentData::clearVolume);
  }

  public static final class InstrumentData {
    private final CellRef<LinearAccumulationEffect, LinearIntegrationCell> ref;
    private final double activeRate;

    public final RealResource volume, rate;

    private InstrumentData(
        final double activeRate,
        final CellRef<LinearAccumulationEffect, LinearIntegrationCell> ref)
    {
      this.ref = ref;
      this.activeRate = activeRate;
      this.volume = RealResource.atom(ref, LinearIntegrationCell::getVolume);
      this.rate = RealResource.atom(ref, LinearIntegrationCell::getRate);
    }

    private void setRate(final double newRate) {
      final var currRate = this.ref.get().getRate().rate;
      this.ref.emit(LinearAccumulationEffect.addRate(newRate - currRate));
    }

    public void clearVolume() {
      this.ref.emit(LinearAccumulationEffect.clearVolume());
    }

    public void activate() {
      this.setRate(this.activeRate);
    }

    public void deactivate() {
      this.setRate(0.0);
    }

    public void setMode(final boolean isActive) {
      if (isActive) {
        this.activate();
      } else {
        this.deactivate();
      }
    }
  }
}
