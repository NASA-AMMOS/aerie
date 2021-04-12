package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.merlin.framework.AdaptationBuilder;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;

public interface AdaptationFactory {
  Adaptation<?> instantiate(final SerializedValue configuration);
  <$Schema> AdaptationBuilder<$Schema> makeBuilder(final Schema.Builder<$Schema> schemaBuilder, final SerializedValue configuration);
}
