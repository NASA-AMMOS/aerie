package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.intellij.lang.annotations.Language;

/*package local*/ public class ClaimRequestAction implements AutoCloseable {
  private static final @Language("SQL") String sql =
      """
    update scheduling_request
      set
        status = 'incomplete'
      where (specification_id = ? and status = 'pending');
  """;

  private final PreparedStatement statement;

  public ClaimRequestAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long specificationId) throws SQLException, UnclaimableRequestException {
    this.statement.setLong(1, specificationId);

    final var count = this.statement.executeUpdate();
    if (count < 1) {
      throw new UnclaimableRequestException(specificationId);
    } else if (count > 1) {
      throw new SQLException(
          String.format(
              "Claiming a scheduling request for specification id %s returned more than one result"
                  + " row.",
              specificationId));
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
