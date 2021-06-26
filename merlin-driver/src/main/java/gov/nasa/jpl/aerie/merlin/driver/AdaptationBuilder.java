package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AdaptationBuilder<$Schema> implements AdaptationFactory.Builder<$Schema> {
  private AdaptationBuilderState<$Schema> state;

  public AdaptationBuilder(final Schema.Builder<$Schema> schemaBuilder) {
    this.state = new UnbuiltState(schemaBuilder);
  }

  @Override
  public boolean isBuilt() {
    return this.state.isBuilt();
  }

  @Override
  public <CellType> CellType getInitialState(
      final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, ?, CellType> query)
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
  public String daemon(final AdaptationFactory.TaskFactory<$Schema> task) {
    return this.state.daemon(task);
  }

  @Override
  public <Activity> void taskSpecType(final String name, final TaskSpecType<$Schema, Activity> type) {
    this.state.taskSpecType(name, type);
  }

  public Adaptation<$Schema> build() {
    return this.state.build();
  }


  private interface AdaptationBuilderState<$Schema> extends AdaptationFactory.Builder<$Schema> {
    // Provide a more specific return type.
    @Override
    <Event, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, Event, CellType>
    allocate(
        Projection<Event, Effect> projection,
        Applicator<Effect, CellType> applicator);

    Adaptation<$Schema> build();
  }

  private final class UnbuiltState implements AdaptationBuilderState<$Schema> {
    private final Map<gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, ?, ?>, Query<$Schema, ?, ?>> queries = new HashMap<>();
    private final Schema.Builder<$Schema> schemaBuilder;

    private final List<ResourceFamily<$Schema, ?>> resourceFamilies = new ArrayList<>();
    private final List<AdaptationFactory.TaskFactory<$Schema>> daemons = new ArrayList<>();
    private final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes = new HashMap<>();

    public UnbuiltState(final Schema.Builder<$Schema> schemaBuilder) {
      this.schemaBuilder = Objects.requireNonNull(schemaBuilder);
    }

    @Override
    public boolean isBuilt() {
      return false;
    }

    @Override
    public <CellType> CellType getInitialState(
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, ?, CellType> token)
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

      final var token = new gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, Event, CellType>() {};

      this.queries.put(token, query);

      return token;
    }

    @Override
    public <Dynamics> void resourceFamily(final ResourceFamily<$Schema, Dynamics> resourceFamily) {
      this.resourceFamilies.add(resourceFamily);
    }

    @Override
    public String daemon(final AdaptationFactory.TaskFactory<$Schema> task) {
      this.daemons.add(task);
      return null;  // TODO: get some way to refer to the daemon task
    }

    @Override
    public <Activity> void taskSpecType(final String id, final TaskSpecType<$Schema, Activity> taskSpecType) {
      this.taskSpecTypes.put(id, taskSpecType);
    }

    @Override
    public Adaptation<$Schema> build() {
      final var adaptation = new Adaptation<>(
          this.queries,
          this.schemaBuilder.build(),
          this.resourceFamilies,
          this.daemons,
          this.taskSpecTypes);

      AdaptationBuilder.this.state = new BuiltState(adaptation);

      return adaptation;
    }
  }

  private final class BuiltState implements AdaptationBuilderState<$Schema> {
    private final Adaptation<$Schema> adaptation;

    public BuiltState(final Adaptation<$Schema> adaptation) {
      this.adaptation = adaptation;
    }

    @Override
    public boolean isBuilt() {
      return true;
    }

    @Override
    public <CellType> CellType getInitialState(
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, ?, CellType> query)
    {
      return this.adaptation
          .getQuery(query)
          .orElseThrow(() -> new IllegalArgumentException("Unrecognized query"))
          .getInitialValue();
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
    public String daemon(final AdaptationFactory.TaskFactory<$Schema> task) {
      throw new IllegalStateException("Daemons cannot be added after the schema is built");
    }

    @Override
    public <Activity> void taskSpecType(final String id, final TaskSpecType<$Schema, Activity> taskSpecType) {
      throw new IllegalStateException("Activity types cannot be added after the schema is built");
    }

    @Override
    public Adaptation<$Schema> build() {
      return this.adaptation;
    }
  }
}
