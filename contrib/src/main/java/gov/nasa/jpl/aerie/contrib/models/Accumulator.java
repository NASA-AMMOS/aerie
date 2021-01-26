package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.linear.LinearAccumulationEffect;
import gov.nasa.jpl.aerie.contrib.cells.linear.LinearIntegrationCell;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;

public final class Accumulator extends Model {
  public final Volume volume;
  public final Rate rate;

  public Accumulator(final Registrar registrar) {
    this(registrar, 0.0, 0.0);
  }

  public Accumulator(
      final Registrar registrar,
      final double initialVolume,
      final double initialRate)
  {
    super(registrar);

    final var ref = registrar.cell(new LinearIntegrationCell(initialVolume, initialRate));

    this.volume = new Volume(ref);
    this.rate = new Rate(ref);

    registrar.resource("volume", this.volume.resource);
    registrar.resource("rate", this.rate.resource);
  }

  public static final class Volume {
    public final RealResource resource;

    private Volume(final CellRef<LinearAccumulationEffect, LinearIntegrationCell> ref) {
      this.resource = RealResource.atom(ref, LinearIntegrationCell::getVolume);
    }

    public double get() {
      return this.resource.ask();
    }

    public Condition<?> isBetween(final double lower, final double upper) {
      return this.resource.isBetween(lower, upper);
    }
  }

  public static final class Rate {
    private final CellRef<LinearAccumulationEffect, LinearIntegrationCell> ref;

    public final RealResource resource;

    private Rate(final CellRef<LinearAccumulationEffect, LinearIntegrationCell> ref) {
      this.ref = ref;
      this.resource = RealResource.atom(ref, LinearIntegrationCell::getRate);
    }

    public double get() {
      return this.resource.ask();
    }

    public void add(final double delta) {
      this.ref.emit(LinearAccumulationEffect.addRate(delta));
    }

    public Condition<?> isBetween(final double lower, final double upper) {
      return this.resource.isBetween(lower, upper);
    }
  }
}
