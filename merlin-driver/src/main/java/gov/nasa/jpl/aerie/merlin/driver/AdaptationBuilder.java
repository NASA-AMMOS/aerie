package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.EngineQuery;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AdaptationBuilder<$Schema> implements Initializer<$Schema> {
  private AdaptationBuilderState<$Schema> state;

  public AdaptationBuilder(final Schema.Builder<$Schema> schemaBuilder) {
    this.state = new UnbuiltState(schemaBuilder);
  }

  @Override
  public <CellType> CellType getInitialState(
      final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<? super $Schema, ?, ? extends CellType> query)
  {
    return this.state.getInitialState(query);
  }

  @Override
  public <Event, Effect, CellType>
  gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, Event, CellType>
  allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator) {
    return this.state.allocate(projection, applicator);
  }

  @Override
  public <Dynamics> void resourceFamily(final ResourceFamily<$Schema, Dynamics> resourceFamily) {
    this.state.resourceFamily(resourceFamily);
  }

  @Override
  public String daemon(final TaskFactory<$Schema> task) {
    return this.state.daemon(task);
  }

  public <Model> Adaptation<$Schema, Model>
  build(final Phantom<$Schema, Model> model, final Map<String, TaskSpecType<Model, ?>> taskSpecTypes) {
    return this.state.build(model, taskSpecTypes);
  }


  private interface AdaptationBuilderState<$Schema> extends Initializer<$Schema> {
    // Provide a more specific return type.
    @Override
    <Event, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, Event, CellType>
    allocate(
        Projection<Event, Effect> projection,
        Applicator<Effect, CellType> applicator);

    <Model>
    Adaptation<$Schema, Model>
    build(
        Phantom<$Schema, Model> model,
        Map<String, TaskSpecType<Model, ?>> taskSpecTypes);
  }

  private final class UnbuiltState implements AdaptationBuilderState<$Schema> {
    private final Map<gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, ?, ?>, Query<$Schema, ?, ?>> queries = new HashMap<>();
    private final Schema.Builder<$Schema> schemaBuilder;

    private final List<ResourceFamily<$Schema, ?>> resourceFamilies = new ArrayList<>();
    private final List<TaskFactory<$Schema>> daemons = new ArrayList<>();

    public UnbuiltState(final Schema.Builder<$Schema> schemaBuilder) {
      this.schemaBuilder = Objects.requireNonNull(schemaBuilder);
    }

    @Override
    public <CellType> CellType getInitialState(
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<? super $Schema, ?, ? extends CellType> token)
    {
      // SAFETY: For every entry in the queries map, the type parameters line up.
      @SuppressWarnings("unchecked")
      final var query = (Query<$Schema, ?, CellType>) this.queries.get(token);

      return Optional
          .ofNullable(query)
          .orElseThrow(() -> new IllegalArgumentException("Unrecognized query"))
          .getInitialValue();
    }

    @Override
    public <Event, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, Event, CellType>
    allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator) {
      final var query = this.schemaBuilder.register(projection, applicator);

      final var token = new EngineQuery<>(query);

      this.queries.put(token, query);

      return token;
    }

    @Override
    public <Dynamics> void resourceFamily(final ResourceFamily<$Schema, Dynamics> resourceFamily) {
      this.resourceFamilies.add(resourceFamily);
    }

    @Override
    public String daemon(final TaskFactory<$Schema> task) {
      this.daemons.add(task);
      return null;  // TODO: get some way to refer to the daemon task
    }

    @Override
    public <Model> Adaptation<$Schema, Model>
    build(final Phantom<$Schema, Model> model, final Map<String, TaskSpecType<Model, ?>> taskSpecTypes) {
      final var adaptation = new Adaptation<>(
          model,
          this.queries,
          this.schemaBuilder.build(),
          this.resourceFamilies,
          this.daemons,
          taskSpecTypes);

      AdaptationBuilder.this.state = new BuiltState();

      return adaptation;
    }
  }

  private final class BuiltState implements AdaptationBuilderState<$Schema> {
    @Override
    public <CellType> CellType getInitialState(
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<? super $Schema, ?, ? extends CellType> query)
    {
      throw new IllegalStateException("Cannot interact with the builder after it is built");
    }

    @Override
    public <Event, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, Event, CellType>
    allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator) {
      throw new IllegalStateException("Cells cannot be allocated after the schema is built");
    }

    @Override
    public <Dynamics> void resourceFamily(final ResourceFamily<$Schema, Dynamics> resourceFamily) {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public String daemon(final TaskFactory<$Schema> task) {
      throw new IllegalStateException("Daemons cannot be added after the schema is built");
    }

    @Override
    public <Model> Adaptation<$Schema, Model>
    build(final Phantom<$Schema, Model> model, final Map<String, TaskSpecType<Model, ?>> taskSpecTypes) {
      throw new IllegalStateException("Cannot build a builder multiple times");
    }
  }
}
