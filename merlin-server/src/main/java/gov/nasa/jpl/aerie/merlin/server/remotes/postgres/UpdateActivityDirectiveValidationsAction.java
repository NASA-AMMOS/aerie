package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/*package-local*/ final class UpdateActivityDirectiveValidationsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into activity_directive_validations (directive_id, validations)
    values (?, ?::json)
    on conflict (directive_id) do update set validations = ?::json
  """;

  private final PreparedStatement statement;

  public UpdateActivityDirectiveValidationsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long directiveId, final List<ValidationNotice> notices)
  throws SQLException, FailedUpdateException
  {
    this.statement.setLong(1, directiveId);
    PreparedStatements.setValidationNotices(this.statement, List.of(2, 3), notices);

    statement.executeUpdate();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
