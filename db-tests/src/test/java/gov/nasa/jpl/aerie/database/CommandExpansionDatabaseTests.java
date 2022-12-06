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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandExpansionDatabaseTests {
  private static final File initSqlScriptFile = new File("../sequencing-server/sql/sequencing/init.sql");
  private DatabaseTestHelper helper;

  private Connection connection;

  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {
    helper = new DatabaseTestHelper(
        "command_expansion_test",
        "Command Expansion Database Tests",
        initSqlScriptFile
    );
    helper.startDatabaseWithLatestSchema();
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
      try (final var statement = connection.createStatement()) {
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

  @Nested
  class SequenceTriggers {
    @Test
    void shouldModifyUpdatedAtTimeOnUpdate() throws SQLException {
      try (final var statement = connection.createStatement()) {
        final var insertRes = statement.executeQuery("""
          insert into sequence (seq_id, simulation_dataset_id, metadata)
          values ('%s', 1, '{}')
          returning seq_id, simulation_dataset_id, created_at, updated_at
        """.formatted(UUID.randomUUID().toString()));
        insertRes.next();
        final var seq_id = insertRes.getString("seq_id");
        final var simulation_dataset_id = insertRes.getInt("simulation_dataset_id");
        final var created_at = insertRes.getTimestamp("created_at");
        final var updated_at = insertRes.getTimestamp("updated_at");

        assertEquals(created_at, updated_at);

        final var updateRes = statement.executeQuery("""
          update sequence set metadata = '{"key": "value"}'
          where seq_id = '%s' and simulation_dataset_id = %d
          returning created_at, updated_at
        """.formatted(seq_id, simulation_dataset_id));
        updateRes.next();
        final var created_at2 = updateRes.getTimestamp("created_at");
        final var updated_at2 = updateRes.getTimestamp("updated_at");

        assertEquals(created_at, created_at2);
        assertNotEquals(updated_at, updated_at2);
        assertNotEquals(created_at2, updated_at2);
      }
    }

    @Test
    void shouldModifyUpdatedAtTimeOnInsertToSequenceToSimulatedActivityTable() throws SQLException {
      try (final var statement = connection.createStatement()) {
        final var insertRes = statement.executeQuery("""
          insert into sequence (seq_id, simulation_dataset_id, metadata)
          values ('%s', 1, '{}')
          returning seq_id, simulation_dataset_id, created_at, updated_at
        """.formatted(UUID.randomUUID().toString()));
        insertRes.next();
        final var seq_id = insertRes.getString("seq_id");
        final var simulation_dataset_id = insertRes.getInt("simulation_dataset_id");
        final var created_at = insertRes.getTimestamp("created_at");
        final var updated_at = insertRes.getTimestamp("updated_at");

        assertEquals(created_at, updated_at);

        final var linkActivityRes = statement.executeQuery("""
          insert into sequence_to_simulated_activity (simulated_activity_id, simulation_dataset_id, seq_id)
          values (%d, %d, '%s')
          returning seq_id
        """.formatted(1, simulation_dataset_id, seq_id));
        linkActivityRes.next();

        final var queryRes = statement.executeQuery("""
          select created_at, updated_at
          from sequence
          where seq_id = '%s' and simulation_dataset_id = %d
        """.formatted(seq_id, simulation_dataset_id));
        queryRes.next();
        final var created_at2 = queryRes.getTimestamp("created_at");
        final var updated_at2 = queryRes.getTimestamp("updated_at");

        assertEquals(created_at, created_at2);
        assertNotEquals(updated_at, updated_at2);
        assertNotEquals(created_at2, updated_at2);
      }
    }
  }
}
