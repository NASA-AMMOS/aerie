package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

/*package-local*/ final class GetActivityAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    with
      json_activity_arguments as
        ( select
            arg.activity_id,
            json_object_agg(arg.name, arg.value) as arguments
          from activity_argument as arg
          group by activity_id )
    select
      a.plan_id,
      to_char(p.start_time + a.start_offset, 'YYYY-DDD"T"HH24:MI:SS.FF6') as start_time,
      a.type,
      coalesce(arguments.arguments, '[]'::json) as arguments
    from plan as p
    left join activity as a
      on p.id = a.plan_id
    left join json_activity_arguments as arguments
      on a.id = arguments.activity_id
    where p.id = ?
      and a.id = ?
    """;

  private final PreparedStatement statement;

  public GetActivityAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public ActivityInstance get(final long planId, final ActivityInstanceId activityId)
  throws SQLException, NoSuchPlanException, NoSuchActivityInstanceException
  {
    this.statement.setLong(1, planId);
    this.statement.setLong(2, activityId.id());

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) throw new NoSuchPlanException(Long.toString(planId));
      requireColumnNonNull(results, 1, () -> new NoSuchActivityInstanceException(
          Long.toString(planId),
          activityId));

      final var startTimestamp = Timestamp.fromString(results.getString(2));
      final var type = results.getString(3);
      final var activityArguments = PostgresPlanRepository.parseActivityArgumentsJson(results.getString(4));

      return new ActivityInstance(type, startTimestamp, activityArguments);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }

  private static <T extends Throwable>
  void requireColumnNonNull(final ResultSet results, final int index, final Supplier<T> makeException)
  throws T, SQLException {
    if (isColumnNull(results, index)) throw makeException.get();
  }

  private static boolean isColumnNull(final ResultSet results, final int index) throws SQLException {
    // You're kidding, right? This is how you detect NULL with JDBC?
    results.getObject(index);
    return results.wasNull();
  }
}
