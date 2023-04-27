package gov.nasa.jpl.aerie.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PresetTests {
  private static final File initSqlScriptFile = new File("../merlin-server/sql/merlin/init.sql");
  private DatabaseTestHelper helper;

  private Connection connection;
  int fileId;
  int missionModelId;

  void setConnection(DatabaseTestHelper helper) {
    connection = helper.connection();
  }

  @BeforeEach
  void beforeEach() throws SQLException {
    fileId = insertFileUpload();
    missionModelId = insertMissionModel(fileId);
    // Insert the "test-activity" activity types to avoid a foreign key conflict
    insertActivityType(missionModelId, "test-activity");
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
    helper.clearTable("plan_snapshot");
    helper.clearTable("plan_latest_snapshot");
    helper.clearTable("plan_snapshot_activities");
    helper.clearTable("plan_snapshot_parent");
    helper.clearTable("anchor_validation_status");
    helper.clearTable("activity_presets");
    helper.clearTable("preset_to_directive");
    helper.clearTable("preset_to_snapshot_directive");
  }

  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {
    helper =
        new DatabaseTestHelper("aerie_merlin_test", "Merlin Database Tests", initSqlScriptFile);
    helper.startDatabase();
    setConnection(helper);
  }

  @AfterAll
  void afterAll() throws SQLException, IOException, InterruptedException {
    helper.stopDatabase();
    connection = null;
    helper = null;
  }

  // region Helper Methods from MerlinDatabaseTests
  int insertFileUpload() throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res =
          statement.executeQuery(
              """
                  INSERT INTO uploaded_file (path, name)
                  VALUES ('test-path', 'test-name-%s')
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString()));
      res.next();
      return res.getInt("id");
    }
  }

  int insertMissionModel(final int fileId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res =
          statement.executeQuery(
              """
                  INSERT INTO mission_model (name, mission, owner, version, jar_id)
                  VALUES ('test-mission-model-%s', 'test-mission', 'tester', '0', %s)
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString(), fileId));
      res.next();
      return res.getInt("id");
    }
  }

  int insertPlan(final int missionModelId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res =
          statement.executeQuery(
              """
                  INSERT INTO plan (name, model_id, duration, start_time)
                  VALUES ('test-plan-%s', '%s', '0', '%s')
                  RETURNING id;"""
                  .formatted(UUID.randomUUID().toString(), missionModelId, "2020-1-1 00:00:00"));
      res.next();
      return res.getInt("id");
    }
  }

  int insertActivity(final int planId) throws SQLException {
    return insertActivity(planId, "{}");
  }

  int insertActivity(final int planId, final String arguments) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res =
          statement.executeQuery(
              """
                  INSERT INTO activity_directive (type, plan_id, start_offset, arguments)
                  VALUES ('test-activity', '%s', '00:00:00', '%s')
                  RETURNING id;"""
                  .formatted(planId, arguments));

      res.next();
      return res.getInt("id");
    }
  }

  void insertActivityType(final int modelId, final String name) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
          INSERT INTO activity_type (model_id, name, parameters, required_parameters, computed_attributes_value_schema)
          VALUES (%d, '%s', '{}', '[]', '{}');
          """
              .formatted(modelId, name));
    }
  }
  // endregion

  // region Helper Methods
  Activity assignPreset(int presetId, int activityId, int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
         select hasura_functions.apply_preset_to_activity(%d, %d, %d);
         """
              .formatted(presetId, activityId, planId));
      return getActivity(planId, activityId);
    }
  }

  Activity getActivity(final int planId, final int activityId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res =
          statement.executeQuery(
              """
        SELECT *
        FROM activity_directive
        WHERE id = %d
        AND plan_id = %d;
      """
                  .formatted(activityId, planId));
      res.first();
      return new Activity(
          res.getInt("id"),
          res.getInt("plan_id"),
          res.getString("name"),
          res.getString("type"),
          res.getString("arguments"));
    }
  }

  ArrayList<Activity> getActivities(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res =
          statement.executeQuery(
              """
        SELECT *
        FROM activity_directive
        WHERE plan_id = %d
        ORDER BY id;
      """
                  .formatted(planId));

      final var activities = new ArrayList<Activity>();
      while (res.next()) {
        activities.add(
            new Activity(
                res.getInt("id"),
                res.getInt("plan_id"),
                res.getString("name"),
                res.getString("type"),
                res.getString("arguments")));
      }
      return activities;
    }
  }

  static void assertActivityEqualsAsideFromArgs(final Activity expected, final Activity actual) {
    assertEquals(expected.activityId, actual.activityId);
    assertEquals(expected.planId, actual.planId);
    assertEquals(expected.name, actual.name);
    assertEquals(expected.type, actual.type);
  }

  int insertPreset(int modelId, String name, String associatedActivityType) throws SQLException {
    return insertPreset(modelId, name, associatedActivityType, "{}");
  }

  int insertPreset(int modelId, String name, String associatedActivityType, String arguments)
      throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res =
          statement.executeQuery(
              """
                  INSERT INTO activity_presets (model_id, name, associated_activity_type, arguments)
                  VALUES (%d, '%s', '%s', '%s')
                  RETURNING id;"""
                  .formatted(modelId, name, associatedActivityType, arguments));
      res.next();
      return res.getInt("id");
    }
  }

  void deletePreset(int presetId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
        DELETE FROM activity_presets
        WHERE id = %d
        """
              .formatted(presetId));
    }
  }

  ArrayList<Activity> getActivitiesWithPreset(final int presetId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      // Select from act dirs using the list of ids gotten from the join table preset to dirs
      final var res =
          statement.executeQuery(
              """
        SELECT ad.id, ad.plan_id, ad.name, ad.type, ad.arguments
        FROM activity_directive ad,
            (SELECT activity_id, plan_id
             FROM preset_to_directive
             WHERE preset_id = %d) presets
        WHERE (ad.id, ad.plan_id) = (presets.activity_id, presets.plan_id);
      """
                  .formatted(presetId));

      final var activities = new ArrayList<Activity>();
      while (res.next()) {
        activities.add(
            new Activity(
                res.getInt("id"),
                res.getInt("plan_id"),
                res.getString("name"),
                res.getString("type"),
                res.getString("arguments")));
      }
      return activities;
    }
  }

  Preset getPresetAssignedToActivity(final int activityId, final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res =
          statement.executeQuery(
              """
      SELECT ap.id, ap.model_id, ap.name, ap.associated_activity_type, ap.arguments
      FROM activity_presets ap, (SELECT preset_id from preset_to_directive WHERE (activity_id, plan_id) = (%d, %d)) o
      WHERE ap.id = o.preset_id;
      """
                  .formatted(activityId, planId));
      return res.first()
          ? new Preset(
              res.getInt("id"),
              res.getInt("model_id"),
              res.getString("name"),
              res.getString("associated_activity_type"),
              res.getString("arguments"))
          : null;
    }
  }
  // endregion

  // region Records
  record Activity(int activityId, int planId, String name, String type, String arguments) {}

  record Preset(
      int id, int modelId, String name, String associatedActivityType, String arguments) {}
  // endregion

  @Test
  void presetAppliesCorrectly() throws SQLException {
    final int planId = insertPlan(missionModelId);
    final String activityArgs = "{\"fruitCount\": 40}";
    final int activityId = insertActivity(planId, activityArgs);
    final String simplePresetArgs = "{\"fruitCount\": 80}";
    final int simplePresetId =
        insertPreset(missionModelId, "simple preset", "test-activity", simplePresetArgs);
    final String extendedPresetArgs = "{\"coreCount\": 120, \"destination\": \"Mars\"}";
    final int extendedPresetId =
        insertPreset(missionModelId, "extended preset", "test-activity", extendedPresetArgs);

    var simplePresetActivities = getActivitiesWithPreset(simplePresetId);
    var extendedPresetActivities = getActivitiesWithPreset(extendedPresetId);
    assertTrue(simplePresetActivities.isEmpty());
    assertTrue(extendedPresetActivities.isEmpty());

    // Simple Update
    final Activity originalActivity = getActivity(planId, activityId);
    final Activity simpleActivity = assignPreset(simplePresetId, activityId, planId);
    assertEquals(1, getActivities(planId).size());
    assertActivityEqualsAsideFromArgs(originalActivity, simpleActivity);
    assertNotEquals(originalActivity.arguments, simpleActivity.arguments);
    assertEquals(simplePresetArgs, simpleActivity.arguments);

    simplePresetActivities = getActivitiesWithPreset(simplePresetId);
    extendedPresetActivities = getActivitiesWithPreset(extendedPresetId);
    assertEquals(1, simplePresetActivities.size());
    assertEquals(simpleActivity, simplePresetActivities.get(0));
    assertTrue(extendedPresetActivities.isEmpty());

    // Extended Update
    final Activity extendedActivity = assignPreset(extendedPresetId, activityId, planId);
    assertEquals(1, getActivities(planId).size());
    assertActivityEqualsAsideFromArgs(originalActivity, extendedActivity);
    assertNotEquals(originalActivity.arguments, extendedActivity.arguments);
    assertEquals(extendedPresetArgs, extendedActivity.arguments);

    simplePresetActivities = getActivitiesWithPreset(simplePresetId);
    extendedPresetActivities = getActivitiesWithPreset(extendedPresetId);
    assertTrue(simplePresetActivities.isEmpty());
    assertEquals(1, extendedPresetActivities.size());
    assertEquals(extendedActivity, extendedPresetActivities.get(0));
  }

  @Test
  void cannotApplyPresetOfIncorrectType() throws SQLException {
    // Insert 'fake-type' to avoid an FK conflict
    insertActivityType(missionModelId, "fake-type");
    final int planId = insertPlan(missionModelId);
    final int activityId = insertActivity(planId);
    final int presetId = insertPreset(missionModelId, "test preset", "fake-type");

    final Activity activity = getActivity(planId, activityId);
    assertEquals("test-activity", activity.type);

    try {
      assignPreset(presetId, activityId, planId);
      fail();
    } catch (SQLException ex) {
      if (!ex.getMessage()
          .contains(
              "Cannot apply preset for activity type \"fake-type\" onto an activity of type"
                  + " \"test-activity\".")) {
        throw ex;
      }
    }
  }

  @Test
  void cannotApplyNonexistentPreset() throws SQLException {
    final int planId = insertPlan(missionModelId);
    final int activityId = insertActivity(planId);

    try {
      assignPreset(-1, activityId, planId);
      fail();
    } catch (SQLException ex) {
      if (!ex.getMessage().contains("Activity preset -1 does not exist")) {
        throw ex;
      }
    }
  }

  @Test
  void cannotApplyPresetToNonexistentActivity() throws SQLException {
    final int planId = insertPlan(missionModelId);
    final int presetId = insertPreset(missionModelId, "test preset", "test-activity");

    try {
      assignPreset(presetId, -1, planId);
      fail();
    } catch (SQLException ex) {
      if (!ex.getMessage().contains("Activity directive -1 does not exist in plan " + planId)) {
        throw ex;
      }
    }
  }

  // Tests that two presets with the same name can be uploaded as long as the model or associated
  // activity type differs
  // and that two presets applied to the same activity type and model but with different names can
  // be uploaded
  // but that two presets with the same name, model, and associated activity type cannot be uploaded
  @Test
  void presetUniquenessConstraint() throws SQLException {
    final int otherModelId = insertMissionModel(fileId);
    // Insert the used activity types to avoid an FK conflict
    insertActivityType(missionModelId, "Shared Type");
    insertActivityType(missionModelId, "Different Type");
    insertActivityType(otherModelId, "Shared Type");
    insertActivityType(otherModelId, "Unique Type");

    insertPreset(missionModelId, "Shared Name", "Shared Type");
    insertPreset(missionModelId, "Shared Name", "Different Type");
    insertPreset(otherModelId, "Shared Name", "Shared Type");
    insertPreset(missionModelId, "Different Name", "Shared Type");
    insertPreset(otherModelId, "Unique Name", "Unique Type");
    try {
      insertPreset(missionModelId, "Shared Name", "Shared Type");
      fail();
    } catch (SQLException ex) {
      if (!ex.getMessage()
          .contains(
              "duplicate key value violates unique constraint"
                  + " \"activity_presets_model_id_associated_activity_type_name_key\"")) {
        throw ex;
      }
    }
  }
}
