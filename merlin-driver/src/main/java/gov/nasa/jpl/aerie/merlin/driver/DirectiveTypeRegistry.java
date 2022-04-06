package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeRegistrar;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

  /**
   * @param id  The ID of the directive type to look up.
   *            This *must* be an ID originally produced by this instance, not by some other registry instance.
   */
  // PRECONDITION: `id` was constructed by this instance.
  public <Input, Output>
  TaskSpecType<Model, Input, Output> lookup(final DirectiveTypeId<Input, Output> id) {
    final TaskSpecType<Model, ?, ?> rawDirectiveType = this.taskSpecTypes.get(id.name);

    // SAFETY: By precondition, the given ID was constructed when this directive was registered.
    @SuppressWarnings("unchecked")
    final var directiveType = (TaskSpecType<Model, Input, Output>) rawDirectiveType;

    return directiveType;
  }

  private static final class Builder<Model> implements DirectiveTypeRegistrar<Model> {
    private final Map<String, TaskSpecType<Model, ?, ?>> taskSpecTypes = new HashMap<>();

    @Override
    public <Input, Output> DirectiveTypeId<Input, Output> registerDirectiveType(
        final String name,
        final TaskSpecType<Model, Input, Output> taskSpecType)
    {
      // TODO: Throw an exception if this name already has already been registered.
      this.taskSpecTypes.put(name, taskSpecType);
      return new DirectiveTypeId<>(name);
    }
  }

  public static final class DirectiveTypeId<Input, Output>
      implements gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeId<Input, Output>
  {
    public final String name;

    private DirectiveTypeId(final String name) {
      this.name = Objects.requireNonNull(name);
    }
  }
}
