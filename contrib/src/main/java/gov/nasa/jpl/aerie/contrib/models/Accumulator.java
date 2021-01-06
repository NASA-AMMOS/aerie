package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.linear.LinearIntegrationCell;
import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.timeline.Query;

public final class Accumulator<$Schema> extends Model<$Schema> {
  private final Query<$Schema, Double, LinearIntegrationCell> query;

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
    this.query = registrar.cell(new LinearIntegrationCell(initialVolume, initialRate), ev -> ev);
    this.volume = new Volume(registrar.real("volume", now -> now.ask(this.query).getVolume()));
    this.rate = new Rate(registrar.real("rate", now -> now.ask(this.query).getRate()));
  }

  public final class Volume {
    public final RealResource<$Schema> resource;

    private Volume(final RealResource<$Schema> resource) {
      this.resource = resource;
    }

    public double get() {
      return this.resource.ask(now());
    }

    public Condition<$Schema> isBetween(final double lower, final double upper) {
      return this.resource.isBetween(lower, upper);
    }
  }

  public final class Rate {
    public final RealResource<$Schema> resource;

    private Rate(final RealResource<$Schema> resource) {
      this.resource = resource;
    }

    public double get() {
      return this.resource.ask(now());
    }

    public void add(final double delta) {
      emit(delta, query);
    }

    public Condition<$Schema> isBetween(final double lower, final double upper) {
      return this.resource.isBetween(lower, upper);
    }
  }
}
