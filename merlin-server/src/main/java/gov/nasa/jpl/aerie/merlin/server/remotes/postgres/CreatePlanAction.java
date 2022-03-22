package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setTimestamp;

/*package-local*/ final class CreatePlanAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into plan (name, model_id, duration, start_time)
    values (?, ?, ?::timestamptz - ?::timestamptz, ?)
    returning id
    """;

  private final PreparedStatement statement;

  public CreatePlanAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long apply(
      final String name,
      final long modelId,
      final Timestamp startTime,
      final Timestamp endTime
  ) throws SQLException, MissionModelRepository.NoSuchMissionModelException, FailedInsertException {
    this.statement.setString(1, name);
    this.statement.setLong(2, modelId);
    setTimestamp(this.statement, 3, endTime);
    setTimestamp(this.statement, 4, startTime);
    setTimestamp(this.statement, 5, startTime);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) throw new FailedInsertException("plan");

      return results.getLong(1);
    } catch (final SQLException ex) {
      // https://www.postgresql.org/docs/current/errcodes-appendix.html
      if (Objects.equals(ex.getSQLState(), "23503")) {  /* foreign_key_violation */
        // The only foreign key on the `plan` table references `mission_model`,
        // so there must not be a mission model with the given ID.
        throw new MissionModelRepository.NoSuchMissionModelException();
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
