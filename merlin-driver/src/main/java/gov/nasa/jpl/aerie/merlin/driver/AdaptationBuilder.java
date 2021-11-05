package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.EngineQuery;
import gov.nasa.jpl.aerie.merlin.driver.timeline.CausalEventSource;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Cell;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Query;
import gov.nasa.jpl.aerie.merlin.driver.timeline.RecursiveEventGraphEvaluator;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Selector;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Phantom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AdaptationBuilder<$Schema> implements Initializer<$Schema> {
  private AdaptationBuilderState<$Schema> state = new UnbuiltState();

  @Override
  public <CellType> CellType getInitialState(
      final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<? super $Schema, ?, ? extends CellType> query)
  {
    return this.state.getInitialState(query);
  }

  @Override
  public <EventType, Effect, CellType>
  gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, EventType, CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final Projection<EventType, Effect> projection
  ) {
    return this.state.allocate(initialState, applicator, projection);
  }

  @Override
  public void resource(final String name, final Resource<? super $Schema, ?> resource) {
    this.state.resource(name, resource);
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
    <EventType, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, EventType, CellType>
    allocate(
        CellType initialState,
        Applicator<Effect, CellType> applicator,
        Projection<EventType, Effect> projection);

    <Model>
    Adaptation<$Schema, Model>
    build(
        Phantom<$Schema, Model> model,
        Map<String, TaskSpecType<Model, ?>> taskSpecTypes);
  }

  private final class UnbuiltState implements AdaptationBuilderState<$Schema> {
    private final LiveCells initialCells = new LiveCells(new CausalEventSource());

    private final Map<String, Resource<? super $Schema, ?>> resources = new HashMap<>();
    private final List<TaskFactory<$Schema>> daemons = new ArrayList<>();

    @Override
    public <CellType> CellType getInitialState(
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<? super $Schema, ?, ? extends CellType> token)
    {
      // SAFETY: The only `Query` objects the model should have were returned by `UnbuiltState#allocate`.
      @SuppressWarnings("unchecked")
      final var query = (EngineQuery<$Schema, ?, CellType>) token;

      final var state$ = this.initialCells.getState(query.query());

      return state$.orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public <EventType, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, EventType, CellType> allocate(
        final CellType initialState,
        final Applicator<Effect, CellType> applicator,
        final Projection<EventType, Effect> projection
    ) {
      final var topic = new Topic<EventType>();
      final var selector = new Selector<>(topic, projection::atom);

      // TODO: The evaluator should probably be specified later, after the model is built.
      //   To achieve this, we'll need to defer the construction of the initial `LiveCells` until later,
      //   instead simply storing the cell specification provided to us (and its associated `Query` token).
      final var evaluator = new RecursiveEventGraphEvaluator();

      final var query = new Query<CellType>();
      this.initialCells.put(query, new Cell<>(applicator, projection, selector, evaluator, initialState));

      return new EngineQuery<>(topic, query);
    }

    @Override
    public void resource(final String name, final Resource<? super $Schema, ?> resource) {
      this.resources.put(name, resource);
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
          this.initialCells,
          this.resources,
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
    public <EventType, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, EventType, CellType> allocate(
        final CellType initialState,
        final Applicator<Effect, CellType> applicator,
        final Projection<EventType, Effect> projection
    ) {
      throw new IllegalStateException("Cells cannot be allocated after the schema is built");
    }

    @Override
    public void resource(final String name, final Resource<? super $Schema, ?> resource) {
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
