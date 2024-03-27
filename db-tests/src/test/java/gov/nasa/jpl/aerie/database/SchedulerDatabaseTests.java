package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchedulerDatabaseTests {
  private DatabaseTestHelper helper;
  private MerlinDatabaseTestHelper merlinHelper;

  private Connection connection;

  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {
    helper = new DatabaseTestHelper("aerie_scheduler_test", "Scheduler Database Tests");
    connection = helper.connection();
    merlinHelper = new MerlinDatabaseTestHelper(connection);
    merlinHelper.insertUser("scheduler db tests");
  }

  @AfterAll
  void afterAll() throws SQLException, IOException, InterruptedException {
    helper.close();
  }

  int getSpecification(final long planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          //language=sql
          """
          select id from scheduler.scheduling_specification
          where plan_id = %d;
          """.formatted(planId));
      res.next();
      return res.getInt("id");
    }
  }

  int insertGoal() throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          //language=sql
          """
          with metadata(id, owner) as (
            insert into scheduler.scheduling_goal_metadata(name, description, owner, updated_by)
            values ('test goal', 'no-op', 'scheduler db tests', 'scheduler db tests')
            returning id, owner
          )
          insert into scheduler.scheduling_goal_definition(goal_id, definition, author)
          select m.id, 'nothing', m.owner
          from metadata m
          returning goal_id as id;
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
      final int modelId = merlinHelper.insertMissionModel(merlinHelper.insertFileUpload());
      specificationIds = new int[]{
          getSpecification(merlinHelper.insertPlan(modelId)),
          getSpecification(merlinHelper.insertPlan(modelId))
      };
      goalIds = new int[]{insertGoal(), insertGoal(), insertGoal(), insertGoal(), insertGoal(), insertGoal()};
    }

    @AfterEach
    void afterEach() throws SQLException {
      helper.clearSchema("merlin");
      helper.clearSchema("scheduler");
    }

    void insertGoalPriorities(int specOrTemplateIndex, final int[] goalIndices, int[] priorities) throws SQLException {
      for (int i = 0; i < priorities.length; i++) {
        connection.createStatement().executeUpdate(
            //language=sql
            """
            insert into scheduler.scheduling_specification_goals(specification_id, goal_id, priority)
            values (%d, %d, %d);
            """.formatted(specificationIds[specOrTemplateIndex], goalIds[goalIndices[i]], priorities[i]));
      }
    }

    void checkPriorities(int specOrTemplateIndex, int[] goalIdIndices, int[] priorities) throws SQLException {
      assertEquals(goalIdIndices.length, priorities.length);
      for (int i = 0; i < priorities.length; i++) {
        final var res = connection.createStatement().executeQuery(
            //language=sql
            """
            select priority from scheduler.scheduling_specification_goals
            where goal_id = %d and specification_id = %d;
            """.formatted(goalIds[goalIdIndices[i]], specificationIds[specOrTemplateIndex]));
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

      helper.clearTable("scheduler.scheduling_specification_goals");
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
      final var res = connection.createStatement().executeQuery(
          //language=sql
          """
          select revision from scheduler.scheduling_specification
          where id = %d;
          """.formatted(specificationId));
      res.next();
      return res.getInt("revision");
    }

    @Test
    void shouldIncrementSpecRevisionAfterModifyingGoalSpec() throws SQLException {
      insertGoalPriorities(0, new int[] {0, 1, 2, 3, 4}, new int[]{0, 1, 2, 3, 4});
      final var revisionBefore  = getSpecificationRevision(specificationIds[0]);
      try(final var statement = connection.createStatement()){
        statement.executeUpdate(
            //language=sql
            """
            update scheduler.scheduling_specification_goals
            set goal_revision = 0
            where goal_id = %d;
            """.formatted(goalIds[3]));
      }
      final var revisionAfter  = getSpecificationRevision(specificationIds[0]);
      assertEquals(revisionBefore + 1, revisionAfter);
    }

    @Test
    void shouldReorderPrioritiesOnUpdate() throws SQLException {
      insertGoalPriorities(0, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      insertGoalPriorities(1, new int[] {3, 4, 5}, new int[]{0, 1, 2});

      try(final var statement = connection.createStatement()) {
        // First test lowering a priority
        statement.executeUpdate(
            //language=sql
            """
            update scheduler.scheduling_specification_goals
            set priority = 0
            where specification_id = %d
            and goal_id = %d;
            """.formatted(specificationIds[0], goalIds[2]));
        checkPriorities(0, new int[]{0, 1, 2}, new int[]{1, 2, 0});
        checkPriorities(1, new int[]{3, 4, 5}, new int[]{0, 1, 2});

        /// Next test raising a priority
        statement.executeUpdate(
            //language=sql
            """
            update scheduler.scheduling_specification_goals
            set priority = 2
            where specification_id = %d
            and goal_id = %d;
            """.formatted(specificationIds[0], goalIds[2]));
      }
      checkPriorities(0, new int[] {0, 1, 2}, new int[] {0, 1, 2});
      checkPriorities(1, new int[] {3, 4, 5}, new int[] {0, 1, 2});
    }

    @Test
    void shouldDecrementPrioritiesOnDelete() throws SQLException {
      insertGoalPriorities(0, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      insertGoalPriorities(1, new int[] {3, 4, 5}, new int[]{0, 1, 2});

      try(final var statement = connection.createStatement()) {
        statement.executeUpdate(
            //language=sql
            """
            delete from scheduler.scheduling_specification_goals
            where specification_id = %d
            and goal_id = %d;
            """.formatted(specificationIds[0], goalIds[1]));
      }
      checkPriorities(0, new int[]{0, 2}, new int[]{0, 1});
      checkPriorities(1, new int[]{3, 4, 5}, new int[]{0, 1, 2});
    }

    @Test
    void shouldTriggerMultipleReorders() throws SQLException {
      insertGoalPriorities(0, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      insertGoalPriorities(1, new int[] {3, 4, 5}, new int[]{0, 1, 2});

      try(final var statement=connection.createStatement()) {
        statement.executeUpdate(
            //language=sql
            """
            delete from scheduler.scheduling_specification_goals
            where goal_id = %d;
            """.formatted(goalIds[1]));
        statement.executeUpdate(
            //language=sql
            """
            delete from scheduler.scheduling_specification_goals
            where goal_id = %d;
            """.formatted(goalIds[4]));
      }
      checkPriorities( 0, new int[] {0, 2}, new int[] {0, 1});
      checkPriorities(1, new int[] {3, 5}, new int[] {0, 1});
    }

    @Test
    void shouldNotTriggerWhenPriorityIsUnchanged() throws SQLException {
      insertGoalPriorities(1, new int[] {0, 1, 2}, new int[]{0, 1, 2});
      try(final var statement = connection.createStatement()) {
        statement.executeUpdate(
            //language=sql
            """
            update scheduler.scheduling_specification_goals
            set specification_id = %d
            where specification_id = %d;
            """.formatted(specificationIds[0], specificationIds[1]));
      }
      checkPriorities(0, new int[]{0, 1, 2}, new int[]{0, 1, 2});
    }

    @Test
    void shouldGeneratePriorityWhenNull() throws SQLException {
      try(final var statement = connection.createStatement()) {
        statement.executeUpdate(
            //language=sql
            """
            insert into scheduler.scheduling_specification_goals(specification_id, goal_id)
            values (%d, %d);
            """.formatted(specificationIds[0], goalIds[0]));
        checkPriorities(0, new int[] {0}, new int[] {0});
        statement.executeUpdate(
            //language=sql
            """
            insert into scheduler.scheduling_specification_goals(specification_id, goal_id, priority)
            values (%d, %d, null);
            """.formatted(specificationIds[0], goalIds[2]));
        checkPriorities(0, new int[] {0, 2}, new int[] {0, 1});
        statement.executeUpdate(
            //language=sql
            """
            insert into scheduler.scheduling_specification_goals(specification_id, goal_id)
            values (%d, %d);
            """.formatted(specificationIds[0], goalIds[1]));
        checkPriorities(0, new int[] {0, 2, 1}, new int[] {0, 1, 2});
      }
    }
  }
}
