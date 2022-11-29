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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class MakeSchedulingGoalsUniqueMigrationTest {
  private static final String migrationName = "1_make_scheduling_goals_unique";
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
    final List<Integer> goalIds;
    {
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("""
          insert into scheduling_goal (name, definition, model_id)
          values
            ('alpha', '', 0),
            ('beta', '', 0),
            ('gamma', '', 0),
            ('delta', '', 0)
          returning id;
        """);
        final var results = new ArrayList<Integer>(4);
        while (res.next()) {
          results.add(res.getInt("id"));
        }
        goalIds = List.copyOf(results);
      }
    }

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

    /*
     Cases:
     1. Goal belongs to no specifications (0)
     2. Goal belongs to one specification (1)
     3. Goal belongs to two specifications (2)
     4. Goal belongs to three specifications (3)
     */

    // Goal 0 belongs to no specifications
    // Goal 1 is contained in specification 0
    helper.executeUpdate(
        "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 0)",
        specificationIds.get(0),
        goalIds.get(1));

    // Goal 2 is contained in specifications 0 and 1
    helper.executeUpdate(
        "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 1)",
        specificationIds.get(0),
        goalIds.get(2));
    helper.executeUpdate(
        "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 0)",
        specificationIds.get(1),
        goalIds.get(2));

    // Goal 3 is contained in specifications 0, 1, and 2
    helper.executeUpdate(
        "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 2)",
        specificationIds.get(0),
        goalIds.get(3));
    helper.executeUpdate(
        "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 1)",
        specificationIds.get(1),
        goalIds.get(3));
    helper.executeUpdate(
        "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 0)",
        specificationIds.get(2),
        goalIds.get(3));

    {
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("select count(1) from scheduling_goal;");
        res.next();
        final var numberOfGoals = res.getInt(1);
        assertEquals(4, numberOfGoals);
      }
    }

    final var originalGoalsBySpecification = getGoalsBySpecification();

    // Run migration. It should fail due to goals being shared across specifications.
    try {
      helper.applyMigration(migrationName);
      fail("Should have thrown exception due to goals being shared across specifications.");
    } catch (SQLException e) {
      assertTrue(e.getMessage().contains("ERROR:  could not create unique index \"scheduling_specification_unique_goal_id\""), "Message: \"" + e.getMessage() + '\"');
    }

    // Run prepare.sql, which will make copies of the shared goals.
    {
      helper.runSqlFile(
          migrationsDirectory
              .resolve(migrationName)
              .resolve("prepare.sql")
              .toString());
    }

    assertEquals(originalGoalsBySpecification, getGoalsBySpecification());

    // Now the migration should succeed:
    helper.applyMigration(migrationName);

    assertEquals(originalGoalsBySpecification, getGoalsBySpecification());

    // Number of goals should now be 7, since one additional copy of Goal 2 and two additional copies of Goal 3 must be made
    {
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("select count(1) from scheduling_goal;");
        res.next();
        final var numberOfGoals = res.getInt(1);
        assertEquals(7, numberOfGoals);
      }
    }

    // Goal 0 belongs to zero scheduling specifications. Adding it to two of them should fail due to the new constraint:
    helper.executeUpdate(
        "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 2)",
        specificationIds.get(0),
        goalIds.get(0));

    try {
      helper.executeUpdate(
          "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 2)",
          specificationIds.get(1),
          goalIds.get(0));
      fail("Should have thrown exception due to violating the new constraint.");
    } catch (SQLException e) {
      assertEquals("duplicate key value violates unique constraint \"scheduling_specification_unique_goal_id\"", e.getMessage());
    }

    // Rollback
    helper.rollbackMigration(migrationName);

    // It should now be possible to associate Goal 0 with a second specification:
    helper.executeUpdate(
        "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 2)",
        specificationIds.get(1),
        goalIds.get(0));
    helper.executeUpdate(
        "delete from scheduling_specification_goals where specification_id = %d and goal_id = %d",
        specificationIds.get(1),
        goalIds.get(0));

    // Re-do migration
    helper.applyMigration(migrationName);

    try {
      helper.executeUpdate(
          "insert into scheduling_specification_goals (specification_id, goal_id, priority) values (%d, %d, 2)",
          specificationIds.get(1),
          goalIds.get(0));
      fail("Should have thrown exception due to violating the new constraint.");
    } catch (SQLException e) {
      assertEquals("duplicate key value violates unique constraint \"scheduling_specification_unique_goal_id\"", e.getMessage());
    }
  }

  private HashMap<Integer, List<GoalRecord>> getGoalsBySpecification() throws SQLException {
    final var goalsBySpecification = new HashMap<Integer, List<GoalRecord>>();
    try (final var statement = helper.connection().createStatement()) {
      final var res = statement.executeQuery(
          """
              select
                specification_id,
                priority,
                revision,
                name,
                definition,
                model_id,
                description,
                author,
                last_modified_by,
                created_date,
                modified_date
              from scheduling_goal g
              join scheduling_specification_goals ssg
              on g.id = ssg.goal_id
              order by priority
              """
      );
      while (res.next()) {
        goalsBySpecification.computeIfAbsent(res.getInt("specification_id"), $ -> new ArrayList<>())
            .add(new GoalRecord(
                res.getInt("revision"),
                res.getString("name"),
                res.getString("definition"),
                res.getInt("model_id"),
                res.getString("description"),
                res.getString("author"),
                res.getString("last_modified_by"),
                res.getString("created_date"),
                res.getString("modified_date")
            ));
      }
    }
    return goalsBySpecification;
  }

  record GoalRecord(int revision,
                    String name,
                    String definition,
                    int model_id,
                    String description,
                    String author,
                    String last_modified_by,
                    String created_date,
                    String modified_date) {}

  @Test
  void testRollbackIdempotent() throws SQLException, IOException, InterruptedException {
    helper.runSqlFile(
        migrationsDirectory
            .resolve(migrationName)
            .resolve("prepare.sql")
            .toString());
    helper.applyMigration(migrationName);
    try {
      helper.applyMigration(migrationName);
      fail("Should have thrown exception due to attempting to re-add an existing constraint.");
    } catch (SQLException e) {
      assertTrue(e.getMessage().contains("ERROR:  relation \"scheduling_specification_unique_goal_id\" already exists"), e.getMessage());
    }

    helper.rollbackMigration(migrationName);
    helper.applyMigration(migrationName);

    helper.rollbackMigration(migrationName);
    helper.rollbackMigration(migrationName);

    helper.runSqlFile(
        migrationsDirectory
            .resolve(migrationName)
            .resolve("prepare.sql")
            .toString());
    helper.applyMigration(migrationName);
  }

  @Test
  void testRealisticVolume() throws SQLException, IOException, InterruptedException {
    // Insert 1000 goals
    // Associate 50 with 0 specifications, 500 with 1 specification, 300 with 2 specifications, 149 with 3 specifications and 1 with 100 specifications
    record SpecificationCount(int numGoals, int numSpecifications) {}
    final var specificationCounts = List.of(
        new SpecificationCount(50, 0),
        new SpecificationCount(500, 1),
        new SpecificationCount(300, 2),
        new SpecificationCount(149, 3),
        new SpecificationCount(1, 100)
    );

    final var goalIds = new ArrayList<Long>();
    for (var i = 0; i < 1000; i++) {
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("""
           insert into scheduling_goal (name, definition, model_id)
           values
             ('%s', '', 0)
           returning id;
         """.formatted(UUID.randomUUID()));
        while (res.next()) {
          goalIds.add(res.getLong("id"));
        }
      }
    }

    final var specificationIds = new ArrayList<Long>();
    try (final var statement = connection.prepareStatement("""
         insert into scheduling_specification (
           plan_id,
           plan_revision,
           horizon_start,
           horizon_end,
           simulation_arguments,
           analysis_only
         )
         values
           (?, 1, now(), now(), '{}', false);
       """, Statement.RETURN_GENERATED_KEYS)) {
      for (var i = 0; i < 100; i++) {
        statement.setInt(1, i);
        statement.addBatch();
      }
      statement.executeBatch();
      final var res = statement.getGeneratedKeys();
      while (res.next()) {
        specificationIds.add(res.getLong(1));
      }
    }

    final var goalIdIterator = goalIds.iterator();
    try (final var statement = helper.connection().prepareStatement(
        """
            insert into scheduling_specification_goals (specification_id, goal_id, priority) values (?, ?, 0)
            """
    )) {
      for (final var specificationCount : specificationCounts) {
        for (var i = 0; i < specificationCount.numGoals; i++) {
          final var goalId = goalIdIterator.next();
          for (var j = 0; j < specificationCount.numSpecifications; j++) {
            statement.setLong(1, specificationIds.get(j));
            statement.setLong(2, goalId);
            statement.addBatch();
          }
        }
        statement.executeBatch();
      }
    }

    final var originalGoalsBySpecification = getGoalsBySpecification();

    try {
      helper.applyMigration(migrationName);
      fail("Should have thrown exception due to goals being shared across specifications.");
    } catch (SQLException e) {
      assertTrue(e.getMessage().contains("ERROR:  could not create unique index \"scheduling_specification_unique_goal_id\""), "Message: \"" + e.getMessage() + '\"');
    }

    helper.runSqlFile(
        migrationsDirectory
            .resolve(migrationName)
            .resolve("prepare.sql")
            .toString());

    assertEquals(originalGoalsBySpecification, getGoalsBySpecification());

    helper.applyMigration(migrationName);
  }
}
