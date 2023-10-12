package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchedulerDatabaseTests {
  private static final File initSqlScriptFile = new File("../scheduler-server/sql/scheduler/init.sql");
  private DatabaseTestHelper helper;

  private Connection connection;

  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {
    helper = new DatabaseTestHelper(
        "aerie_scheduler_test",
        "Scheduler Database Tests",
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

  int insertSpecification(final long planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        insert into scheduling_specification(
          revision, plan_id, plan_revision, horizon_start, horizon_end, simulation_arguments, analysis_only
        ) values (0, %d, 0, now(), now(), '{}', false) returning id;
      """.formatted(planId));
      res.next();
      return res.getInt("id");
    }
  }

  int insertGoal() throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        insert into scheduling_goal(
          revision, name, definition, model_id, description, author, last_modified_by, created_date, modified_date
        ) values (0, 'goal', 'does thing', 0, 'hey there', 'its me', 'also me', now(), now()) returning id;
      """);
      res.next();
      return res.getInt("id");
    }
  }


  @Nested
  class TestSpecificationAndTemplateGoalTriggers {
    int[] specificationIds;
    int[] goalIds;

    @BeforeEach
    void beforeEach() throws SQLException {
      specificationIds = new int[]{insertSpecification(0), insertSpecification(1)};
      goalIds = new int[]{insertGoal(), insertGoal(), insertGoal(), insertGoal(), insertGoal(), insertGoal()};
    }

    @AfterEach
    void afterEach() throws SQLException {
      helper.clearTable("scheduling_specification");
      helper.clearTable("scheduling_goal");
      helper.clearTable("scheduling_specification_goals");
    }

    void insertGoalPriorities(int specOrTemplateIndex, final int[] goalIndices, int[] priorities) throws SQLException {
      for (int i = 0; i < priorities.length; i++) {
        connection.createStatement().executeUpdate("""
          insert into scheduling_specification_goals(specification_id, goal_id, priority)
          values (%d, %d, %d);
        """.formatted(
            specificationIds[specOrTemplateIndex],
            goalIds[goalIndices[i]],
            priorities[i]
        ));
      }
    }

    void checkPriorities(int specOrTemplateIndex, int[] goalIdIndices, int[] priorities) throws SQLException {
      assertEquals(goalIdIndices.length, priorities.length);
      for (int i = 0; i < priorities.length; i++) {
        final var res = connection.createStatement().executeQuery("""
          select priority from scheduling_specification_goals
          where goal_id = %d and specification_id = %d;
        """.formatted(
            goalIds[goalIdIndices[i]],
            specificationIds[specOrTemplateIndex])
        );
        res.next();
        assertEquals(priorities[i], res.getInt("priority"));
        res.close();
      }
    }

    @Test
    void shouldIncrementPrioritiesOnCollision() throws SQLException {
      // untouched values in table, should be unchanged
      insertGoalPriorities(1, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      checkPriorities(1, new int[]{0, 1, 2}, new int[]{0, 1, 2});

      helper.clearTable("scheduling_specification_goals");
      // should cause increments
      insertGoalPriorities(0, new int[] {0, 1, 2}, new int[]{0, 0, 0});
      checkPriorities(0, new int[]{0, 1, 2}, new int[]{2, 1, 0});
    }

    @Test
    void shouldErrorWhenInsertingNegativePriority() {
      assertThrows(SQLException.class, () -> insertGoalPriorities(
          0, new int[] {0, 1, 2}, new int[]{-1}
      ));
    }

    @Test
    void shouldErrorWhenInsertingNonConsecutivePriority() throws SQLException {
      insertGoalPriorities(1, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      assertThrows(SQLException.class, () -> insertGoalPriorities(
          0, new int[] {0, 1, 2}, new int[]{1}
      ));
    }

    private int getSpecificationRevision(int specificationId) throws SQLException {
      final var res = connection.createStatement().executeQuery("""
          select revision from scheduling_specification
          where id = %d;
          """.formatted(specificationId));
      res.next();
      return res.getInt("revision");
    }

    @Test
    void shouldIncrementSpecRevisionAfterModifyingGoal() throws SQLException {
      insertGoalPriorities(0, new int[] {0, 1, 2, 3, 4}, new int[]{0, 1, 2, 3, 4});
      final var revisionBefore  = getSpecificationRevision(specificationIds[0]);
      connection.createStatement().executeUpdate("""
        update scheduling_goal
        set name = 'other name' where id = %d;
      """.formatted(goalIds[3]));
      final var revisionAfter  = getSpecificationRevision(specificationIds[0]);
      assertEquals(revisionBefore + 1, revisionAfter);
    }

    @Test
    void shouldReorderPrioritiesOnUpdate() throws SQLException {
      insertGoalPriorities(0, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      insertGoalPriorities(1, new int[] {3, 4, 5}, new int[]{0, 1, 2});

      // First test lowering a priority
      connection.createStatement().executeUpdate("""
        update scheduling_specification_goals
        set priority = 0 where specification_id = %d and goal_id = %d;
      """.formatted(specificationIds[0], goalIds[2]));
      checkPriorities( 0, new int[]{0, 1, 2}, new int[]{1, 2, 0});
      checkPriorities( 1, new int[]{3, 4, 5}, new int[]{0, 1, 2});

      /// Next test raising a priority
      connection.createStatement().executeUpdate("""
        update scheduling_specification_goals
        set priority = 2 where specification_id = %d and goal_id = %d;
      """.formatted(specificationIds[0], goalIds[2]));
      checkPriorities( 0, new int[] {0, 1, 2}, new int[] {0, 1, 2});
      checkPriorities( 1, new int[] {3, 4, 5}, new int[] {0, 1, 2});
    }

    @Test
    void shouldDecrementPrioritiesOnDelete() throws SQLException {
      insertGoalPriorities(0, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      insertGoalPriorities(1, new int[] {3, 4, 5}, new int[]{0, 1, 2});

      connection.createStatement().executeUpdate("""
        delete from scheduling_specification_goals
        where specification_id = %d and goal_id = %d;
      """.formatted(specificationIds[0], goalIds[1]));
      checkPriorities(0, new int[]{0, 2}, new int[]{0, 1});
      checkPriorities(1, new int[]{3, 4, 5}, new int[]{0, 1, 2});
    }

    @Test
    void shouldTriggerMultipleReorders() throws SQLException {
      insertGoalPriorities(0, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      insertGoalPriorities(1, new int[] {3, 4, 5}, new int[]{0, 1, 2});

      connection.createStatement().executeUpdate("""
        delete from scheduling_specification_goals
        where goal_id = %d;
      """.formatted(goalIds[1]));
      connection.createStatement().executeUpdate("""
        delete from scheduling_specification_goals
        where goal_id = %d;
      """.formatted(goalIds[4]));
      checkPriorities( 0, new int[] {0, 2}, new int[] {0, 1});
      checkPriorities(1, new int[] {3, 5}, new int[] {0, 1});
    }

    @Test
    void shouldNotTriggerWhenPriorityIsUnchanged() throws SQLException {
      insertGoalPriorities(1, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      connection.createStatement().executeUpdate("""
        update scheduling_specification_goals
        set specification_id = %d
        where specification_id = %d;
      """.formatted(
          specificationIds[0],
          specificationIds[1]
      ));
      checkPriorities(0, new int[]{0, 1, 2}, new int[]{0, 1, 2});
    }

    @Test
    void shouldGeneratePriorityWhenNull() throws SQLException {
      connection.createStatement().executeUpdate("""
          insert into scheduling_specification_goals(specification_id, goal_id)
          values (%d, %d);
        """.formatted(
          specificationIds[0],
          goalIds[0]
      ));
      checkPriorities(0, new int[]{0}, new int[]{0});
      connection.createStatement().executeUpdate("""
          insert into scheduling_specification_goals(specification_id, goal_id, priority)
          values (%d, %d, null);
        """.formatted(
          specificationIds[0],
          goalIds[2]
      ));
      checkPriorities(0, new int[]{0, 2}, new int[]{0, 1});
      connection.createStatement().executeUpdate("""
          insert into scheduling_specification_goals(specification_id, goal_id)
          values (%d, %d);
        """.formatted(
          specificationIds[0],
          goalIds[1]
      ));
      checkPriorities(0, new int[]{0, 2, 1}, new int[]{0, 1, 2});
    }
  }

}
