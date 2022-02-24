package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class CreateRequestAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      insert into scheduling_request (specification_id, specification_revision)
      values (?, ?)
      returning analysis_id
    """;

  private final PreparedStatement statement;

  public CreateRequestAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public RequestRecord apply(final SpecificationRecord specification) throws SQLException {
    this.statement.setLong(1, specification.id());
    this.statement.setLong(2, specification.revision());

    final var result = this.statement.executeQuery();
    if (!result.next()) throw new FailedInsertException("scheduling_request");

    final var analysis_id = result.getLong(1);

    return new RequestRecord(
        specification.id(),
        analysis_id,
        specification.revision()
    );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
