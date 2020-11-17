package gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.effects.Projection;

import java.util.ArrayList;
import java.util.List;

public final class Schema<$Schema, Event> {
  /*package-local*/ final List<Query<? super $Schema, Event, ?>> queries;

  private Schema(final List<Query<? super $Schema, Event, ?>> queries) {
    this.queries = queries;
  }

  public static <Event> Builder<?, Event> builder() {
    return new Builder<>();
  }

  public Builder<? extends $Schema, Event> extend() {
    return new Builder<>(this);
  }

  public static final class Builder<$Schema, Event> {
    private BuilderState<$Schema, Event> state = new UnbuiltState();
    private final List<Query<? super $Schema, Event, ?>> queries;

    private Builder(final List<Query<? super $Schema, Event, ?>> queries) {
      this.queries = queries;
    }

    private Builder() {
      this(new ArrayList<>());
    }

    private Builder(final Schema<? super $Schema, Event> schema) {
      this(new ArrayList<>(schema.queries));
    }

    public <Effect, ModelType>
    Query<$Schema, Event, ModelType>
    register(
        final Projection<Event, Effect> projection,
        final Applicator<Effect, ModelType> applicator)
    {
      return this.state.register(this, projection, applicator);
    }

    public Schema<$Schema, Event> build() {
      return this.state.build(this);
    }


    private interface BuilderState<$Schema, Event> {
      <Effect, ModelType>
      Query<$Schema, Event, ModelType>
      register(
          Builder<$Schema, Event> builder,
          Projection<Event, Effect> projection,
          Applicator<Effect, ModelType> applicator);

      Schema<$Schema, Event>
      build(Builder<$Schema, Event> builder);
    }

    private final class UnbuiltState implements BuilderState<$Schema, Event> {
      @Override
      public <Effect, ModelType> Query<$Schema, Event, ModelType> register(
          final Builder<$Schema, Event> builder,
          final Projection<Event, Effect> projection,
          final Applicator<Effect, ModelType> applicator)
      {
        final var index = builder.queries.size();
        final var query = new Query<$Schema, Event, ModelType>(projection, applicator, index);
        builder.queries.add(query);

        return query;
      }

      @Override
      public Schema<$Schema, Event> build(final Builder<$Schema, Event> builder) {
        final var schema = new Schema<>(builder.queries);
        builder.state = new BuiltState(schema);
        return schema;
      }
    }

    private final class BuiltState implements BuilderState<$Schema, Event> {
      private final Schema<$Schema, Event> schema;

      public BuiltState(final Schema<$Schema, Event> schema) {
        this.schema = schema;
      }

      @Override
      public <Effect, ModelType> Query<$Schema, Event, ModelType> register(
          final Builder<$Schema, Event> builder,
          final Projection<Event, Effect> projection,
          final Applicator<Effect, ModelType> applicator)
      {
        throw new IllegalStateException(
            "A schema has already been built from this builder."
            + " Call Schema#extend() to derive a new schema from the built one.");
      }

      @Override
      public Schema<$Schema, Event> build(final Builder<$Schema, Event> builder) {
        return this.schema;
      }
    }
  }
}
