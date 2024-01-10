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
  //   a plan without any enabled constraints from a non-existent plan.
  // A plan without any enabled constraints will produce a placeholder row with nulls.
  private static final @Language("SQL") String sql = """
    select c.constraint_id, c.revision, c.name, c.description, c.definition
    from plan p
      left join (select cs.plan_id, cs.constraint_id, cd.revision, cm.name, cm.description, cd.definition
                 from constraint_specification cs
                   left join constraint_definition cd using (constraint_id)
                   left join public.constraint_metadata cm on cs.constraint_id = cm.id
                 where cs.enabled
                   and ((cs.constraint_revision is not null
                           and cs.constraint_revision = cd.revision)
                          or (cs.constraint_revision is null
                                and cd.revision = (select def.revision
                                                   from constraint_definition def
                                                   where def.constraint_id = cs.constraint_id
                                                   order by def.revision desc limit 1)))
                   ) c
        on p.id = c.plan_id
    where p.id = ?;
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
            results.getLong(2),
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
