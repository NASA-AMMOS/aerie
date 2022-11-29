package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class OneSchedulingSpecificationPerPlanMigrationTest {
  private static final String migrationName = "2_one_scheduling_spec_per_plan";
  private static final File initSqlScriptFile = new File("../scheduler-server/sql/scheduler/init.sql");
  public static final Path migrationsDirectory = Path.of(System.getenv("AERIE_ROOT"), "deployment", "hasura", "migrations", "AerieScheduler");
  private DatabaseTestHelper helper;

  private Connection connection;

  @BeforeEach
  void beforeEach() throws SQLException, IOException, InterruptedException {
    helper = new DatabaseTestHelper(
        "aerie_scheduler_migration_test",
        "Scheduler Database Migration Tests",
        initSqlScriptFile,
        migrationsDirectory
    );
    helper.startDatabaseBeforeMigration(migrationName);
    connection = helper.connection();
  }

  @AfterEach
  void afterEach() throws SQLException, IOException, InterruptedException {
    helper.stopDatabase();
    connection = null;
    helper = null;
  }

  @Test
  void testMigration() throws IOException, InterruptedException, SQLException {
    // Populate with test data
    final List<Integer> specificationIds;
    {
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("""
          insert into scheduling_specification (
            plan_id,
            plan_revision,
            horizon_start,
            horizon_end,
            simulation_arguments,
            analysis_only
          )
          values
            (1, 1, now(), now(), '{}', false),
            (1, 1, now(), now(), '{}', false),
            (2, 1, now(), now(), '{}', false)
          returning id;
        """);
        final var results = new ArrayList<Integer>(4);
        while (res.next()) {
          results.add(res.getInt("id"));
        }
        specificationIds = List.copyOf(results);
      }
    }

    // Run migration. It should fail due to specs 0 and 1 being associated with the same plan
    try {
      helper.applyMigration(migrationName);
      fail("Migration should have failed because there are two scheduling specifications associated with the same plan");
    } catch (SQLException e) {
      assertTrue(e.getMessage().contains("ERROR:  could not create unique index \"scheduling_specification_unique_plan_id\""), "Message: \"" + e.getMessage() + '\"');
    }

    helper.executeUpdate(
        "delete from scheduling_specification where id = %d",
        specificationIds.get(1)
    );

    helper.applyMigration(migrationName);

    try {
      helper.executeUpdate(
          """
                insert into scheduling_specification (
                  plan_id,
                  plan_revision,
                  horizon_start,
                  horizon_end,
                  simulation_arguments,
                  analysis_only
                )
                values
                  (1, 1, now(), now(), '{}', false)
              """
      );
    } catch (SQLException e) {
      assertEquals("duplicate key value violates unique constraint \"scheduling_specification_unique_plan_id\"", e.getMessage());
    }

    // Inserting a specification for another plan should work fine:
    helper.executeUpdate(
        """
              insert into scheduling_specification (
                plan_id,
                plan_revision,
                horizon_start,
                horizon_end,
                simulation_arguments,
                analysis_only
              )
              values
                (3, 1, now(), now(), '{}', false)
            """
    );

    // Deleting specifications should work fine:
    helper.executeUpdate(
        "delete from scheduling_specification where plan_id = 1",
        specificationIds.get(1)
    );
  }

  @Test
  void testRollbackIdempotent() throws SQLException, IOException, InterruptedException {
    helper.applyMigration(migrationName);
    try {
      helper.applyMigration(migrationName);
      fail("Should have thrown exception due to re-adding an existing constraint.");
    } catch (SQLException e) {
      assertTrue(e.getMessage().contains("ERROR:  relation \"scheduling_specification_unique_plan_id\" already exists"), e.getMessage());
    }

    helper.rollbackMigration(migrationName);
    helper.applyMigration(migrationName);

    helper.rollbackMigration(migrationName);
    helper.rollbackMigration(migrationName);
    helper.applyMigration(migrationName);
  }
}
