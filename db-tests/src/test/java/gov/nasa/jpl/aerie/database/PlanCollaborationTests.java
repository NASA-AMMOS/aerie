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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PlanCollaborationTests {
  private static final File initSqlScriptFile = new File("../merlin-server/sql/merlin/init.sql");
  private DatabaseTestHelper helper;

  private Connection connection;
  int fileId;
  int missionModelId;
  int planId;
  int activityId;
  int simulationTemplateId;
  int simulationWithTemplateId;
  int simulationWithoutTemplateId;
  int datasetId;
  gov.nasa.jpl.aerie.database.SimulationDatasetRecord simulationDatasetRecord;
  gov.nasa.jpl.aerie.database.PlanDatasetRecord planDatasetRecord;

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
    clearTable("plan_snapshot");
    clearTable("plan_latest_snapshot");
    clearTable("plan_snapshot_activities");
    clearTable("plan_snapshot_parent");
  }

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

  //region Helper Methods from MerlinDatabaseTests
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
    return insertActivity(planId, "00:00:00");
  }

  int insertActivity(final int planId, final String startOffset) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              """
                  INSERT INTO activity_directive (type, plan_id, start_offset, arguments)
                  VALUES ('test-activity', '%s', '%s', '{}')
                  RETURNING id;"""
                  .formatted(planId, startOffset)
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

  gov.nasa.jpl.aerie.database.PlanDatasetRecord insertPlanDataset(final int planId) throws SQLException {
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
      return new gov.nasa.jpl.aerie.database.PlanDatasetRecord(res.getInt("plan_id"), res.getInt("dataset_id"));
    }
  }

  gov.nasa.jpl.aerie.database.SimulationDatasetRecord insertSimulationDataset(final int simulationId, final int datasetId) throws SQLException {
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
      return new gov.nasa.jpl.aerie.database.SimulationDatasetRecord(
          res.getInt("simulation_id"),
          res.getInt("dataset_id"));
    }
  }

  void clearTable(String table) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate("TRUNCATE " + table + " CASCADE;");
    }
  }
  //endregion

  //region Helper Methods
  private boolean updateActivityName(String newName, int activityId, int planId) throws SQLException {
    try(final var statement = connection.createStatement()) {
      return statement.execute(
          """
          update activity_directive
          set name = '%s'
          where id = %d and plan_id = %d;
          """.formatted(newName, activityId, planId));
    }
  }

  int duplicatePlan(final int planId, final String newPlanName) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        select duplicate_plan(%s, '%s') as id;
      """.formatted(planId, newPlanName));
      res.next();
      return res.getInt("id");
    }
  }

  int createSnapshot(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
                                                   select create_snapshot(%s) as id;
                                                   """.formatted(planId));
      res.next();
      return res.getInt("id");
    }
  }

  int getParentPlanId(final int planId) throws SQLException{
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
                                                   select parent_id
                                                   from plan
                                                   where plan.id = %d;
                                                   """.formatted(planId));
      res.next();
      return res.getInt("parent_id");
    }
  }

  private void lockPlan(final int planId) throws SQLException{
    try(final var statement = connection.createStatement()){
      statement.execute("""
          update plan
          set is_locked = true
          where id = %d;
          """.formatted(planId));
    }
  }

  private void unlockPlan(final int planId) throws SQLException{
    //Unlock first to allow for after tasks
    try(final var statement = connection.createStatement()){
      statement.execute("""
          update plan
          set is_locked = false
          where id = %d;
          """.formatted(planId));
    }
  }

  int getMergeBaseFromPlanIds(final int planIdReceivingChanges, final int planIdSupplyingChanges) throws SQLException{
    try(final var statement = connection.createStatement()){
      final var snapshotRes = statement.executeQuery(
          """
              select snapshot_id
              from plan_latest_snapshot
              where plan_id = %d
              order by snapshot_id desc
              limit 1;
              """.formatted(planIdSupplyingChanges));
      snapshotRes.first();
      final int snapshotIdSupplyingChanges = snapshotRes.getInt(1);

      final var res = statement.executeQuery(
          """
              select get_merge_base(%d, %d);
              """.formatted(planIdReceivingChanges, snapshotIdSupplyingChanges));

      res.first();

      return res.getInt(1);
    }

  }

  private int createMergeRequest(final int planId_receiving, final int planId_supplying) throws SQLException{
    try(final var statement = connection.createStatement()){
      final var res = statement.executeQuery(
          """
              select create_merge_request(%d, %d, 'PlanCollaborationTests Requester');
              """.formatted(planId_supplying, planId_receiving)
      );
      res.first();
      return res.getInt(1);
    }
  }

  private void beginMerge(final int mergeRequestId) throws SQLException{
    try(final var statement = connection.createStatement()){
      statement.execute(
          """
          call begin_merge(%d, 'PlanCollaborationTests Reviewer')
          """.formatted(mergeRequestId)
      );
    }
  }

  private void deleteActivityDirective(final int planId, final int activityId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate("""
        delete from activity_directive where id = %s and plan_id = %s
      """.formatted(activityId, planId));
    }
  }

  private ArrayList<Activity> getActivities(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        SELECT *
        FROM activity_directive
        WHERE plan_id = %d
        ORDER BY id;
      """.formatted(planId));

      final var activities = new ArrayList<Activity>();
      while (res.next()){
        activities.add(new Activity(
            res.getInt("id"),
            res.getInt("plan_id"),
            res.getString("name"),
            (String[]) res.getArray("tags").getArray(),
            res.getInt("source_scheduling_goal_id"),
            res.getString("created_at"),
            res.getString("last_modified_at"),
            res.getString("start_offset"),
            res.getString("type"),
            res.getString("arguments"),
            res.getString("last_modified_arguments_at"),
            res.getString("metadata")
            ));
      }
      return activities;
    }
  }

  private ArrayList<SnapshotActivity> getSnapshotActivities(final int snapshotId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        SELECT *
        FROM plan_snapshot_activities
        WHERE snapshot_id = %d
        ORDER BY id;
      """.formatted(snapshotId));

      final var activities = new ArrayList<SnapshotActivity>();
      while (res.next()){
        activities.add(new SnapshotActivity(
            res.getInt("id"),
            res.getInt("snapshot_id"),
            res.getString("name"),
            (String[]) res.getArray("tags").getArray(),
            res.getInt("source_scheduling_goal_id"),
            res.getString("created_at"),
            res.getString("last_modified_at"),
            res.getString("start_offset"),
            res.getString("type"),
            res.getString("arguments"),
            res.getString("last_modified_arguments_at"),
            res.getString("metadata")
        ));
      }
      return activities;
    }
  }
  //endregion

  //region Records
  private record Activity(
      int activityId,
      int planId,
      String name,
      String[] tags,
      int sourceSchedulingGoalId,
      String createdAt,
      String lastModifiedAt,
      String startOffset,
      String type,
      String arguments,
      String lastModifiedArgumentsAt,
      String metadata
  ) {}
  private record SnapshotActivity(
      int activityId,
      int snapshotId,
      String name,
      String[] tags,
      int sourceSchedulingGoalId,
      String createdAt,
      String lastModifiedAt,
      String startOffset,
      String type,
      String arguments,
      String lastModifiedArgumentsAt,
      String metadata
  ) {}
  //endregion

  @Nested
  class PlanSnapshotTests{
    @Test
    void snapshotCapturesAllActivities() throws SQLException {
      final var planId = insertPlan(missionModelId);
      final int activityCount = 200;
      final var activityIds = new HashSet<>();
      for (var i = 0; i < activityCount; i++) {
        activityIds.add(insertActivity(planId));
      }
      final var planActivities = getActivities(planId);

      final var snapshotId = createSnapshot(planId);
      final var snapshotActivities = getSnapshotActivities(snapshotId);

      //assert the correct number of activities were copied
      assertFalse(planActivities.isEmpty());
      assertFalse(snapshotActivities.isEmpty());
      assertEquals(planActivities.size(), snapshotActivities.size());
      assertEquals(activityCount, planActivities.size());

      for (int i = 0; i < activityCount; ++i) {
        //assert that this activity exists
        assertTrue(activityIds.contains(planActivities.get(i).activityId));
        assertTrue(activityIds.contains(snapshotActivities.get(i).activityId));
        // validate all shared properties
        assertEquals(planActivities.get(i).activityId, snapshotActivities.get(i).activityId);
        assertEquals(planActivities.get(i).name, snapshotActivities.get(i).name);

        assertEquals(planActivities.get(i).sourceSchedulingGoalId, snapshotActivities.get(i).sourceSchedulingGoalId);
        assertEquals(planActivities.get(i).createdAt, snapshotActivities.get(i).createdAt);
        assertEquals(planActivities.get(i).lastModifiedAt, snapshotActivities.get(i).lastModifiedAt);
        assertEquals(planActivities.get(i).startOffset, snapshotActivities.get(i).startOffset);
        assertEquals(planActivities.get(i).type, snapshotActivities.get(i).type);
        assertEquals(planActivities.get(i).arguments, snapshotActivities.get(i).arguments);
        assertEquals(planActivities.get(i).lastModifiedArgumentsAt, snapshotActivities.get(i).lastModifiedArgumentsAt);
        assertEquals(planActivities.get(i).metadata, snapshotActivities.get(i).metadata);

        assertEquals(planActivities.get(i).tags.length, snapshotActivities.get(i).tags.length);
        for(int j = 0; j < planActivities.get(i).tags.length; ++j)
        {
          assertEquals(planActivities.get(i).tags[j], snapshotActivities.get(i).tags[j]);
        }

        activityIds.remove(planActivities.get(i).activityId);
      }
      assert activityIds.isEmpty();
    }

    @Test
    void snapshotInheritsAllLatestAsParents() throws SQLException{
      final int planId = insertPlan(missionModelId);

      //take n snapshots, then insert them all into the latest table
      final int numberOfSnapshots = 4;
      final int[] snapshotIds = new int[numberOfSnapshots];
      for(int i = 0; i < numberOfSnapshots; ++i){
        snapshotIds[i] = createSnapshot(planId);
      }

      try(final var statement = connection.createStatement()) {
        //assert that there is exactly one entry for this plan in plan_latest_snapshot
        var res = statement.executeQuery(
            """
            select snapshot_id from plan_latest_snapshot where plan_id = %d;
            """.formatted(planId));
        assertTrue(res.first());
        assertTrue(res.isLast());

        //delete the current entry of plan_latest_snapshot for this plan to avoid any confusion when it is readded below
        statement.execute("""
                                delete from plan_latest_snapshot where plan_id = %d;
                                """.formatted(planId));

        for (final int snapshotId : snapshotIds) {
          statement.execute("""
                                insert into plan_latest_snapshot(plan_id, snapshot_id) VALUES (%d, %d);
                                """.formatted(planId, snapshotId));
        }

        final int finalSnapshotId = createSnapshot(planId);

        //assert that there is now only one entry for this plan in plan_latest_snapshot
        res = statement.executeQuery(
            """
            select snapshot_id from plan_latest_snapshot where plan_id = %d;
            """.formatted(planId));
        assertTrue(res.first());
        assertTrue(res.isLast());

        //assert that the snapshot history is n+1 long
        res = statement.executeQuery(
            """
            select get_snapshot_history(%d);
            """.formatted(finalSnapshotId));
        assertTrue(res.last());
        assertEquals(res.getRow(), numberOfSnapshots+1);

        //assert that res contains, in order: finalSnapshotId, snapshotId[0,1,...,n]
        res.first();
        assertEquals(res.getInt(1), finalSnapshotId);
        for (final int snapshotId : snapshotIds) {
          res.next();
          assertEquals(res.getInt(1), snapshotId);
        }
      }
    }

    @Test
    void snapshotFailsForNonexistentPlanId() throws SQLException{
      try {
        createSnapshot(1000);
        fail();
      }
      catch(SQLException sqlEx)
      {
        if(!sqlEx.getMessage().contains("Plan 1000 does not exist."))
          throw sqlEx;
      }
    }
  }

  @Nested
  class DuplicatePlanTests{
    @Test
    void duplicateCapturesAllActivities() throws SQLException {
      /*
      TODO when we implement copying affiliated data
          (scheduling spec, constraints, command expansions...?),
          check that affiliated data has been copied as well
      */
      final var planId = insertPlan(missionModelId);
      final int activityCount = 200;
      final var activityIds = new HashSet<>();
      for (var i = 0; i < activityCount; i++) {
        activityIds.add(insertActivity(planId));
      }

      final var planActivities = getActivities(planId);

      final var childPlan = duplicatePlan(planId, "My new duplicated plan");
      final var childActivities = getActivities(childPlan);
      //assert the correct number of activities were copied
      assertFalse(planActivities.isEmpty());
      assertFalse(childActivities.isEmpty());
      assertEquals(planActivities.size(), childActivities.size());
      assertEquals(activityCount, planActivities.size());

      for (int i = 0; i < activityCount; ++i) {
        //assert that this activity exists
        assertTrue(activityIds.contains(planActivities.get(i).activityId));
        assertTrue(activityIds.contains(childActivities.get(i).activityId));
        // validate all shared properties
        assertEquals(planActivities.get(i).activityId, childActivities.get(i).activityId);
        assertEquals(planActivities.get(i).name, childActivities.get(i).name);

        assertEquals(planActivities.get(i).sourceSchedulingGoalId, childActivities.get(i).sourceSchedulingGoalId);
        assertEquals(planActivities.get(i).createdAt, childActivities.get(i).createdAt);
        assertEquals(planActivities.get(i).lastModifiedAt, childActivities.get(i).lastModifiedAt);
        assertEquals(planActivities.get(i).startOffset, childActivities.get(i).startOffset);
        assertEquals(planActivities.get(i).type, childActivities.get(i).type);
        assertEquals(planActivities.get(i).arguments, childActivities.get(i).arguments);
        assertEquals(planActivities.get(i).lastModifiedArgumentsAt, childActivities.get(i).lastModifiedArgumentsAt);
        assertEquals(planActivities.get(i).metadata, childActivities.get(i).metadata);

        assertEquals(planActivities.get(i).tags.length, childActivities.get(i).tags.length);
        for(int j = 0; j < planActivities.get(i).tags.length; ++j)
        {
          assertEquals(planActivities.get(i).tags[j], childActivities.get(i).tags[j]);
        }

        activityIds.remove(planActivities.get(i).activityId);
      }
      assert activityIds.isEmpty();
    }

    @Test
    void duplicateSetsLatestSnapshot() throws SQLException{
      final int parentPlanId = insertPlan(missionModelId);
      final int parentOldSnapshot = createSnapshot(parentPlanId);
      final int childPlanId = duplicatePlan(parentPlanId, "Child Plan");

      try(final var statement = connection.createStatement()){
        var res = statement.executeQuery("""
                                                     select snapshot_id from plan_latest_snapshot
                                                     where plan_id = %s;
                                                   """.formatted(parentPlanId));
        assertTrue(res.next());
        final int parentLatestSnapshot = res.getInt(1);
        assertFalse(res.next()); // Should only be 1 latest snapshot
        res = statement.executeQuery("""
                                                     select snapshot_id from plan_latest_snapshot
                                                     where plan_id = %s;
                                                   """.formatted(childPlanId));
        assertTrue(res.next());
        final int childLatestSnapshot = res.getInt(1);
        assertFalse(res.next());
        assertEquals(childLatestSnapshot, parentLatestSnapshot);
        assertNotEquals(parentOldSnapshot, parentLatestSnapshot);
      }
    }

    @Test
    void duplicateAttachesParentHistoryToChild() throws SQLException{
      final int parentPlanId = insertPlan(missionModelId);
      final int numberOfSnapshots = 4;
      for(int i = 0; i < numberOfSnapshots; ++i){
        createSnapshot(parentPlanId);
      }
      final int childPlanId = duplicatePlan(parentPlanId, "Snapshot Inheritance Test");

      try(final var statementParent = connection.createStatement();
          final var statementChild = connection.createStatement()) {
        final var parentRes = statementParent.executeQuery(
            """
                select get_snapshot_history_from_plan(%d);
            """.formatted(parentPlanId));
        final var childRes = statementChild.executeQuery(
            """
                select get_snapshot_history_from_plan(%d);
            """.formatted(childPlanId));

        parentRes.last();
        childRes.last();
        assertEquals(parentRes.getRow(), childRes.getRow()); //assert the history length is the same
        assertEquals(numberOfSnapshots+1, parentRes.getRow()); //assert the history is the length expected

        //assert the history is the same
        parentRes.first();
        childRes.first();
        do{
          assertEquals(parentRes.getInt(1), childRes.getInt(1));
          childRes.next();
          parentRes.next();
        }while(!parentRes.isAfterLast());
      }
    }

    @Test
    void duplicateNonexistentPlanFails() throws SQLException {
      try {
        duplicatePlan(1000, "Nonexistent Parent Duplicate");
        fail();
      }
      catch(SQLException sqlEx)
      {
        if(!sqlEx.getMessage().contains("Plan 1000 does not exist."))
          throw sqlEx;
      }
    }

  }

  @Nested
  class PlanHistoryTests {
    @Test
    void getPlanHistoryCapturesAllAncestors() throws SQLException {
      final int[] plans = new int[10];
      plans[0] = insertPlan(missionModelId);
      for(int i = 1; i < plans.length; ++i){
        plans[i] = duplicatePlan(plans[i-1], "Child of "+(i-1));
      }

      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("""
          SELECT get_plan_history(%d);
          """.formatted(plans[9])
        );
        assertTrue(res.first());
        assertEquals(plans[9], res.getInt(1));

        for(int i = plans.length-2; i >= 0; --i){
          assertTrue(res.next());
          assertEquals(plans[i], res.getInt(1));
        }
      }
    }

    @Test
    void getPlanHistoryNoAncestors() throws SQLException {
      final int planId = insertPlan(missionModelId);

      //The history of a plan with no ancestors is itself.
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("""
          SELECT get_plan_history(%d);
          """.formatted(planId)
        );
        assertTrue(res.first());
        assertTrue(res.isLast());
        assertEquals(planId, res.getInt(1));
      }
    }

    @Test
    void getPlanHistoryInvalidId() throws SQLException {
      try (final var statement = connection.createStatement()) {
        statement.execute("""
          SELECT get_plan_history(-1);
          """
        );
        fail();
      }
      catch (SQLException sqlException) {
        if (!sqlException.getMessage().contains("Plan ID -1 is not present in plan table."))
          throw sqlException;
      }
    }

    @Test
    void grandparentAdoptsChildrenOnDelete() throws SQLException{
      final int grandparentPlan = insertPlan(missionModelId);
      final int parentPlan = duplicatePlan(grandparentPlan, "Parent Plan");
      final int sibling1 = duplicatePlan(parentPlan, "Older Sibling");
      final int sibling2 = duplicatePlan(parentPlan, "Younger Sibling");
      final int childOfSibling1 = duplicatePlan(sibling1, "Child of Older Sibling");
      final int unrelatedPlan = insertPlan(missionModelId);
      final int childOfUnrelatedPlan = duplicatePlan(unrelatedPlan, "Child of Related Plan");

      // Assert that parentage starts as expected
      assertEquals(0, getParentPlanId(grandparentPlan)); // When the value is null, res.getInt returns 0
      assertEquals(grandparentPlan, getParentPlanId(parentPlan));
      assertEquals(parentPlan, getParentPlanId(sibling1));
      assertEquals(parentPlan, getParentPlanId(sibling2));
      assertEquals(sibling1, getParentPlanId(childOfSibling1));
      assertEquals(0, getParentPlanId(unrelatedPlan));
      assertEquals(unrelatedPlan, getParentPlanId(childOfUnrelatedPlan));

      // Delete Parent Plan
      try(final var statement = connection.createStatement()){
        statement.execute("""
          delete from plan
          where id = %d;
          """.formatted(parentPlan));
      }

      // Assert that sibling1 and sibling2 now have grandparentPlan set as their parent
      assertEquals(0, getParentPlanId(grandparentPlan));
      assertEquals(grandparentPlan, getParentPlanId(sibling1));
      assertEquals(grandparentPlan, getParentPlanId(sibling2));
      assertEquals(sibling1, getParentPlanId(childOfSibling1));
      assertEquals(0, getParentPlanId(unrelatedPlan));
      assertEquals(unrelatedPlan, getParentPlanId(childOfUnrelatedPlan));
    }
  }

  @Nested
  class LockedPlanTests {
    @Test
    void updateActivityShouldFailOnLockedPlan() throws SQLException {
      final int planId = insertPlan(missionModelId);
      final int activityId = insertActivity(planId);
      final String newName = "Test :-)";
      final String oldName = "oldName";

      updateActivityName(oldName, activityId, planId);

      try {
        lockPlan(planId);
        updateActivityName(newName, activityId, planId);
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Plan " + planId + " is locked."))
          throw sqlEx;
      } finally {
        unlockPlan(planId);
      }

      //Assert that there is one activity and it is the one that was added earlier.
      final var activitiesBefore = getActivities(planId);
      assertEquals(1, activitiesBefore.size());
      assertEquals(activityId, activitiesBefore.get(0).activityId);
      assertEquals(oldName, activitiesBefore.get(0).name);

      updateActivityName(newName, activityId, planId);
      final var activitiesAfter = getActivities(planId);
      assertEquals(1, activitiesAfter.size());
      assertEquals(activityId, activitiesAfter.get(0).activityId);
      assertEquals(newName, activitiesAfter.get(0).name);
    }

    @Test
    void deleteActivityShouldFailOnLockedPlan() throws SQLException {
      final var planId = insertPlan(missionModelId);
      final var activityId = insertActivity(planId);

      try {
        lockPlan(planId);
        deleteActivityDirective(planId, activityId);
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Plan " + planId + " is locked."))
          throw sqlEx;
      } finally {
        unlockPlan(planId);
      }

      try (final var statement = connection.createStatement()) {
        //Assert that there is one activity and it is the one that was added earlier.
        final var activitiesBefore = getActivities(planId);
        assertEquals(1, activitiesBefore.size());
        assertEquals(activityId, activitiesBefore.get(0).activityId);

        deleteActivityDirective(planId, activityId);
        final var activitiesAfter = getActivities(planId);
        assertTrue(activitiesAfter.isEmpty());
      }
    }

    @Test
    void insertActivityShouldFailOnLockedPlan() throws SQLException {
      final var planId = insertPlan(missionModelId);

      try {
        lockPlan(planId);
        insertActivity(planId);
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Plan " + planId + " is locked."))
          throw sqlEx;
      } finally {
        unlockPlan(planId);
      }

      try (final var statement = connection.createStatement()) {
        //Assert that there are no activities for this plan.
        final var activitiesBefore = getActivities(planId);
        assertTrue(activitiesBefore.isEmpty());

        final int insertedId = insertActivity(planId);

        final var activitiesAfter = getActivities(planId);
        assertEquals(1, activitiesAfter.size());
        assertEquals(insertedId, activitiesAfter.get(0).activityId);
      }
    }

    @Test
    void beginReviewFailsOnLockedPlan() throws SQLException {
      final var planId = insertPlan(missionModelId);
      insertActivity(planId);
      final var childPlanId = duplicatePlan(planId, "Child Plan");

      final int mergeRequest = createMergeRequest(planId, childPlanId);

      try {
        lockPlan(planId);
        beginMerge(mergeRequest);
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot begin merge request. Plan to receive changes is locked."))
          throw sqlEx;
      } finally {
        unlockPlan(planId);
      }
    }

    @Test
    void deletePlanFailsWhileLocked() throws SQLException {
      final var planId = insertPlan(missionModelId);

      try (final var statement = connection.createStatement()) {
        lockPlan(planId);
        statement.execute("""
        delete from plan
        where id = %d
        """.formatted(planId));
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot delete locked plan."))
          throw sqlEx;
      } finally {
        unlockPlan(planId);
      }
    }

    /**
     * Assert that locking one plan does not stop other plans from being updated
     */
    @Test
    void lockingPlanDoesNotAffectOtherPlans() throws SQLException {
      final int planId = insertPlan(missionModelId);
      final int activityId = insertActivity(planId);
      final int relatedPlanId = duplicatePlan(planId, "Child");

      final int unrelatedPlanId = insertPlan(missionModelId);
      final int unrelatedActivityId = insertActivity(unrelatedPlanId);


      try (final var statementRelated = connection.createStatement();
           final var statementUnrelated = connection.createStatement()
      ) {
        lockPlan(planId);

        //Update the activity in the unlocked plans
        final String newName = "Test";

        updateActivityName(newName, activityId, relatedPlanId);
        updateActivityName(newName, unrelatedActivityId, unrelatedPlanId);

        var relatedActivities = getActivities(relatedPlanId);
        var unrelatedActivities = getActivities(unrelatedPlanId);

        assertEquals(1, relatedActivities.size());
        assertEquals(1, unrelatedActivities.size());
        assertEquals(activityId, relatedActivities.get(0).activityId);
        assertEquals(unrelatedActivityId, unrelatedActivities.get(0).activityId);
        assertEquals(newName, relatedActivities.get(0).name);
        assertEquals(newName, unrelatedActivities.get(0).name);


        //Insert a new activity into the unlocked plans
        final int newActivityRelated = insertActivity(relatedPlanId);
        final int newActivityUnrelated = insertActivity(unrelatedPlanId);

        relatedActivities = getActivities(relatedPlanId);
        unrelatedActivities = getActivities(unrelatedPlanId);

        assertEquals(2, relatedActivities.size());
        assertEquals(2, unrelatedActivities.size());
        assertEquals(activityId, relatedActivities.get(0).activityId);
        assertEquals(unrelatedActivityId, unrelatedActivities.get(0).activityId);
        assertEquals(newActivityRelated, relatedActivities.get(1).activityId);
        assertEquals(newActivityUnrelated, unrelatedActivities.get(1).activityId);

        //Delete the first activity in the unlocked plans
        deleteActivityDirective(relatedPlanId, activityId);
        deleteActivityDirective(unrelatedPlanId, unrelatedActivityId);

        relatedActivities = getActivities(relatedPlanId);
        unrelatedActivities = getActivities(unrelatedPlanId);

        assertEquals(1, relatedActivities.size());
        assertEquals(1, unrelatedActivities.size());
        assertEquals(newActivityRelated, relatedActivities.get(0).activityId);
        assertEquals(newActivityUnrelated, unrelatedActivities.get(0).activityId);
      } finally {
        unlockPlan(planId);
      }

    }
  }
}

