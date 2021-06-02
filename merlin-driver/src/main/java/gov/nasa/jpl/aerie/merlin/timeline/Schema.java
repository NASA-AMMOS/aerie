package gov.nasa.jpl.aerie.merlin.timeline;

import gov.nasa.jpl.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;

import java.util.ArrayList;
import java.util.List;

public final class Schema<$Schema> {
  /*package-local*/ final List<Query<? super $Schema, ?, ?>> queries;

  private Schema(final List<Query<? super $Schema, ?, ?>> queries) {
    this.queries = queries;
  }

  public static Builder<?> builder() {
    return new Builder<>();
  }

  public Builder<? extends $Schema> extend() {
    return new Builder<>(this);
  }

  public static final class Builder<$Schema> {
    private BuilderState<$Schema> state = new UnbuiltState();
    private final List<Query<? super $Schema, ?, ?>> queries;

    private Builder(final List<Query<? super $Schema, ?, ?>> queries) {
      this.queries = queries;
    }

    private Builder() {
      this(new ArrayList<>());
    }

    private Builder(final Schema<? super $Schema> schema) {
      this(new ArrayList<>(schema.queries));
    }

    public <Event, Effect, CellType>
    Query<$Schema, Event, CellType>
    register(
        final Projection<Event, Effect> projection,
        final Applicator<Effect, CellType> applicator)
    {
      return this.state.register(this, projection, applicator);
    }

    public Schema<$Schema> build() {
      return this.state.build(this);
    }


    private interface BuilderState<$Schema> {
      <Event, Effect, CellType>
      Query<$Schema, Event, CellType>
      register(
          Builder<$Schema> builder,
          Projection<Event, Effect> projection,
          Applicator<Effect, CellType> applicator);

      Schema<$Schema>
      build(Builder<$Schema> builder);
    }

    private final class UnbuiltState implements BuilderState<$Schema> {
      @Override
      public <Event, Effect, CellType> Query<$Schema, Event, CellType> register(
          final Builder<$Schema> builder,
          final Projection<Event, Effect> projection,
          final Applicator<Effect, CellType> applicator)
      {
        final var index = builder.queries.size();
        final var query = new Query<$Schema, Event, CellType>(projection, applicator, index);
        builder.queries.add(query);

        return query;
      }

      @Override
      public Schema<$Schema> build(final Builder<$Schema> builder) {
        final var schema = new Schema<>(builder.queries);
        builder.state = new BuiltState(schema);
        return schema;
      }
    }

    private final class BuiltState implements BuilderState<$Schema> {
      private final Schema<$Schema> schema;

      public BuiltState(final Schema<$Schema> schema) {
        this.schema = schema;
      }

      @Override
      public <Event, Effect, CellType> Query<$Schema, Event, CellType> register(
          final Builder<$Schema> builder,
          final Projection<Event, Effect> projection,
          final Applicator<Effect, CellType> applicator)
      {
        throw new IllegalStateException(
            "A schema has already been built from this builder."
            + " Call Schema#extend() to derive a new schema from the built one.");
      }

      @Override
      public Schema<$Schema> build(final Builder<$Schema> builder) {
        return this.schema;
      }
    }
  }
}
