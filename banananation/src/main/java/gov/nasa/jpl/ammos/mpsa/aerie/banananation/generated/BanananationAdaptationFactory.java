package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BuiltAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

// TODO: Automatically generate at compile time.
public final class BanananationAdaptationFactory implements AdaptationFactory {
  @Override
  public Adaptation<?> instantiate() {
    return this.instantiate(Schema.builder());
  }

  public <$Schema> Adaptation<$Schema> instantiate(final Schema.Builder<$Schema> schemaBuilder) {
    final var rootContext = DynamicCell.<Context<$Schema>>create();

    final var builder = new ResourcesBuilder<>(rootContext, schemaBuilder);
    final var module = new BanananationResources<>(builder.getCursor());
    final var activityTypes = ActivityTypes.get(rootContext, module);
    final var resources = builder.build();

    return new BuiltAdaptation<>(rootContext, resources, activityTypes);
  }
}
