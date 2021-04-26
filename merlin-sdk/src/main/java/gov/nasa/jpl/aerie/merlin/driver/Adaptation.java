package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Adaptation<$Schema> {
  private final Map<gov.nasa.jpl.aerie.merlin.protocol.Query<$Schema, ?, ?>, Query<$Schema, ?, ?>> queries;

  private final Schema<$Schema> schema;
  private final List<ResourceFamily<$Schema, ?>> resourceFamilies;
  private final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes;
  private final List<AdaptationFactory.TaskFactory<$Schema>> daemons;

  public Adaptation(
      final Map<gov.nasa.jpl.aerie.merlin.protocol.Query<$Schema, ?, ?>, Query<$Schema, ?, ?>> queries,
      final Schema<$Schema> schema,
      final List<ResourceFamily<$Schema, ?>> resourceFamilies,
      final List<AdaptationFactory.TaskFactory<$Schema>> daemons,
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
  getQuery(final gov.nasa.jpl.aerie.merlin.protocol.Query<? extends $Schema, Event, State> token) {
    // SAFETY: For every entry in the queries map, the type parameters line up.
    @SuppressWarnings("unchecked")
    final var query = (Query<$Schema, Event, State>) this.queries.get(token);

    return Optional.ofNullable(query);
  }

  public Map<String, TaskSpecType<$Schema, ?>> getTaskSpecificationTypes() {
    return this.taskSpecTypes;
  }

  public <$Timeline extends $Schema> Task<$Timeline> getDaemon() {
    return scheduler -> {
      this.daemons.forEach(daemon -> scheduler.spawn(daemon.create()));
      return TaskStatus.completed();
    };
  }

  public Iterable<ResourceFamily<$Schema, ?>> getResourceFamilies() {
    return this.resourceFamilies;
  }

  public Schema<$Schema> getSchema() {
    return this.schema;
  }
}
