package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*package-local*/ final class GetModelConstraintsAction implements AutoCloseable {
  // We left join through the mission_model table in order to distinguish
  //   a mission model without any constraints from a non-existent mission model.
  // A mission model without constraints will produce a placeholder row with nulls.
  private static final @Language("SQL") String sql = """
    select c.id, c.name, c.description, c.definition
    from mission_model AS m
    left join "constraint" AS c
      on m.id = c.model_id
    where m.id = ?
    """;

  private final PreparedStatement statement;

  public GetModelConstraintsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<List<ConstraintRecord>> get(final long modelId) throws SQLException {
    this.statement.setLong(1, modelId);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) return Optional.empty();

      final var constraints = new ArrayList<ConstraintRecord>();
      do {
        if (results.getObject(1) == null) continue;

        final var constraint = new ConstraintRecord(
            results.getLong(1),
            results.getString(2),
            results.getString(3),
            results.getString(4));

        constraints.add(constraint);
      } while (results.next());

      return Optional.of(constraints);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
