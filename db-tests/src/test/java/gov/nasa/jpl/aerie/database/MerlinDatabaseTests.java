package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

record SimulationDatasetRecord(int simulation_id, int dataset_id){}
record PlanDatasetRecord(int plan_id, int dataset_id) {}


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MerlinDatabaseTests {
  private static final File initSqlScriptFile = new File("../merlin-server/sql/merlin/init.sql");
  private DatabaseTestHelper helper;

  private Connection connection;

  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {
    helper = new DatabaseTestHelper(
        "aerie_merlin_test",
        "Merlin Database Tests",
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

  int insertFileUpload() throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO uploaded_file (path, name)
                  VALUES ('test-path', 'test-name-%s')
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString())
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertMissionModel(final int fileId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO mission_model (name, mission, owner, version, jar_id)
                  VALUES ('test-mission-model-%s', 'test-mission', 'tester', '0', %s)
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString(), fileId)
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertPlan(final int missionModelId) throws SQLException {
    return insertPlan(missionModelId, "2020-1-1 00:00:00");
  }

  int insertPlan(final int missionModelId, final String start_time) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO plan (name, model_id, duration, start_time)
                  VALUES ('test-plan-%s', '%s', '0', '%s')
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString(), missionModelId, start_time)
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertActivity(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO activity_directive (type, plan_id, start_offset, arguments)
                  VALUES ('test-activity', '%s', '00:00:00', '{}')
                  RETURNING id;"""
                  .formatted(planId)
          );

      res.next();
      return res.getInt("id");
    }
  }

  int insertSimulationTemplate(final int modelId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO simulation_template (model_id, description, arguments)
                  VALUES ('%s', 'test-description', '{}')
                  RETURNING id;"""
                  .formatted(modelId)
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertSimulationWithTemplateId(final int simulationTemplateId, final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO simulation (simulation_template_id, plan_id, arguments)
                  VALUES ('%s', '%s', '{}')
                  RETURNING id;"""
                  .formatted(simulationTemplateId, planId)
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertSimulationWithoutTemplateId(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO simulation (plan_id, arguments)
                  VALUES ('%s', '{}')
                  RETURNING id;"""
                  .formatted(planId)
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertDataset() throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO dataset
                  DEFAULT VALUES
                  RETURNING id;"""
          );
      res.next();
      return res.getInt("id");
    }
  }

  PlanDatasetRecord insertPlanDataset(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO plan_dataset (plan_id, offset_from_plan_start)
                  VALUES ('%s', '0')
                  RETURNING plan_id, dataset_id;"""
                  .formatted(planId)
          );
      res.next();
      return new PlanDatasetRecord(res.getInt("plan_id"), res.getInt("dataset_id"));
    }
  }

  SimulationDatasetRecord insertSimulationDataset(final int simulationId, final int datasetId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO simulation_dataset (simulation_id, dataset_id, offset_from_plan_start)
                  VALUES ('%s', '%s', '0')
                  RETURNING simulation_id, dataset_id;"""
                  .formatted(simulationId, datasetId)
          );
      res.next();
      return new SimulationDatasetRecord(res.getInt("simulation_id"), res.getInt("dataset_id"));
    }
  }

  void clearTable(String table) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate("TRUNCATE " + table + " CASCADE;");
    }
  }

  int fileId;
  int missionModelId;
  int planId;
  int activityId;
  int simulationTemplateId;
  int simulationWithTemplateId;
  int simulationWithoutTemplateId;
  int datasetId;
  SimulationDatasetRecord simulationDatasetRecord;
  PlanDatasetRecord planDatasetRecord;

  @BeforeEach
  void beforeEach() throws SQLException {
    fileId = insertFileUpload();
    missionModelId = insertMissionModel(fileId);
    planId = insertPlan(missionModelId);
    activityId = insertActivity(planId);
    simulationTemplateId = insertSimulationTemplate(missionModelId);
    simulationWithTemplateId = insertSimulationWithTemplateId(simulationTemplateId, planId);
    simulationWithoutTemplateId = insertSimulationWithoutTemplateId(planId);
    planDatasetRecord = insertPlanDataset(planId);
    datasetId = insertDataset();
    simulationDatasetRecord = insertSimulationDataset(simulationWithTemplateId, datasetId);
  }

  @AfterEach
  void afterEach() throws SQLException {
    clearTable("uploaded_file");
    clearTable("mission_model");
    clearTable("plan");
    clearTable("activity_directive");
    clearTable("simulation_template");
    clearTable("simulation");
    clearTable("dataset");
    clearTable("plan_dataset");
    clearTable("simulation_dataset");
  }

  @Nested
  class MissionModelTriggers {
    @Test
    void shouldIncrementMissionModelRevisionOnMissionModelUpdate() throws SQLException {
      final var res = connection.createStatement()
                                .executeQuery(
                                    """
                                        SELECT revision
                                        FROM mission_model
                                        WHERE id = %s;"""
                                        .formatted(missionModelId)
                                );
      res.next();
      final var revision = res.getInt("revision");
      res.close();

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE mission_model
                        SET name = 'updated-name-%s'
                        WHERE id = %s;"""
                        .formatted(UUID.randomUUID().toString(), missionModelId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision
                                               FROM mission_model
                                               WHERE id = %s;"""
                                               .formatted(missionModelId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");
      updatedRes.close();

      assertEquals(revision + 1, updatedRevision);
    }

    @Test
    void shouldIncrementMissionModelRevisionOnMissionModelJarIdUpdate() throws SQLException {
      final var res = connection.createStatement()
                                .executeQuery(
                                    """
                                        SELECT revision
                                        FROM mission_model
                                        WHERE id = %s;"""
                                        .formatted(missionModelId)
                                );
      res.next();
      final var revision = res.getInt("revision");
      res.close();

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE uploaded_file
                        SET path = 'test-path-updated'
                        WHERE id = %s;"""
                        .formatted(fileId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision
                                               FROM mission_model
                                               WHERE id = %s;"""
                                               .formatted(missionModelId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");
      updatedRes.close();

      assertEquals(revision + 1, updatedRevision);
    }
  }

  @Nested
  class PlanTriggers {
    @Test
    void shouldIncrementPlanRevisionOnPlanUpdate() throws SQLException {
      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");
      initialRes.close();

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE plan SET name = 'test-plan-updated-%s'
                        WHERE id = %s;"""
                        .formatted(UUID.randomUUID().toString(), planId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");
      updatedRes.close();

      assertEquals(initialRevision + 1, updatedRevision);
    }

    @Test
    void shouldIncrementPlanRevisionOnActivityInsert() throws SQLException {
      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");
      initialRes.close();

      insertActivity(planId);

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");
      updatedRes.close();

      assertEquals(initialRevision + 1, updatedRevision);
    }

    @Test
    void shouldIncrementPlanRevisionOnActivityUpdate() throws SQLException {

      final var activityId = insertActivity(planId);

      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE activity_directive SET type = 'test-activity-updated'
                        WHERE id = %s;"""
                        .formatted(activityId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");
      updatedRes.close();

      assertEquals(initialRevision + 1, updatedRevision);
    }

    @Test
    void shouldIncrementPlanRevisionOnActivityDelete() throws SQLException {

      final var activityId = insertActivity(planId);

      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");
      initialRes.close();

      connection.createStatement()
                .executeUpdate(
                    """
                        DELETE FROM activity_directive
                        WHERE id = %s;"""
                        .formatted(activityId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM plan
                                               WHERE id = %s;"""
                                               .formatted(planId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");
      updatedRes.close();

      assertEquals(initialRevision + 1, updatedRevision);
    }
  }

  @Nested
  class SimulationTemplateTriggers {
    @Test
    void shouldIncrementSimulationTemplateRevisionOnSimulationTemplateUpdate() throws SQLException {

      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation_template
                                               WHERE id = %s;"""
                                               .formatted(simulationTemplateId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");
      initialRes.close();

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE simulation_template
                        SET description = 'test-description-updated'
                        WHERE id = %s;"""
                        .formatted(simulationTemplateId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation_template
                                               WHERE id = %s;"""
                                               .formatted(simulationTemplateId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");
      updatedRes.close();

      assertEquals(initialRevision + 1, updatedRevision);
    }
  }

  @Nested
  class SimulationTriggers {
    @Test
    void shouldIncrementSimulationRevisionOnSimulationUpdate() throws SQLException {

      final var initialRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation
                                               WHERE id = %s;"""
                                               .formatted(simulationWithTemplateId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");
      initialRes.close();

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE simulation SET arguments = '{}'
                        WHERE id = %s;"""
                        .formatted(simulationWithTemplateId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation
                                               WHERE id = %s;"""
                                               .formatted(simulationWithTemplateId)
                                       );
      updatedRes.next();
      final var updatedRevision = updatedRes.getInt("revision");
      updatedRes.close();

      assertEquals(initialRevision + 1, updatedRevision);
    }
  }

  @Nested
  class PlanDatasetTriggers {
    @Test
    void shouldCreateDefaultDatasetOnPlanDatasetInsertWithNullDatasetId() throws SQLException {
      final var res = connection.createStatement()
          .executeQuery(
              """
                  INSERT INTO plan_dataset (plan_id, offset_from_plan_start)
                  VALUES (%s, '0')
                  RETURNING dataset_id;"""
                  .formatted(planId)
          );
      res.next();
      final var newDatasetId = res.getInt("dataset_id");
      assertFalse(res.wasNull());
      res.close();


      final var datasetRes = connection.createStatement()
          .executeQuery(
              """
                  SELECT * FROM dataset
                  WHERE id = %s;"""
                  .formatted(newDatasetId)
          );

      datasetRes.next();
      assertEquals(newDatasetId, datasetRes.getInt("id"));
      assertEquals(0, datasetRes.getInt("revision"));
      datasetRes.close();
    }

    @Test
    void shouldCalculatePlanDatasetOffsetOnPlanDatasetInsertWithNonNullDatasetId() throws SQLException {

      final var planRes = connection.createStatement()
          .executeQuery(
              """
                  SELECT * from plan
                  WHERE id = %s;"""
                  .formatted(planDatasetRecord.plan_id())
          );
      planRes.next();
      final var planStartTime = planRes.getTimestamp("start_time");
      planRes.close();

      final var planDatasetSelectRes = connection.createStatement()
          .executeQuery(
              """
                  SELECT * FROM plan_dataset
                  WHERE plan_id = %s and dataset_id = %s;"""
                  .formatted(planDatasetRecord.plan_id(), planDatasetRecord.dataset_id())
          );
      planDatasetSelectRes.next();
      final var offsetFromPlanStart = Duration.parse(planDatasetSelectRes.getString("offset_from_plan_start"));
      planDatasetSelectRes.close();

      final var newPlanId = insertPlan(missionModelId, "2020-1-1 01:00:00");

      final var newPlanRes = connection.createStatement()
          .executeQuery(
              """
                  SELECT * from plan
                  WHERE id = %s;"""
                  .formatted(newPlanId)
          );
      newPlanRes.next();
      final var newPlanStartTime = newPlanRes.getTimestamp("start_time");
      newPlanRes.close();

      final var planDatasetInsertRes = connection.createStatement()
          .executeQuery(
              """
                  INSERT INTO plan_dataset (plan_id, dataset_id)
                  VALUES (%s, %s)
                  RETURNING *;"""
                  .formatted(newPlanId, planDatasetRecord.dataset_id())
          );
      planDatasetInsertRes.next();
      final var newOffsetFromPlanStart = Duration.parse(planDatasetInsertRes.getString("offset_from_plan_start"));
      planDatasetInsertRes.close();

      final var calculatedOffset = offsetFromPlanStart.minus(Duration.ofMillis(newPlanStartTime.getTime() - planStartTime.getTime()));

      assertEquals(calculatedOffset, newOffsetFromPlanStart);
    }

    @Test
    void shouldDeleteDatasetWithNoAssociatedPlansOnPlanDatasetDelete() throws SQLException {
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery(
            """
                SELECT COUNT(*) FROM dataset
                WHERE id = %s;"""
                .formatted(planDatasetRecord.dataset_id())
        );
        res.next();
        assertEquals(1, res.getInt(1));
      }
      try (final var statement = connection.createStatement()) {
        statement.executeUpdate(
            """
                DELETE FROM plan_dataset
                WHERE plan_id = %s and dataset_id = %s;"""
                .formatted(planDatasetRecord.plan_id(), planDatasetRecord.dataset_id())
        );
      }
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery(
            """
                SELECT COUNT(*) FROM dataset
                WHERE id = %s;"""
                .formatted(planDatasetRecord.dataset_id())
        );
        res.next();
        assertEquals(0, res.getInt(1));
      }
    }
  }

  @Nested
  class SimulationDatasetTriggers {
    @Test
    void shouldInitializeDatasetOnInsertWithTemplate() throws SQLException {
      try (final var statement = connection.createStatement()) {
        try (final var res = statement.executeQuery(
            """
                SELECT plan_revision, model_revision, simulation_revision, simulation_template_revision
                FROM simulation_dataset
                WHERE simulation_id = %s AND dataset_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id(), simulationDatasetRecord.dataset_id())
        )) {
          res.next();
          assertEquals(1, res.getInt("plan_revision"));
          assertEquals(0, res.getInt("model_revision"));
          assertEquals(0, res.getInt("simulation_revision"));
          assertEquals(0, res.getInt("simulation_template_revision"));
        }
      }
    }

    @Test
    void shouldInitializeDatasetOnInsertWithoutTemplate() throws SQLException {
      try (final var statement = connection.createStatement()) {
        final var simulationDatasetWithoutTemplateId = insertSimulationDataset(simulationWithoutTemplateId, datasetId);
        try (final var res = statement.executeQuery(
            """
                SELECT plan_revision, model_revision, simulation_revision, simulation_template_revision
                FROM simulation_dataset
                WHERE simulation_id = %s AND dataset_id = %s;"""
                .formatted(simulationDatasetWithoutTemplateId.simulation_id(), simulationDatasetWithoutTemplateId.dataset_id())
        )) {
          res.next();
          assertEquals(1, res.getInt("plan_revision"));
          assertEquals(0, res.getInt("model_revision"));
          assertEquals(0, res.getInt("simulation_revision"));
          res.getInt("simulation_template_revision");
          assertTrue(res.wasNull());
        }
      }
    }

    @Test
    void shouldDeleteDatasetOnSimulationDatasetDelete() throws SQLException {
      try (final var statement = connection.createStatement()) {
        statement.executeUpdate(
            """
                DELETE FROM simulation_dataset
                WHERE simulation_id = %s AND dataset_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id(), simulationDatasetRecord.dataset_id())
        );
        try (final var res = statement.executeQuery(
            """
                SELECT COUNT(*)
                FROM dataset
                WHERE id = %s;"""
                .formatted(simulationDatasetRecord.dataset_id())
        )) {
          res.next();
          assertEquals(0, res.getInt("count"));
        }
      }
    }

    @Test
    void shouldCancelSimulationOnMissionModelUpdate() throws SQLException {
      try (final var statement = connection.createStatement()) {
        try (final var res = statement.executeQuery(
            """
                SELECT canceled
                FROM simulation_dataset
                WHERE simulation_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id())
        )) {
          res.next();
          assertFalse(res.getBoolean("canceled"));
        }

        statement
            .executeUpdate(
                """
                    UPDATE mission_model
                    SET name = 'updated-name-%s'
                    WHERE id = %s;"""
                    .formatted(UUID.randomUUID().toString(), missionModelId)
            );

        try (final var res = statement.executeQuery(
            """
                SELECT canceled
                FROM simulation_dataset
                WHERE simulation_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id())
        )) {
          res.next();
          assertTrue(res.getBoolean("canceled"));
        }
      }
    }

    @Test
    void shouldCancelSimulationOnPlanUpdate() throws SQLException {
      try (final var statement = connection.createStatement()) {
        try (final var res = statement.executeQuery(
            """
                SELECT canceled
                FROM simulation_dataset
                WHERE simulation_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id())
        )) {
          res.next();
          assertFalse(res.getBoolean("canceled"));
        }

        statement
            .executeUpdate(
                """
                    UPDATE plan
                    SET name = 'test-plan-updated-%s'
                    WHERE id = %s;"""
                    .formatted(UUID.randomUUID().toString(), planId)
            );

        try (final var res = statement.executeQuery(
            """
                SELECT canceled
                FROM simulation_dataset
                WHERE simulation_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id())
        )) {
          res.next();
          assertTrue(res.getBoolean("canceled"));
        }
      }
    }

    @Test
    void shouldCancelSimulationOnSimulationUpdate() throws SQLException {
      try (final var statement = connection.createStatement()) {
        try (final var res = statement.executeQuery(
            """
                SELECT canceled
                FROM simulation_dataset
                WHERE simulation_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id())
        )) {
          res.next();
          assertFalse(res.getBoolean("canceled"));
        }

        statement
            .executeUpdate(
                """
                    UPDATE simulation
                    SET arguments = '{}'
                    WHERE id = %s;"""
                    .formatted(simulationWithTemplateId)
            );

        try (final var res = statement.executeQuery(
            """
                SELECT canceled
                FROM simulation_dataset
                WHERE simulation_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id())
        )) {
          res.next();
          assertTrue(res.getBoolean("canceled"));
        }
      }
    }

    @Test
    void shouldCancelSimulationOnSimulationTemplateUpdate() throws SQLException {
      try (final var statement = connection.createStatement()) {
        try (final var res = statement.executeQuery(
            """
                SELECT canceled
                FROM simulation_dataset
                WHERE simulation_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id())
        )) {
          res.next();
          assertFalse(res.getBoolean("canceled"));
        }

        statement
            .executeUpdate(
                """
                    UPDATE simulation_template
                    SET description = 'test-description-updated'
                    WHERE id = %s;"""
                    .formatted(simulationTemplateId)
            );

        try (final var res = statement.executeQuery(
            """
                SELECT canceled
                FROM simulation_dataset
                WHERE simulation_id = %s;"""
                .formatted(simulationDatasetRecord.simulation_id())
        )) {
          res.next();
          assertTrue(res.getBoolean("canceled"));
        }
      }
    }
  }
}
