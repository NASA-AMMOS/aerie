package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Adaptation<$Schema> {
  private final Map<gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, ?, ?>, Query<$Schema, ?, ?>> queries;

  private final Schema<$Schema> schema;
  private final List<ResourceFamily<$Schema, ?>> resourceFamilies;
  private final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes;
  private final List<Initializer.TaskFactory<$Schema>> daemons;

  public Adaptation(
      final Map<gov.nasa.jpl.aerie.merlin.protocol.driver.Query<$Schema, ?, ?>, Query<$Schema, ?, ?>> queries,
      final Schema<$Schema> schema,
      final List<ResourceFamily<$Schema, ?>> resourceFamilies,
      final List<Initializer.TaskFactory<$Schema>> daemons,
      final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes)
  {
    this.queries = Objects.requireNonNull(queries);
    this.schema = Objects.requireNonNull(schema);
    this.resourceFamilies = Collections.unmodifiableList(resourceFamilies);
    this.taskSpecTypes = Collections.unmodifiableMap(taskSpecTypes);
    this.daemons = Collections.unmodifiableList(daemons);
  }

  public <Event, State>
  Optional<Query<$Schema, Event, State>>
  getQuery(final gov.nasa.jpl.aerie.merlin.protocol.driver.Query<? extends $Schema, Event, State> token) {
    // SAFETY: For every entry in the queries map, the type parameters line up.
    @SuppressWarnings("unchecked")
    final var query = (Query<$Schema, Event, State>) this.queries.get(token);

    return Optional.ofNullable(query);
  }

  public Map<String, TaskSpecType<$Schema, ?>> getTaskSpecificationTypes() {
    return this.taskSpecTypes;
  }

  public <$Timeline extends $Schema> Task<$Timeline> getDaemon() {
    return new Task<>() {
      @Override
      public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
        Adaptation.this.daemons.forEach(daemon -> scheduler.spawn(daemon.create()));
        return TaskStatus.completed();
      }

      @Override
      public void reset() {
      }
    };
  }

  public Iterable<ResourceFamily<$Schema, ?>> getResourceFamilies() {
    return this.resourceFamilies;
  }

  public Schema<$Schema> getSchema() {
    return this.schema;
  }
}
