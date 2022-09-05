package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeRegistrar;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record DirectiveTypeRegistry<Registry, Model>(
    Map<String, TaskSpecType<Model, ?, ?>> taskSpecTypes,
    Registry registry)
{
  public static <Registry, Model>
  DirectiveTypeRegistry<Registry, Model> extract(final MissionModelFactory<Registry, ?, Model> factory) {
    return extract(factory::buildRegistry);
  }

  public static <Registry, Model>
  DirectiveTypeRegistry<Registry, Model> extract(final Function<DirectiveTypeRegistrar<Model>, Registry> scope) {
    final var builder = new Builder<Model>();
    final var registry = scope.apply(builder);
    return new DirectiveTypeRegistry<>(builder.taskSpecTypes, registry);
  }

  private static final class Builder<Model> implements DirectiveTypeRegistrar<Model> {
    private final Map<String, TaskSpecType<Model, ?, ?>> taskSpecTypes = new HashMap<>();

    @Override
    public <Input, Output> void registerDirectiveType(
        final String name,
        final TaskSpecType<Model, Input, Output> taskSpecType)
    {
      // TODO: Throw an exception if this name already has already been registered.
      this.taskSpecTypes.put(name, taskSpecType);
    }
  }
}
