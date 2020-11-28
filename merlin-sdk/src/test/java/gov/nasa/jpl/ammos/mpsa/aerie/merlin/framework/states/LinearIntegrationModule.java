package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.LinearIntegrationModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;

public final class LinearIntegrationModule<$Schema> extends Module<$Schema> {
  private final Query<$Schema, Double, LinearIntegrationModel> query;
  public final RealResource<History<? extends $Schema>> volume;
  public final RealResource<History<? extends $Schema>> rate;

  public LinearIntegrationModule(final ResourcesBuilder.Cursor<$Schema> builder) {
    super(builder);

    this.query = builder.model(new LinearIntegrationModel(0.0, 0.0), ev -> ev);
    this.volume = builder
        .real("volume", now -> now.ask(this.query).getVolume())
        ::getDynamics;

    this.rate = builder
        .real("rate", now -> now.ask(this.query).getRate())
        ::getDynamics;
  }

  public void addRate(final double delta) {
    emit(delta, this.query);
  }

  public double getVolume() {
    return ask(this.volume);
  }

  public double getRate() {
    return ask(this.rate);
  }
}
