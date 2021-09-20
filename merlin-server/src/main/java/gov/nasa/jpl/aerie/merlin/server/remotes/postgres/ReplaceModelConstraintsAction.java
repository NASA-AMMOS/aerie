package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

/*package-local*/ final class ReplaceModelConstraintsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into mission_model_condition (model_id, name, summary, description, definition)
    values (?, ?, ?, ?, ?)
    on conflict on constraint mission_model_condition_has_unique_name do update
    set
      summary = excluded.summary,
      description = excluded.description,
      definition = excluded.definition
    """;

  private final PreparedStatement statement;

  public ReplaceModelConstraintsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void add(final long modelId, final Constraint constraint)
  throws SQLException, AdaptationRepository.NoSuchAdaptationException
  {
    this.statement.setLong(1, modelId);
    this.statement.setString(2, constraint.name());
    this.statement.setString(3, constraint.summary());
    this.statement.setString(4, constraint.description());
    this.statement.setString(5, constraint.definition());

    this.statement.addBatch();
  }

  public void apply() throws SQLException, AdaptationRepository.NoSuchAdaptationException {
    try {
      final var results = this.statement.executeBatch();
      for (final var result : results) {
        if (result != 1) throw new FailedInsertException("mission_model_condition");
      }
    } catch (final SQLException ex) {
      // https://www.postgresql.org/docs/current/errcodes-appendix.html
      if (Objects.equals(ex.getSQLState(), "23503")) {  /* foreign_key_violation */
        // The only foreign key on the `mission_model_condition` table references `mission_model`,
        // so there must not be a mission model with the given ID.
        throw new AdaptationRepository.NoSuchAdaptationException();
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
