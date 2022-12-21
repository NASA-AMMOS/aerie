package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test brings up two databases - one using migrations, and one using the latest state, and makes sure that
 * their schemas match.
 */
public class EnsureMigrationsAndSchemasMatch {
  @Test
  void testMigrationsAndSchemasMatch() throws SQLException, IOException, InterruptedException {
    // bring up a database using migrations
    // bring up a database using the state
    // do a diff
    final File initSqlScriptFile = new File("../scheduler-server/sql/scheduler/init.sql");
    final Path migrationsDirectory = Path.of(System.getenv("AERIE_ROOT"), "deployment", "hasura", "migrations", "AerieScheduler");
    final var migrationsDbHelper = new DatabaseTestHelper(
        "aerie_scheduler_schema_test_migrations",
        "Scheduler Database Schema Test using Migrations",
        initSqlScriptFile
    );
    migrationsDbHelper.stopDatabase();
    migrationsDbHelper.startDatabaseWithLatestSchema();
    final var stateDbHelper = new DatabaseTestHelper(
        "aerie_scheduler_schema_test_state",
        "Scheduler Database Schema Test using State",
        initSqlScriptFile,
        migrationsDirectory
    );
    stateDbHelper.stopDatabase();
    stateDbHelper.startDatabaseWithLatestSchema();
    assertEquals(migrationsDbHelper.dumpSchema(), stateDbHelper.dumpSchema());
    assertEquals(getSchemaMigrations(migrationsDbHelper), getSchemaMigrations(stateDbHelper));
  }

  private List<String> getSchemaMigrations(DatabaseTestHelper helper) throws SQLException {
    try (final var statement = helper.connection().createStatement()) {
      final var res = statement.executeQuery("""
        select version from schema_migrations order by version
      """);
      final var versions = new ArrayList<String>();
      while (res.next()) {
        versions.add(res.getString("version"));
      }
      return versions;
    }
  }
}
