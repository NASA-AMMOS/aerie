package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.LinearIntegrationModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;

public final class LinearIntegrationModule<$Schema> extends Module<$Schema> {
  private final Query<$Schema, Double, LinearIntegrationModel> query;

  public final Volume volume;
  public final Rate rate;

  public LinearIntegrationModule(final ResourcesBuilder.Cursor<$Schema> builder) {
    super(builder);

    this.query = builder.model(new LinearIntegrationModel(0.0, 0.0), ev -> ev);
    this.volume = new Volume(builder.real("volume", now -> now.ask(this.query).getVolume()));
    this.rate = new Rate(builder.real("rate", now -> now.ask(this.query).getRate()));
  }

  public final class Volume {
    public final RealResource<History<? extends $Schema>> resource;

    private Volume(final RealResource<History<? extends $Schema>> resource) {
      this.resource = resource;
    }

    public double get() {
      return this.resource.getDynamics(now()).getDynamics().initial;
    }
  }

  public final class Rate {
    public final RealResource<History<? extends $Schema>> resource;

    private Rate(final RealResource<History<? extends $Schema>> resource) {
      this.resource = resource;
    }

    public double get() {
      return this.resource.getDynamics(now()).getDynamics().initial;
    }

    public void add(final double delta) {
      emit(delta, query);
    }
  }
}
