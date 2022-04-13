package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  int insertSpecification() throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        insert into scheduling_specification(
          revision, plan_id, plan_revision, horizon_start, horizon_end, simulation_arguments
        ) values (0, 0, 0, now(), now(), '{}') returning id;
      """);
      res.next();
      return res.getInt("id");
    }
  }

  int insertTemplate() throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        insert into scheduling_template(
          revision, model_id, description, simulation_arguments
        ) values (0, 0, 'it templates', '{}') returning id;
      """);
      res.next();
      return res.getInt("id");
    }
  }

  int insertGoal() throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        insert into scheduling_goal(
          revision, name, definition, model_id, description, author, last_modified_by, created_date, modified_date
        ) values (0, 'goal', 'does thing', 0, 'hey there', 'its me', 'also me', now(), now()) returning id;
      """);
      res.next();
      return res.getInt("id");
    }
  }

  void clearTable(String table) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate("TRUNCATE " + table + " CASCADE;");
    }
  }

  @Nested
  class TestSpecificationAndTemplateGoalTriggers {
    Map<String, int[]> specAndTemplateIds;
    int[] goalIds;

    @BeforeEach
    void beforeEach() throws SQLException {
      specAndTemplateIds = new HashMap<>();
      specAndTemplateIds.put("specification", new int[]{insertSpecification(), insertSpecification()});
      specAndTemplateIds.put("template", new int[]{insertTemplate(), insertTemplate()});
      goalIds = new int[]{insertGoal(), insertGoal(), insertGoal()};
    }

    @AfterEach
    void afterEach() throws SQLException {
      clearTable("scheduling_specification");
      clearTable("scheduling_template");
      clearTable("scheduling_goal");
      clearTable("scheduling_specification_goals");
      clearTable("scheduling_template_goals");
    }

    void insertGoalPriorities(String tableStem, int specOrTemplateIndex, int[] priorities) throws SQLException {
      for (int i = 0; i < priorities.length; i++) {
        connection.createStatement().executeUpdate("""
          insert into scheduling_%s_goals(%s_id, goal_id, priority)
          values (%d, %d, %d);
        """.formatted(
            tableStem, tableStem,
            specAndTemplateIds.get(tableStem)[specOrTemplateIndex],
            goalIds[i],
            priorities[i]
        ));
      }
    }

    void checkPriorities(String tableStem, int specOrTemplateIndex, int[] goalIdIndices, int[] priorities) throws SQLException {
      assertEquals(goalIdIndices.length, priorities.length);
      for (int i = 0; i < priorities.length; i++) {
        final var res = connection.createStatement().executeQuery("""
          select priority from scheduling_%s_goals
          where goal_id = %d and %s_id = %d;
        """.formatted(
            tableStem,
            goalIds[goalIdIndices[i]],
            tableStem,
            specAndTemplateIds.get(tableStem)[specOrTemplateIndex])
        );
        res.next();
        assertEquals(priorities[i], res.getInt("priority"));
        res.close();
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"specification", "template"})
    void shouldIncrementPrioritiesOnCollision(String tableStem) throws SQLException {
      // untouched values in table, should be unchanged
      insertGoalPriorities(tableStem, 1, new int[]{0, 1, 2});

      // should cause increments
      insertGoalPriorities(tableStem, 0, new int[]{0, 0, 0});

      checkPriorities(
          tableStem, 0,
          new int[]{0, 1},
          new int[]{2, 1}
      );
      checkPriorities(
          tableStem, 1,
          new int[]{0, 1, 2},
          new int[]{0, 1, 2}
      );
    }

    @ParameterizedTest
    @ValueSource(strings = {"specification", "template"})
    void shouldErrorWhenInsertingNegativePriority(String tableStem) {
      assertThrows(SQLException.class, () -> insertGoalPriorities(
          tableStem, 0, new int[]{-1}
      ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"specification", "template"})
    void shouldErrorWhenInsertingNonConsecutivePriority(String tableStem) throws SQLException {
      insertGoalPriorities(tableStem, 1, new int[]{0, 1, 2});
      assertThrows(SQLException.class, () -> insertGoalPriorities(
          tableStem, 0, new int[]{1}
      ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"specification", "template"})
    void shouldReorderPrioritiesOnUpdate(String tableStem) throws SQLException {
      insertGoalPriorities(tableStem, 0, new int[]{0, 1, 2});
      insertGoalPriorities(tableStem, 1, new int[]{0, 1, 2});

      // First test lowering a priority
      connection.createStatement().executeUpdate("""
        update scheduling_%s_goals
        set priority = 0 where %s_id = %d and goal_id = %d;
      """.formatted(tableStem, tableStem, specAndTemplateIds.get(tableStem)[0], goalIds[2]));
      checkPriorities(
          tableStem, 0,
          new int[]{0, 1, 2},
          new int[]{1, 2, 0}
      );
      checkPriorities(
          tableStem, 1,
          new int[]{0, 1, 2},
          new int[]{0, 1, 2}
      );

      /// Next test raising a priority
      connection.createStatement().executeUpdate("""
        update scheduling_%s_goals
        set priority = 2 where %s_id = %d and goal_id = %d;
      """.formatted(tableStem, tableStem, specAndTemplateIds.get(tableStem)[0], goalIds[2]));
      for (int i : new int[]{0, 1}) {
        checkPriorities(
            tableStem, i,
            new int[]{0, 1, 2},
            new int[]{0, 1, 2}
        );
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"specification", "template"})
    void shouldDecrementPrioritiesOnDelete(String tableStem) throws SQLException {
      insertGoalPriorities(tableStem, 0, new int[]{0, 1, 2});
      insertGoalPriorities(tableStem, 1, new int[]{0, 1, 2});

      connection.createStatement().executeUpdate("""
        delete from scheduling_%s_goals
        where %s_id = %d and goal_id = %d;
      """.formatted(tableStem, tableStem, specAndTemplateIds.get(tableStem)[0], goalIds[1]));
      checkPriorities(
          tableStem, 0,
          new int[]{0, 2},
          new int[]{0, 1}
      );
      checkPriorities(
          tableStem, 1,
          new int[]{0, 1, 2},
          new int[]{0, 1, 2}
      );
    }

    @ParameterizedTest
    @ValueSource(strings = {"specification", "template"})
    void shouldTriggerMultipleReorders(String tableStem) throws SQLException {
      insertGoalPriorities(tableStem, 0, new int[]{0, 1, 2});
      insertGoalPriorities(tableStem, 1, new int[]{0, 1, 2});

      connection.createStatement().executeUpdate("""
        delete from scheduling_%s_goals
        where goal_id = %d;
      """.formatted(tableStem, goalIds[1]));
      for (int i : new int[]{0, 1}) {
        checkPriorities(
            tableStem, i,
            new int[]{0, 2},
            new int[]{0, 1}
        );
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"specification", "template"})
    void shouldNotTriggerWhenPriorityIsUnchanged(String tableStem) throws SQLException {
      insertGoalPriorities(tableStem, 1, new int[]{0, 1, 2});
      connection.createStatement().executeUpdate("""
        update scheduling_%s_goals
        set %s_id = %d
        where %s_id = %d;
      """.formatted(
          tableStem,
          tableStem,
          specAndTemplateIds.get(tableStem)[0],
          tableStem,
          specAndTemplateIds.get(tableStem)[1]
      ));
      checkPriorities(
          tableStem, 0,
          new int[]{0, 1, 2},
          new int[]{0, 1, 2}
      );
    }

    @ParameterizedTest
    @ValueSource(strings = {"specification", "template"})
    void shouldGeneratePriorityWhenNull(String tableStem) throws SQLException {
      connection.createStatement().executeUpdate("""
          insert into scheduling_%s_goals(%s_id, goal_id)
          values (%d, %d);
        """.formatted(
          tableStem, tableStem,
          specAndTemplateIds.get(tableStem)[0],
          goalIds[0]
      ));
      checkPriorities(
          tableStem, 0,
          new int[]{0},
          new int[]{0}
      );
      connection.createStatement().executeUpdate("""
          insert into scheduling_%s_goals(%s_id, goal_id, priority)
          values (%d, %d, null);
        """.formatted(
          tableStem, tableStem,
          specAndTemplateIds.get(tableStem)[0],
          goalIds[2]
      ));
      checkPriorities(
          tableStem, 0,
          new int[]{0, 2},
          new int[]{0, 1}
      );
      connection.createStatement().executeUpdate("""
          insert into scheduling_%s_goals(%s_id, goal_id)
          values (%d, %d);
        """.formatted(
          tableStem, tableStem,
          specAndTemplateIds.get(tableStem)[0],
          goalIds[1]
      ));
      checkPriorities(
          tableStem, 0,
          new int[]{0, 2, 1},
          new int[]{0, 1, 2}
      );
    }
  }

}
