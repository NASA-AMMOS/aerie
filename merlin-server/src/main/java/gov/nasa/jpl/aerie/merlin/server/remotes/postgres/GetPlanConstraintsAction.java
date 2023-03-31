package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*package local*/ final class GetPlanConstraintsAction implements AutoCloseable {
  // We left join through the plan table in order to distinguish
  //   a plan without any constraints from a non-existent plan.
  // A plan without constraints will produce a placeholder row with nulls.
  private static final @Language("SQL") String sql = """
    select c.id, c.name, c.summary, c.description, c.definition
    from plan AS p
    left join "constraint" AS c
      on p.id = c.plan_id
    where p.id = ?
    """;

  private final PreparedStatement statement;

  public GetPlanConstraintsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<List<ConstraintRecord>> get(final long planId) throws SQLException {
    this.statement.setLong(1, planId);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) return Optional.empty();

      final var constraints = new ArrayList<ConstraintRecord>();
      do {
        if (results.getObject(1) == null) continue;

        final var constraint = new ConstraintRecord(
            results.getLong(1),
            results.getString(2),
            results.getString(3),
            results.getString(4),
            results.getString(5));

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
