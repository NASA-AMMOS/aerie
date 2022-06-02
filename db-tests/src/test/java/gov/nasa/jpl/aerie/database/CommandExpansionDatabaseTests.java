package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandExpansionDatabaseTests {
  private static final File initSqlScriptFile = new File("../command-expansion-server/sql/commanding/init.sql");
  private DatabaseTestHelper helper;

  private Connection connection;

  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {
    helper = new DatabaseTestHelper(
        "command_expansion_test",
        "Command Expansion Database Tests",
        initSqlScriptFile
    );
    helper.startDatabase();
    connection = helper.connection();
  }

  @AfterAll
  void afterAll() throws SQLException, IOException, InterruptedException {
    helper.stopDatabase();
    connection = null;
    helper = null;
  }

  @Nested
  class ExpansionRuleTriggers {
    @Test
    void shouldModifyUpdatedAtTimeOnUpdate() throws SQLException {
      try(final var statement = connection.createStatement()) {
        final var insertRes = statement.executeQuery("""
        insert into expansion_rule (activity_type, expansion_logic)
        values ('test-activity-type', 'test-activity-logic')
        returning id, created_at, updated_at
      """);
        insertRes.next();
        final var id = insertRes.getInt("id");
        final var created_at = insertRes.getTimestamp("created_at");
        final var updated_at = insertRes.getTimestamp("updated_at");

        assertEquals(created_at, updated_at);

        final var updateRes = statement.executeQuery("""
        update expansion_rule set expansion_logic = 'updated-logic'
        where id = %d
        returning created_at, updated_at
      """.formatted(id));
        updateRes.next();
        final var created_at2 = updateRes.getTimestamp("created_at");
        final var updated_at2 = updateRes.getTimestamp("updated_at");

        assertEquals(created_at, created_at2);
        assertNotEquals(updated_at, updated_at2);
        assertNotEquals(created_at2, updated_at2);
      }
    }
  }
}
