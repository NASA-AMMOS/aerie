package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AdaptationBuilder<$Schema> implements AdaptationFactory.Builder<$Schema> {
  private AdaptationBuilderState<$Schema> state;

  public AdaptationBuilder(final Schema.Builder<$Schema> schemaBuilder) {
    this.state = new UnbuiltState(schemaBuilder);
  }

  @Override
  public boolean isBuilt() {
    return state.isBuilt();
  }

  @Override
  public <Event, Effect, CellType>
  Query<$Schema, Event, CellType>
  allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator) {
    return this.state.allocate(projection, applicator);
  }

  @Override
  public <Dynamics> void resourceFamily(final ResourceFamily<$Schema, Dynamics> resourceFamily) {
    this.state.resourceFamily(resourceFamily);
  }

  public String daemon(final AdaptationFactory.TaskFactory<$Schema> task) {
    return this.state.daemon(task);
  }

  @Override
  public <Activity> void taskSpecType(final String name, final TaskSpecType<$Schema, Activity> type) {
    this.state.taskSpecType(name, type);
  }

  public BuiltAdaptation<$Schema> build() {
    return this.state.build();
  }


  private interface AdaptationBuilderState<$Schema> extends AdaptationFactory.Builder<$Schema> {
    BuiltAdaptation<$Schema> build();
  }

  private final class UnbuiltState implements AdaptationBuilderState<$Schema> {
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
    public <Event, Effect, CellType> Query<$Schema, Event, CellType> allocate(
        final Projection<Event, Effect> projection,
        final Applicator<Effect, CellType> applicator)
    {
      return this.schemaBuilder.register(projection, applicator);
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
    public BuiltAdaptation<$Schema> build() {
      final var adaptation = new BuiltAdaptation<>(
          this.schemaBuilder.build(),
          this.resourceFamilies,
          this.daemons,
          this.taskSpecTypes);

      AdaptationBuilder.this.state = new BuiltState(adaptation);

      return adaptation;
    }
  }

  private final class BuiltState implements AdaptationBuilderState<$Schema> {
    private final BuiltAdaptation<$Schema> adaptation;

    public BuiltState(final BuiltAdaptation<$Schema> adaptation) {
      this.adaptation = adaptation;
    }

    @Override
    public boolean isBuilt() {
      return true;
    }

    @Override
    public <Event, Effect, CellType> Query<$Schema, Event, CellType> allocate(
        final Projection<Event, Effect> projection,
        final Applicator<Effect, CellType> applicator)
    {
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
    public BuiltAdaptation<$Schema> build() {
      return this.adaptation;
    }
  }
}
