package gov.nasa.jpl.aerie.merlin.protocol;

/**
 * @param <$Schema>
 * @param <Event>
 * @param <State>
 */
@Capability
public interface Query<$Schema, Event, State> {
  default <$Timeline extends $Schema> Query<$Timeline, Event, State> specialize() {
    // SAFETY: A query against a schema is valid against any timeline implementing that schema.
    @SuppressWarnings("unchecked")
    final var query = (Query<$Timeline, Event, State>) this;

    return query;
  }
}
