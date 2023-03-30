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
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

record SimulationDatasetRecord(int simulation_id, int dataset_id){}
record PlanDatasetRecord(int plan_id, int dataset_id) {}
record ProfileSegmentAtATimeRecord(int datasetId, int profileId, String name, String type, String startOffset, String dynamics, boolean isGap) {}


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
    helper.startDatabase();
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

  int getSimulationId(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
              """
                  SELECT id
                  FROM simulation
                  WHERE simulation.plan_id = '%s';
              """.formatted(planId)
          );
      res.next();
      return res.getInt("id");
    }
  }

   void addTemplateIdToSimulation(final int simulationTemplateId, final int simulationId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate(
          """
              UPDATE simulation
              SET simulation_template_id = '%s',
                  arguments = '{}'
              WHERE id = '%s';
          """.formatted(simulationTemplateId, simulationId));
    }
  }

  int getDatasetId(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  SELECT dataset_id from plan_dataset
                  WHERE plan_id = '%s'""".formatted(planId)
          );
      res.next();
      return res.getInt("dataset_id");
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
                  INSERT INTO simulation_dataset (simulation_id, dataset_id, arguments, simulation_start_time, simulation_end_time)
                  VALUES ('%s', '%s', '{}', '2020-1-1 00:00:00', '2020-1-2 00:00:00')
                  RETURNING simulation_id, dataset_id;"""
                  .formatted(simulationId, datasetId)
          );
      res.next();
      return new SimulationDatasetRecord(res.getInt("simulation_id"), res.getInt("dataset_id"));
    }
  }

  int fileId;
  int missionModelId;
  int planId;
  int activityId;
  int simulationTemplateId;
  int simulationId;
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
    simulationId = getSimulationId(planId);
    addTemplateIdToSimulation(simulationTemplateId, simulationId);
    planDatasetRecord = insertPlanDataset(planId);
    datasetId = getDatasetId(planId);
    simulationDatasetRecord = insertSimulationDataset(simulationId, datasetId);
  }

  @AfterEach
  void afterEach() throws SQLException {
    helper.clearTable("uploaded_file");
    helper.clearTable("mission_model");
    helper.clearTable("plan");
    helper.clearTable("activity_directive");
    helper.clearTable("simulation_template");
    helper.clearTable("simulation");
    helper.clearTable("dataset");
    helper.clearTable("plan_dataset");
    helper.clearTable("simulation_dataset");
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
                                               .formatted(simulationId)
                                       );
      initialRes.next();
      final var initialRevision = initialRes.getInt("revision");
      initialRes.close();

      connection.createStatement()
                .executeUpdate(
                    """
                        UPDATE simulation SET arguments = '{}'
                        WHERE id = %s;"""
                        .formatted(simulationId)
                );

      final var updatedRes = connection.createStatement()
                                       .executeQuery(
                                           """
                                               SELECT revision FROM simulation
                                               WHERE id = %s;"""
                                               .formatted(simulationId)
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
    void shouldInitializeDatasetOnInsert() throws SQLException {
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
          assertEquals(1, res.getInt("simulation_revision")); //1, as we add a template in the BeforeEach
          assertEquals(0, res.getInt("simulation_template_revision"));
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
                    .formatted(simulationId)
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

  @Test
  void shouldRejectInsertProfileSegmentWithNonExistentProfile() throws SQLException {
    final var datasetId = allocateDataset();
    try {
      insertProfileSegment(datasetId, 1256);
      fail();
    } catch (SQLException e) {
      if (!e.getMessage().contains("foreign key violation: there is no profile with id 1256 in dataset %d".formatted(
          datasetId))) {
        throw e;
      }
    }
  }

  @Test
  void shouldRejectInsertEventWithNonExistentTopic() throws SQLException {
    final var datasetId = allocateDataset();
    try (final var statement = connection.createStatement()) {
      statement
          .executeUpdate(
              """
                  INSERT INTO event (dataset_id, real_time, transaction_index, causal_time, value, topic_index)
                  VALUES (%d, '0 seconds', 0, '.1', '{}', 1234);
                  """.formatted(datasetId)
          );
      fail();
    } catch (SQLException e) {
      if (!e.getMessage().contains("foreign key violation: there is no topic with topic_index 1234 in dataset %d".formatted(datasetId))) {
        throw e;
      }
    }
  }

  @Test
  void shouldRejectUpdateProfileSegmentToNonExistentProfile() throws SQLException {
    final var datasetId = allocateDataset();
    final int profileId = insertProfile(datasetId);

    insertProfileSegment(datasetId, profileId);

    try (final var statement = connection.createStatement()) {
      statement
          .executeUpdate(
              """
                  UPDATE profile_segment
                   set profile_id=%d
                   where dataset_id=%d and profile_id=%d;
                  """.formatted(profileId + 1, datasetId, profileId)
          );
      fail();
    } catch (SQLException e) {
      if (!e.getMessage().contains("foreign key violation: there is no profile with id %d in dataset %d".formatted(profileId + 1, datasetId))) {
        throw e;
      }
    }

  }

  @Test
  void shouldRejectUpdateEventToNonExistentTopic() throws SQLException {
    final var datasetId = allocateDataset();
    final var topicIndex = 42;

    insertTopic(datasetId, topicIndex);
    insertEvent(datasetId, topicIndex);

    try (final var statement = connection.createStatement()) {
      statement.executeUpdate(
              """
              UPDATE event
              set topic_index=%d
              where dataset_id=%d and topic_index=%d;
              """.formatted(topicIndex + 1, datasetId, topicIndex)
          );
      fail();
    } catch (SQLException e) {
      if (!e.getMessage().contains("foreign key violation: there is no topic with topic_index %d in dataset %d".formatted(topicIndex + 1, datasetId))) {
        throw e;
      }
    }
  }

  @Test
  void shouldCascadeWhenDeletingProfile() throws SQLException {
    final var datasetId = allocateDataset();
    final int profileId = insertProfile(datasetId);
    insertProfileSegment(datasetId, profileId);

    try (final var statement = connection.createStatement()) {
      statement
          .executeUpdate(
              """
              DELETE FROM profile
              WHERE dataset_id=%d and id=%d
              """.formatted(datasetId, profileId)
          );
    }

    assertEquals(0, getProfileSegmentCount(datasetId, profileId));
  }

  @Test
  void shouldCascadeWhenDeletingTopic() throws SQLException {
    final var datasetId = allocateDataset();
    final var topicIndex = 42;

    insertTopic(datasetId, topicIndex);
    insertEvent(datasetId, topicIndex);

    try (final var statement = connection.createStatement()) {
      statement
          .executeUpdate(
              """
              DELETE FROM topic
              WHERE dataset_id=%d and topic_index=%d
              """.formatted(datasetId, topicIndex)
          );

      try (final var res = statement.executeQuery(
          """
          SELECT count(1) FROM event WHERE dataset_id=%d and topic_index=%d
          """.formatted(datasetId, topicIndex)
      )) {
        res.next();
        assertEquals(0, res.getInt("count"));
      }
    }
  }

  @Test
  void shouldCascadeWhenUpdatingProfile() throws SQLException {
    final var datasetId = allocateDataset();
    final int profileId = insertProfile(datasetId);
    insertProfileSegment(datasetId, profileId);

    final int newProfileId;
    try (final var statement = connection.createStatement();
         final var res = statement.executeQuery(
              """
              UPDATE profile
              SET id=default
              WHERE dataset_id=%d and id=%d
              RETURNING id;
              """.formatted(datasetId, profileId)
          )
      ) {
        res.next();
        newProfileId = res.getInt("id");
      }

    assertNotEquals(profileId, newProfileId);
    assertEquals(1, getProfileSegmentCount(datasetId, newProfileId));
  }

  @Test
  void shouldCascadeWhenUpdatingTopicIndex() throws SQLException {
    final var datasetId = allocateDataset();
    final var topicIndex = 42;

    insertTopic(datasetId, topicIndex);
    insertEvent(datasetId, topicIndex);

    try (final var statement = connection.createStatement()) {
      statement
          .executeUpdate(
              """
              UPDATE topic
              SET topic_index=%d
              WHERE dataset_id=%d and topic_index=%d
              """.formatted(topicIndex + 1, datasetId, topicIndex)
          );

      try (final var res = statement.executeQuery(
          """
          SELECT count(1) FROM event WHERE dataset_id=%d and topic_index=%d
          """.formatted(datasetId, topicIndex + 1)
      )) {
        res.next();
        assertEquals(1, res.getInt("count"));
      }
    }
  }

  @Test
  void getResourcesAtTimeFetchesLatestData() throws SQLException {
    final var datasetId = allocateDataset();

    final var contestantName = "contestantCount";
    final var contestantType = "{\"type\": \"discrete\", \"schema\": {\"type\": \"int\"}}";

    final var winnerName = "winner";
    final var winnerType = "{\"type\": \"discrete\", \"schema\": {\"type\": \"string\"}}";

    final var contestantCountId = insertProfile(datasetId, contestantName, contestantType, "12 hours");
    final var winnerId = insertProfile(datasetId, winnerName, winnerType,  "12 hours");

    // Contestant Count Segments:
    insertProfileSegment(datasetId, contestantCountId, "0 seconds", "20", false);
    insertProfileSegment(datasetId, contestantCountId, "2 hours", "12", false);
    insertProfileSegment(datasetId, contestantCountId, "12 hours", "1", false);

    // Winner Segments:
    insertProfileSegment(datasetId, winnerId, "6 hours", "\"Bob or Alice\"", false);
    insertProfileSegment(datasetId, winnerId, "10 hours", "\"Alice\"", false);

    final var segmentsAtStart = getResourcesAtStartOffset(datasetId, "00:00:00");
    final var segmentsAtOneHour = getResourcesAtStartOffset(datasetId, "06:00:00");
    final var segmentsAtTwelveHours = getResourcesAtStartOffset(datasetId, "12:00:00");

    assertEquals(1, segmentsAtStart.size());
    assertEquals(2, segmentsAtOneHour.size());
    assertEquals(2, segmentsAtTwelveHours.size());

    final var atStartSegment0 = new ProfileSegmentAtATimeRecord(datasetId, contestantCountId, contestantName, contestantType, "PT0S", "20", false);
    assertEquals(atStartSegment0, segmentsAtStart.get(0));

    final var atOneSegment0 = new ProfileSegmentAtATimeRecord(datasetId, contestantCountId, contestantName, contestantType, "PT2H", "12", false);
    final var atOneSegment1 = new ProfileSegmentAtATimeRecord(datasetId, winnerId, winnerName, winnerType, "PT6H", "\"Bob or Alice\"", false);

    assertEquals(atOneSegment0, segmentsAtOneHour.get(0));
    assertEquals(atOneSegment1, segmentsAtOneHour.get(1));

    final var atTwelveSegment0 = new ProfileSegmentAtATimeRecord(datasetId, contestantCountId, contestantName, contestantType, "PT12H", "1", false);
    final var atTwelveSegment1 = new ProfileSegmentAtATimeRecord(datasetId, winnerId, winnerName, winnerType, "PT10H", "\"Alice\"", false);

    assertEquals(atTwelveSegment0, segmentsAtTwelveHours.get(0));
    assertEquals(atTwelveSegment1, segmentsAtTwelveHours.get(1));
  }

  private int insertProfile(final int datasetId) throws SQLException {
    return insertProfile(datasetId, "fred", "{}", "0 seconds");
  }

  private int insertProfile(final int datasetId, final String name, final String type, final String duration) throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var results = statement.executeQuery(
        """
        INSERT INTO profile(dataset_id, name, type, duration)
        VALUES (%d, '%s', '%s', '%s')
        RETURNING id;
        """.formatted(datasetId, name, type, duration));
        assertTrue(results.first());
        return results.getInt("id");
    }
  }

  private void insertProfileSegment(final int datasetId, final int profileId) throws SQLException {
    insertProfileSegment(datasetId, profileId, "0 seconds", "{}", false);
  }

  private void insertProfileSegment(final int datasetId, final int profileId, final String startOffset, final String dynamics, final boolean isGap) throws SQLException {
    try(final var statement = connection.createStatement()) {
      statement.execute(
        """
        INSERT INTO profile_segment(dataset_id, profile_id, start_offset, dynamics, is_gap)
        VALUES (%d, %d, '%s'::interval, '%s'::jsonb, %b);
        """.formatted(datasetId, profileId, startOffset, dynamics, isGap));
    }
  }

  private int getProfileSegmentCount(final int datasetId, final int profileId) throws SQLException {
    try (final Statement statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          """
              SELECT count(1) FROM profile_segment WHERE dataset_id=%d and profile_id=%d
              """.formatted(datasetId, profileId));
      assertTrue(res.first());
      return res.getInt("count");
    }
  }

  private void insertTopic(final int datasetId, final int topicIndex) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement
          .executeUpdate(
              """
                  INSERT INTO topic (dataset_id, topic_index, name, value_schema)
                  VALUES (%d, %d, 'fred', '{}');
                  """.formatted(datasetId, topicIndex));
    }
  }

  private void insertEvent(final int datasetId, final int topicIndex) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement
          .executeUpdate(
              """
                  INSERT INTO event (dataset_id, real_time, transaction_index, causal_time, value, topic_index)
                  VALUES (%d, '0 seconds', 0, '.1', '{}', %d);
                  """.formatted(datasetId, topicIndex)
          );
    }
  }

  private ArrayList<ProfileSegmentAtATimeRecord> getResourcesAtStartOffset(final int datasetId, final String startOffset) throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        select * from hasura_functions.get_resources_at_start_offset(%d, '%s');
        """.formatted(datasetId, startOffset));

      final var segments = new ArrayList<ProfileSegmentAtATimeRecord>();
      while (res.next()){
        segments.add(new ProfileSegmentAtATimeRecord(
            res.getInt("dataset_id"),
            res.getInt("id"),
            res.getString("name"),
            res.getString("type"),
            res.getString("start_offset"),
            res.getString("dynamics"),
            res.getBoolean("is_gap")
        ));
      }
      return segments;
    }
  }

  private int allocateDataset() throws SQLException {
    final int datasetId;
    try (final Statement statement = connection.createStatement()) {
      try (final var res = statement.executeQuery(
          """
            INSERT INTO dataset
            DEFAULT VALUES
            RETURNING id;
            """)) {
        res.next();
        datasetId = res.getInt("id");
      }

      statement
        .executeUpdate(
            """
            SELECT FROM allocate_dataset_partitions(%d)
            """.formatted(datasetId)
        );
      return datasetId;
    }
  }
}
