package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/*package-local*/ final class UpdateActivityDirectiveValidationsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    update activity_directive
    set validations = ?::json
    where id = ?
  """;

  private final PreparedStatement statement;

  public UpdateActivityDirectiveValidationsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long directiveId, final List<ValidationNotice> notices)
  throws SQLException, FailedUpdateException
  {
    PreparedStatements.setValidationNotices(this.statement, 1, notices);
    this.statement.setLong(2, directiveId);

    statement.executeUpdate();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
