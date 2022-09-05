package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeRegistrar;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;

import java.util.HashMap;
import java.util.Map;

public record DirectiveTypeRegistry<Model>(Map<String, DirectiveType<Model, ?, ?>> taskSpecTypes) {
  public static <Model>
  DirectiveTypeRegistry<Model> extract(final MissionModelFactory<?, Model> factory) {
    final var builder = new Builder<Model>();
    factory.buildRegistry(builder);
    return new DirectiveTypeRegistry<>(builder.taskSpecTypes);
  }

  private static final class Builder<Model> implements DirectiveTypeRegistrar<Model> {
    private final Map<String, DirectiveType<Model, ?, ?>> taskSpecTypes = new HashMap<>();

    @Override
    public <Input, Output> void registerDirectiveType(
        final String name,
        final DirectiveType<Model, Input, Output> directiveType)
    {
      // TODO: Throw an exception if this name already has already been registered.
      this.taskSpecTypes.put(name, directiveType);
    }
  }
}
