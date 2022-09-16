package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValidationNotice;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/*package-local*/ final class UpdateActivityDirectiveValidationsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into activity_directive_validations
        as validation (directive_id, last_modified_at, validations)
    values (?, ?::timestamptz, ?::json)
    on conflict (directive_id) do update
        set
            last_modified_at = excluded.last_modified_at,
            validations = excluded.validations
        where validation.last_modified_at < excluded.last_modified_at
  """;

  private final PreparedStatement statement;

  public UpdateActivityDirectiveValidationsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long directiveId, final Timestamp argumentsModifiedTime, final List<ValidationNotice> notices)
  throws SQLException, FailedUpdateException
  {
    this.statement.setLong(1, directiveId);
    PreparedStatements.setTimestamp(this.statement, 2, argumentsModifiedTime);
    PreparedStatements.setValidationNotices(this.statement, 3, notices);

    statement.executeUpdate();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
