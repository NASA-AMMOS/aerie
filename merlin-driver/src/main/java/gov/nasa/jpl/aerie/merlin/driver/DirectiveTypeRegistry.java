package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;

import java.util.Map;

public record DirectiveTypeRegistry<Model>(Map<String, ? extends DirectiveType<Model, ?, ?>> taskSpecTypes) {
  public static <Model>
  DirectiveTypeRegistry<Model> extract(final MissionModelFactory<?, Model> factory) {
    return new DirectiveTypeRegistry<>(factory.getDirectiveTypes());
  }
}
