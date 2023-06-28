package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.remotes.PermissionType;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;

/*package local*/ final class LookupPermissionTypeAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      action_permissions
    from metadata.user_role_permissions
    where role = ?
    """;

  private final PreparedStatement statement;

  public LookupPermissionTypeAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public PermissionType get(final String role, final String action)
  throws SQLException {
    this.statement.setString(1, role);
    final var results = this.statement.executeQuery();

    if (!results.next()) return PermissionType.ALWAYS_UNAUTHORIZED;

    final var permissionTypesByAction = parse(results.getCharacterStream("action_permissions"));
    if (!permissionTypesByAction.containsKey(action)) return PermissionType.ALWAYS_UNAUTHORIZED;

    final var permissionTypeString = permissionTypesByAction.get(action);
    try {
      return PermissionType.valueOf(permissionTypeString);
    } catch (IllegalArgumentException e) {
      throw new SQLException("Invalid permission type " + permissionTypeString);
    }
  }

  private Map<String, String> parse(final Reader stream) {
    try (final var reader = Json.createReader(stream)) {
      final var json = reader.readValue();
      return mapP(stringP).parse(json).getSuccessOrThrow($ -> new RuntimeException($.toString()));
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
