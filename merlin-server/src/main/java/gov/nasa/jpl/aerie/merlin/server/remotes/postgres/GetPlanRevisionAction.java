package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class GetPlanRevisionAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select revision
    from plan
    where id = ?
    """;

  private final PreparedStatement statement;

  public GetPlanRevisionAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long get(final PlanId planId) throws SQLException, NoSuchPlanException {
    this.statement.setLong(1, planId.id());

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) throw new NoSuchPlanException(planId);

      return results.getLong(1);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
