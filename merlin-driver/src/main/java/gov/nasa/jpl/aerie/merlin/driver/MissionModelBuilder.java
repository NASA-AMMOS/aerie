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
import gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MissionModelBuilder implements Initializer {
  private MissionModelBuilderState state = new UnbuiltState();

  @Override
  public <CellType> CellType getInitialState(
      final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<?, ? extends CellType> query)
  {
    return this.state.getInitialState(query);
  }

  @Override
  public <EventType, Effect, CellType>
  gov.nasa.jpl.aerie.merlin.protocol.driver.Query<EventType, CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final EffectTrait<Effect> trait,
      final Function<EventType, Effect> projection
  ) {
    return this.state.allocate(initialState, applicator, trait, projection);
  }

  @Override
  public void resource(final String name, final Resource<?> resource) {
    this.state.resource(name, resource);
  }

  @Override
  public <Event> void topic(
      final String name,
      final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<Event, ?> query,
      final ValueSchema schema,
      final Function<Event, SerializedValue> serializer)
  {
    this.state.topic(name, query, schema, serializer);
  }

  @Override
  public <Return> String daemon(final TaskFactory<Return> task) {
    return this.state.daemon(task);
  }

  public <Model>
  MissionModel<Model> build(
      final Model model,
      final ConfigurationType<?> configurationType,
      final Map<String, TaskSpecType<Model, ?, ?>> taskSpecTypes)
  {
    return this.state.build(model, configurationType, taskSpecTypes);
  }

  private interface MissionModelBuilderState extends Initializer {
    <Model> MissionModel<Model>
    build(
        Model model,
        ConfigurationType<?> configurationType,
        Map<String, TaskSpecType<Model, ?, ?>> taskSpecTypes);
  }

  private final class UnbuiltState implements MissionModelBuilderState {
    private final LiveCells initialCells = new LiveCells(new CausalEventSource());

    private final Map<String, Resource<?>> resources = new HashMap<>();
    private final List<TaskFactory<?>> daemons = new ArrayList<>();
    private final List<MissionModel.SerializableTopic<?>> topics = new ArrayList<>();

    @Override
    public <CellType> CellType getInitialState(
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<?, ? extends CellType> token)
    {
      // SAFETY: The only `Query` objects the model should have were returned by `UnbuiltState#allocate`.
      @SuppressWarnings("unchecked")
      final var query = (EngineQuery<?, CellType>) token;

      final var state$ = this.initialCells.getState(query.query());

      return state$.orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public <EventType, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<EventType, CellType> allocate(
        final CellType initialState,
        final Applicator<Effect, CellType> applicator,
        final EffectTrait<Effect> trait,
        final Function<EventType, Effect> projection
    ) {
      final var topic = new Topic<EventType>();
      final var selector = new Selector<>(topic, projection);

      // TODO: The evaluator should probably be specified later, after the model is built.
      //   To achieve this, we'll need to defer the construction of the initial `LiveCells` until later,
      //   instead simply storing the cell specification provided to us (and its associated `Query` token).
      final var evaluator = new RecursiveEventGraphEvaluator();

      final var query = new Query<CellType>();
      this.initialCells.put(query, new Cell<>(applicator, trait, selector, evaluator, initialState));

      return new EngineQuery<>(topic, query);
    }

    @Override
    public void resource(final String name, final Resource<?> resource) {
      this.resources.put(name, resource);
    }

    @Override
    public <Event> void topic(
        final String name,
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<Event, ?> query,
        final ValueSchema schema,
        final Function<Event, SerializedValue> serializer)
    {
      this.topics.add(new MissionModel.SerializableTopic<>(name, query, schema, serializer));
    }

    @Override
    public <Return> String daemon(final TaskFactory<Return> task) {
      this.daemons.add(task);
      return null;  // TODO: get some way to refer to the daemon task
    }

    @Override
    public <Model>
    MissionModel<Model> build(
        final Model model,
        final ConfigurationType<?> configurationType,
        final Map<String, TaskSpecType<Model, ?, ?>> taskSpecTypes)
    {
      final var missionModel = new MissionModel<>(
          model,
          this.initialCells,
          this.resources,
          this.topics,
          this.daemons,
          configurationType,
          taskSpecTypes);

      MissionModelBuilder.this.state = new BuiltState();

      return missionModel;
    }
  }

  private static final class BuiltState implements MissionModelBuilderState {
    @Override
    public <CellType> CellType getInitialState(
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<?, ? extends CellType> query)
    {
      throw new IllegalStateException("Cannot interact with the builder after it is built");
    }

    @Override
    public <EventType, Effect, CellType>
    gov.nasa.jpl.aerie.merlin.protocol.driver.Query<EventType, CellType> allocate(
        final CellType initialState,
        final Applicator<Effect, CellType> applicator,
        final EffectTrait<Effect> trait,
        final Function<EventType, Effect> projection
    ) {
      throw new IllegalStateException("Cells cannot be allocated after the schema is built");
    }

    @Override
    public void resource(final String name, final Resource<?> resource) {
      throw new IllegalStateException("Resources cannot be added after the schema is built");
    }

    @Override
    public <Event> void topic(
        final String name,
        final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<Event, ?> query,
        final ValueSchema schema,
        final Function<Event, SerializedValue> serializer)
    {
      throw new IllegalStateException("Topics cannot be added after the schema is built");
    }

    @Override
    public <Return> String daemon(final TaskFactory<Return> task) {
      throw new IllegalStateException("Daemons cannot be added after the schema is built");
    }

    @Override
    public <Model>
    MissionModel<Model> build(
        final Model model,
        final ConfigurationType<?> configurationType,
        final Map<String, TaskSpecType<Model, ?, ?>> taskSpecTypes)
    {
      throw new IllegalStateException("Cannot build a builder multiple times");
    }
  }
}
