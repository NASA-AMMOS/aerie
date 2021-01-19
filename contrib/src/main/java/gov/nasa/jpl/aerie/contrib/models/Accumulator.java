package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.linear.LinearIntegrationCell;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;

public final class Accumulator<$Schema> extends Model<$Schema> {
  public final Volume volume;
  public final Rate rate;

  public Accumulator(final Registrar<$Schema> registrar) {
    this(registrar, 0.0, 0.0);
  }

  public Accumulator(
      final Registrar<$Schema> registrar,
      final double initialVolume,
      final double initialRate)
  {
    super(registrar);

    final var ref = registrar.cell(new LinearIntegrationCell(initialVolume, initialRate));

    this.volume = new Volume(ref);
    this.rate = new Rate(ref);
  }

  public final class Volume {
    public final RealResource resource;

    private Volume(final CellRef<Double, LinearIntegrationCell> ref) {
      this.resource = RealResource.atom(ref, LinearIntegrationCell::getVolume);
    }

    public double get() {
      return this.resource.ask();
    }

    public Condition<?> isBetween(final double lower, final double upper) {
      return this.resource.isBetween(lower, upper);
    }
  }

  public final class Rate {
    private final CellRef<Double, LinearIntegrationCell> ref;

    public final RealResource resource;

    private Rate(final CellRef<Double, LinearIntegrationCell> ref) {
      this.ref = ref;
      this.resource = RealResource.atom(ref, LinearIntegrationCell::getRate);
    }

    public double get() {
      return this.resource.ask();
    }

    public void add(final double delta) {
      this.ref.emit(delta);
    }

    public Condition<?> isBetween(final double lower, final double upper) {
      return this.resource.isBetween(lower, upper);
    }
  }
}
