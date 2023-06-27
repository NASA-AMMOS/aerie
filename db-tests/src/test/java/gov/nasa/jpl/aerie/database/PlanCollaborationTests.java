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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.nasa.jpl.aerie.database.TagsTests.Tag;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PlanCollaborationTests {
  private static final File initSqlScriptFile = new File("../merlin-server/sql/merlin/init.sql");
  private DatabaseTestHelper helper;
  private MerlinDatabaseTestHelper merlinHelper;

  private Connection connection;
  int fileId;
  int missionModelId;

  @BeforeEach
  void beforeEach() throws SQLException {
    fileId = merlinHelper.insertFileUpload();
    missionModelId = merlinHelper.insertMissionModel(fileId);
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
    helper.clearTable("merge_request");
    helper.clearTable("merge_staging_area");
    helper.clearTable("conflicting_activities");
    helper.clearTable("anchor_validation_status");
    helper.clearTable("activity_presets");
    helper.clearTable("preset_to_directive");
    helper.clearTable("preset_to_snapshot_directive");
    helper.clearTable("metadata.tags");
    helper.clearTable("metadata.activity_directive_tags");
    helper.clearTable("metadata.constraint_tags");
    helper.clearTable("metadata.snapshot_activity_tags");
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
    merlinHelper = new MerlinDatabaseTestHelper(connection);
  }

  @AfterAll
  void afterAll() throws SQLException, IOException, InterruptedException {
    helper.stopDatabase();
    connection = null;
    helper = null;
  }

  //region Helper Methods
  private void updateActivityName(String newName, int activityId, int planId) throws SQLException {
    try(final var statement = connection.createStatement()) {
      statement.execute(
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
        select duplicate_plan(%s, '%s', -1) as id;
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
      snapshotRes.next();
      final int snapshotIdSupplyingChanges = snapshotRes.getInt(1);

      final var res = statement.executeQuery(
          """
              select get_merge_base(%d, %d);
              """.formatted(planIdReceivingChanges, snapshotIdSupplyingChanges));

      res.next();

      return res.getInt(1);
    }

  }

  private int createMergeRequest(final int planId_receiving, final int planId_supplying) throws SQLException{
    try(final var statement = connection.createStatement()){
      final var res = statement.executeQuery(
          """
              select create_merge_request(%d, %d, -1);
              """.formatted(planId_supplying, planId_receiving)
      );
      res.next();
      return res.getInt(1);
    }
  }

  private void beginMerge(final int mergeRequestId) throws SQLException{
    try(final var statement = connection.createStatement()){
      statement.execute(
          """
          call begin_merge(%d, -2)
          """.formatted(mergeRequestId)
      );
    }
  }

  private void commitMerge(final int mergeRequestId) throws SQLException{
    try(final var statement = connection.createStatement()){
      statement.execute(
          """
          call commit_merge(%d)
          """.formatted(mergeRequestId)
      );
    }
  }

  private void withdrawMergeRequest(final int mergeRequestId) throws SQLException{
    try(final var statement = connection.createStatement()){
      statement.execute(
          """
          call withdraw_merge_request(%d)
          """.formatted(mergeRequestId)
      );
    }
  }

  private void denyMerge(final int mergeRequestId) throws SQLException{
    try(final var statement = connection.createStatement()){
      statement.execute(
          """
          call deny_merge(%d)
          """.formatted(mergeRequestId)
      );
    }
  }

  private void cancelMerge(final int mergeRequestId) throws SQLException{
    try(final var statement = connection.createStatement()){
      statement.execute(
          """
          call cancel_merge(%d)
          """.formatted(mergeRequestId)
      );
    }
  }

  private void setResolution(final int mergeRequestId, final int activityId, final String status) throws SQLException {
    try(final var statement = connection.createStatement()){
        statement.execute(
            """
            update conflicting_activities
            set resolution = '%s'::conflict_resolution
            where merge_request_id = %d and activity_id = %d;
            """.formatted(status, mergeRequestId, activityId)
        );
    }
  }

  ArrayList<ConflictingActivity> getConflictingActivities(final int mergeRequestId) throws SQLException {
    final var conflicts = new ArrayList<ConflictingActivity>();
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
                                                   select activity_id, change_type_supplying, change_type_receiving
                                                   from conflicting_activities
                                                   where merge_request_id = %s
                                                   order by activity_id asc;
                                                 """.formatted(mergeRequestId));
      while (res.next()) {
        conflicts.add(new ConflictingActivity(
            res.getInt("activity_id"),
            res.getString("change_type_supplying"),
            res.getString("change_type_receiving")
        ));
      }
    }
    return conflicts;
  }

  ArrayList<StagingAreaActivity> getStagingAreaActivities(final int mergeRequestId) throws SQLException{
    final var activities = new ArrayList<StagingAreaActivity>();
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
                                                   select activity_id, change_type
                                                   from merge_staging_area
                                                   where merge_request_id = %s
                                                   order by activity_id asc;
                                                 """.formatted(mergeRequestId));
      while (res.next()) {
        activities.add(new StagingAreaActivity(
            res.getInt("activity_id"),
            res.getString("change_type")
        ));
      }
    }
    return activities;
  }

  private void deleteActivityDirective(final int planId, final int activityId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate("""
        delete from activity_directive where id = %s and plan_id = %s
      """.formatted(activityId, planId));
    }
  }

  private Activity getActivity(final int planId, final int activityId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        SELECT *
        FROM activity_directive
        WHERE id = %d
        AND plan_id = %d;
      """.formatted(activityId, planId));
      res.next();
      return new Activity(
          res.getInt("id"),
          res.getInt("plan_id"),
          res.getString("name"),
          res.getInt("source_scheduling_goal_id"),
          res.getString("created_at"),
          res.getString("last_modified_at"),
          res.getString("start_offset"),
          res.getString("type"),
          res.getString("arguments"),
          res.getString("last_modified_arguments_at"),
          res.getString("metadata"),
          res.getString("anchor_id"),
          res.getBoolean("anchored_to_start")
      );
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
            res.getInt("source_scheduling_goal_id"),
            res.getString("created_at"),
            res.getString("last_modified_at"),
            res.getString("start_offset"),
            res.getString("type"),
            res.getString("arguments"),
            res.getString("last_modified_arguments_at"),
            res.getString("metadata"),
            res.getString("anchor_id"),
            res.getBoolean("anchored_to_start")
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
            res.getInt("source_scheduling_goal_id"),
            res.getString("created_at"),
            res.getString("last_modified_at"),
            res.getString("start_offset"),
            res.getString("type"),
            res.getString("arguments"),
            res.getString("last_modified_arguments_at"),
            res.getString("metadata"),
            res.getString("anchor_id"),
            res.getBoolean("anchored_to_start")
        ));
      }
      return activities;
    }
  }

  private MergeRequest getMergeRequest(final int requestId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        SELECT *
        FROM merge_request
        WHERE id = %d;
      """.formatted(requestId));
      res.next();
      return new MergeRequest(
          res.getInt("id"),
          res.getInt("plan_id_receiving_changes"),
          res.getInt("snapshot_id_supplying_changes"),
          res.getInt("merge_base_snapshot_id"),
          res.getString("status")
      );
    }
  }

  private void setMergeRequestStatus(final int requestId, final String newStatus) throws SQLException {
    try(final var statement = connection.createStatement()) {
      statement.execute(
          """
          UPDATE merge_request
          SET status = '%s'
          WHERE id = %d;
          """.formatted(newStatus, requestId)
      );
    }
  }

  private static void assertActivityEquals(final Activity expected, final Activity actual) {
    // validate all shared properties
    assertEquals(expected.name, actual.name);
    assertEquals(expected.sourceSchedulingGoalId, actual.sourceSchedulingGoalId);
    assertEquals(expected.createdAt, actual.createdAt);
    assertEquals(expected.startOffset, actual.startOffset);
    assertEquals(expected.type, actual.type);
    assertEquals(expected.arguments, actual.arguments);
    assertEquals(expected.metadata, actual.metadata);
    assertEquals(expected.anchorId, actual.anchorId);
    assertEquals(expected.anchoredToStart, actual.anchoredToStart);
  }
  //endregion

  //region Records
  private record Activity(
      int activityId,
      int planId,
      String name,
      int sourceSchedulingGoalId,
      String createdAt,
      String lastModifiedAt,
      String startOffset,
      String type,
      String arguments,
      String lastModifiedArgumentsAt,
      String metadata,
      String anchorId,  // Since anchor_id allows for null values, this is a String to avoid confusion over what the number means.
      boolean anchoredToStart
  ) {}
  private record SnapshotActivity(
      int activityId,
      int snapshotId,
      String name,
      int sourceSchedulingGoalId,
      String createdAt,
      String lastModifiedAt,
      String startOffset,
      String type,
      String arguments,
      String lastModifiedArgumentsAt,
      String metadata,
      String anchorId,  // Since anchor_id allows for null values, this is a String to avoid confusion over what the number means.
      boolean anchoredToStart
  ) {}
  record ConflictingActivity(int activityId, String changeTypeSupplying, String changeTypeReceiving) {}
  record StagingAreaActivity(int activityId, String changeType) {} //only relevant fields
  private record MergeRequest(int requestId, int receivingPlan, int supplyingSnapshot, int mergeBaseSnapshot, String status) {}
  //endregion

  @Nested
  class PlanSnapshotTests{
    @Test
    void snapshotCapturesAllActivities() throws SQLException {
      final var planId = merlinHelper.insertPlan(missionModelId);
      final int activityCount = 200;
      final var activityIds = new HashSet<>();
      for (var i = 0; i < activityCount; i++) {
        activityIds.add(merlinHelper.insertActivity(planId));
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
        assertEquals(planActivities.get(i).anchorId, snapshotActivities.get(i).anchorId);
        assertEquals(planActivities.get(i).anchoredToStart, snapshotActivities.get(i).anchoredToStart);

        activityIds.remove(planActivities.get(i).activityId);
      }
      assert activityIds.isEmpty();
    }

    @Test
    void snapshotInheritsAllLatestAsParents() throws SQLException{
      final int planId = merlinHelper.insertPlan(missionModelId);

      //take n snapshots, then insert them all into the latest table
      final int numberOfSnapshots = 4;
      final int[] snapshotIds = new int[numberOfSnapshots];
      for(int i = 0; i < numberOfSnapshots; ++i){
        snapshotIds[i] = createSnapshot(planId);
      }

      try(final var statement = connection.createStatement()) {
        {
          //assert that there is exactly one entry for this plan in plan_latest_snapshot
          final var res = statement.executeQuery(
              """
              select snapshot_id from plan_latest_snapshot where plan_id = %d;
              """.formatted(planId));
          assertTrue(res.next());
          assertFalse(res.next());
        }

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

        {
          //assert that there is now only one entry for this plan in plan_latest_snapshot
          final var res = statement.executeQuery(
            """
            select snapshot_id from plan_latest_snapshot where plan_id = %d;
            """.formatted(planId));
          assertTrue(res.next());
          assertFalse(res.next());
        }

        final var snapshotHistory = new ArrayList<Integer>();
        {
          final var res = statement.executeQuery(
              """
                  select get_snapshot_history(%d);
                  """.formatted(finalSnapshotId));

          while (res.next()) {
            snapshotHistory.add(res.getInt(1));
          }
        }

        //assert that the snapshot history is n+1 long
        assertEquals(snapshotHistory.size(), numberOfSnapshots + 1);

        //assert that res contains, in order: finalSnapshotId, snapshotId[0,1,...,n]
        assertEquals(finalSnapshotId, snapshotHistory.get(0));

        for (var i = 1; i < snapshotHistory.size(); i++) {
          assertEquals(snapshotIds[i - 1], snapshotHistory.get(i));
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
      final var planId = merlinHelper.insertPlan(missionModelId);
      final int activityCount = 200;
      final var activityIds = new HashSet<>();
      for (var i = 0; i < activityCount; i++) {
        activityIds.add(merlinHelper.insertActivity(planId));
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

        assertEquals(planActivities.get(i).anchorId, childActivities.get(i).anchorId);
        assertEquals(planActivities.get(i).anchoredToStart, childActivities.get(i).anchoredToStart);

        activityIds.remove(planActivities.get(i).activityId);
      }
      assert activityIds.isEmpty();
    }

    @Test
    void duplicateSetsLatestSnapshot() throws SQLException{
      final int parentPlanId = merlinHelper.insertPlan(missionModelId);
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
      final int parentPlanId = merlinHelper.insertPlan(missionModelId);
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

        final var parentHistory = new ArrayList<Integer>();
        while (parentRes.next()) {
          parentHistory.add(parentRes.getInt(1));
        }

        assertEquals(parentHistory.size(), numberOfSnapshots + 1);

        final var childHistory = new ArrayList<Integer>();
        while (childRes.next()) {
          childHistory.add(childRes.getInt(1));
        }

        assertEquals(parentHistory, childHistory);
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
      plans[0] = merlinHelper.insertPlan(missionModelId);
      for(int i = 1; i < plans.length; ++i){
        plans[i] = duplicatePlan(plans[i-1], "Child of "+(i-1));
      }

      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("""
          SELECT get_plan_history(%d);
          """.formatted(plans[9])
        );
        assertTrue(res.next());
        assertEquals(plans[9], res.getInt(1));

        for(int i = plans.length-2; i >= 0; --i){
          assertTrue(res.next());
          assertEquals(plans[i], res.getInt(1));
        }
      }
    }

    @Test
    void getPlanHistoryNoAncestors() throws SQLException {
      final int planId = merlinHelper.insertPlan(missionModelId);

      //The history of a plan with no ancestors is itself.
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery("""
          SELECT get_plan_history(%d);
          """.formatted(planId)
        );
        assertTrue(res.next());
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
      final int grandparentPlan = merlinHelper.insertPlan(missionModelId);
      final int parentPlan = duplicatePlan(grandparentPlan, "Parent Plan");
      final int sibling1 = duplicatePlan(parentPlan, "Older Sibling");
      final int sibling2 = duplicatePlan(parentPlan, "Younger Sibling");
      final int childOfSibling1 = duplicatePlan(sibling1, "Child of Older Sibling");
      final int unrelatedPlan = merlinHelper.insertPlan(missionModelId);
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
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(planId);
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
      final var planId = merlinHelper.insertPlan(missionModelId);
      final var activityId = merlinHelper.insertActivity(planId);

      try {
        lockPlan(planId);
        deleteActivityDirective(planId, activityId);
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

      deleteActivityDirective(planId, activityId);
      final var activitiesAfter = getActivities(planId);
      assertTrue(activitiesAfter.isEmpty());
    }

    @Test
    void insertActivityShouldFailOnLockedPlan() throws SQLException {
      final var planId = merlinHelper.insertPlan(missionModelId);

      try {
        lockPlan(planId);
        merlinHelper.insertActivity(planId);
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Plan " + planId + " is locked."))
          throw sqlEx;
      } finally {
        unlockPlan(planId);
      }

      //Assert that there are no activities for this plan.
      final var activitiesBefore = getActivities(planId);
      assertTrue(activitiesBefore.isEmpty());

      final int insertedId = merlinHelper.insertActivity(planId);

      final var activitiesAfter = getActivities(planId);
      assertEquals(1, activitiesAfter.size());
      assertEquals(insertedId, activitiesAfter.get(0).activityId);
    }

    @Test
    void beginReviewFailsOnLockedPlan() throws SQLException {
      final var planId = merlinHelper.insertPlan(missionModelId);
      merlinHelper.insertActivity(planId);
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
      final var planId = merlinHelper.insertPlan(missionModelId);

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
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(planId);
      final int relatedPlanId = duplicatePlan(planId, "Child");

      final int unrelatedPlanId = merlinHelper.insertPlan(missionModelId);
      final int unrelatedActivityId = merlinHelper.insertActivity(unrelatedPlanId);

      try {
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
        final int newActivityRelated = merlinHelper.insertActivity(relatedPlanId);
        final int newActivityUnrelated = merlinHelper.insertActivity(unrelatedPlanId);

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

  @Nested
  class MergeBaseTests{
    /**
     * The MB between a plan and itself is its latest snapshot.
     * Create additional snapshots between creation and MB to verify this.
     */
    @Test
    void mergeBaseBetweenSelf() throws SQLException {
      final int parentPlanId = merlinHelper.insertPlan(missionModelId);
      final int planId = duplicatePlan(parentPlanId, "New Plan");

      createSnapshot(planId);
      createSnapshot(planId);
      final int mostRecentSnapshotId = createSnapshot(planId);

      try(final var statement = connection.createStatement()){
        final var results = statement.executeQuery(
            """
            SELECT snapshot_id
            FROM plan_latest_snapshot
            WHERE plan_id = %d;
            """.formatted(planId)
        );
        assertTrue(results.next());
        assertEquals(mostRecentSnapshotId, results.getInt(1));
      }
    }

    /**
     * The MB between a plan and its child (and a child and its parent) is the creation snapshot for the child.
     * Create additional snapshots between creation and MB to verify this.
     */
    @Test
    void mergeBaseParentChild() throws SQLException {
      final int parentPlanId = merlinHelper.insertPlan(missionModelId);
      final int childPlanId = duplicatePlan(parentPlanId, "New Plan");
      final int childCreationSnapshotId;

      try(final var statement = connection.createStatement()){
        final var results = statement.executeQuery(
            """
            SELECT snapshot_id
            FROM plan_latest_snapshot
            WHERE plan_id = %d;
            """.formatted(childPlanId)
        );
        assertTrue(results.next());
        childCreationSnapshotId = results.getInt(1);
      }

      createSnapshot(childPlanId);
      createSnapshot(childPlanId);

      final int mergeBaseParentChild = getMergeBaseFromPlanIds(parentPlanId, childPlanId);
      final int mergeBaseChildParent = getMergeBaseFromPlanIds(childPlanId, parentPlanId);

      assertEquals(mergeBaseParentChild, mergeBaseChildParent);
      assertEquals(childCreationSnapshotId, mergeBaseParentChild);
    }

    /**
     * The MB between two sibling plans is the creation snapshot for the older sibling
     */
    @Test
    void mergeBaseSiblings() throws SQLException {
      final int parentPlan = merlinHelper.insertPlan(missionModelId);
      final int olderSibling = duplicatePlan(parentPlan, "Older");
      final int olderSibCreationId;

      try(final var statement = connection.createStatement()){
        final var results = statement.executeQuery(
            """
            SELECT snapshot_id
            FROM plan_latest_snapshot
            WHERE plan_id = %d;
            """.formatted(olderSibling)
        );
        assertTrue(results.next());
        olderSibCreationId = results.getInt(1);
      }

      final int youngerSibling = duplicatePlan(parentPlan, "Younger");

      final int mbOlderYounger = getMergeBaseFromPlanIds(olderSibling,youngerSibling);
      final int mbYoungerOlder = getMergeBaseFromPlanIds(youngerSibling, olderSibling);

      assertEquals(mbOlderYounger, mbYoungerOlder);
      assertEquals(olderSibCreationId, mbOlderYounger);
    }


    /**
     * The MB between a plan and its nth child is the creation snapshot of the child plan's n-1's ancestor.
     */
    @Test
    void mergeBase10thGrandchild() throws SQLException {
      final int ancestor = merlinHelper.insertPlan(missionModelId);
      int priorAncestor = duplicatePlan(ancestor, "Child of " + ancestor);

      final int ninthGrandparentCreation;
      //get creation snapshot of the 9th grandparent
      try (final var statement = connection.createStatement()) {
        final var results = statement.executeQuery(
            """
                SELECT snapshot_id
                FROM plan_latest_snapshot
                WHERE plan_id = %d;
                """.formatted(priorAncestor)
        );
        assertTrue(results.next());
        ninthGrandparentCreation = results.getInt(1);
      }

      for (int i = 0; i < 8; ++i) {
        priorAncestor = duplicatePlan(priorAncestor, "Child of " + priorAncestor);
      }
      final int tenthGrandchild = duplicatePlan(priorAncestor, "10th Grandchild");

      final int mbAncestorGrandchild = getMergeBaseFromPlanIds(ancestor, tenthGrandchild);
      final int mbGrandchildAncestor = getMergeBaseFromPlanIds(tenthGrandchild, ancestor);

      assertEquals(mbGrandchildAncestor, mbAncestorGrandchild);
      assertEquals(ninthGrandparentCreation, mbAncestorGrandchild);
    }

    /**
     * The MB between nth-cousins is the creation snapshot of the eldest of the n-1 ancestors.
     */
    @Test
    void mergeBase10thCousin() throws SQLException{
      final int commonAncestor = merlinHelper.insertPlan(missionModelId);
      final int olderSibling = duplicatePlan(commonAncestor, "Older Sibling");

      final int olderSiblingCreation;
      try (final var statement = connection.createStatement()) {
        final var results = statement.executeQuery(
            """
                SELECT snapshot_id
                FROM plan_latest_snapshot
                WHERE plan_id = %d;
                """.formatted(olderSibling)
        );
        assertTrue(results.next());
        olderSiblingCreation = results.getInt(1);
      }

      final int youngerSibling = duplicatePlan(commonAncestor, "Younger Sibling");

      int olderDescendant = olderSibling;
      int youngerDescendant = youngerSibling;

      for (int i = 0; i < 9; ++i) {
        olderDescendant = duplicatePlan(olderDescendant, "Child of " + olderDescendant);
        youngerDescendant = duplicatePlan(youngerDescendant, "Child of " + youngerDescendant);
      }

      final int olderTenthCousin = duplicatePlan(olderDescendant, "Child of " + olderDescendant);
      final int youngerTenthCousin = duplicatePlan(youngerDescendant, "Child of " + youngerDescendant);

      final int mbOlderYounger = getMergeBaseFromPlanIds(olderTenthCousin, youngerTenthCousin);
      final int mbYoungerOlder = getMergeBaseFromPlanIds(youngerTenthCousin, olderTenthCousin);
      assertEquals(mbOlderYounger, mbYoungerOlder);
      assertEquals(olderSiblingCreation, mbOlderYounger);
    }

    /**
     * The MB between two plans that have been previously merged is the snapshot created during a merge
     */
    @Test
    void mergeBasePreviouslyMerged() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int newPlan = duplicatePlan(basePlan, "New Plan");
      final int creationSnapshot;
      final int postMergeSnapshot;

      try (final var statement = connection.createStatement()) {
        final var results = statement.executeQuery(
            """
                SELECT snapshot_id
                FROM plan_latest_snapshot
                WHERE plan_id = %d;
                """.formatted(newPlan)
        );
        assertTrue(results.next());
        creationSnapshot = results.getInt(1);
      }

      merlinHelper.insertActivity(newPlan);

      final int mergeRequest = createMergeRequest(basePlan, newPlan);
      beginMerge(mergeRequest);
      commitMerge(mergeRequest);

      try (final var statement = connection.createStatement()) {
        final var results = statement.executeQuery(
            """
                SELECT snapshot_id_supplying_changes
                FROM merge_request mr
                WHERE mr.id = %d;
                """.formatted(mergeRequest)
        );
        assertTrue(results.next());
        postMergeSnapshot = results.getInt(1);
      }

      final int newMergeBase = getMergeBaseFromPlanIds(basePlan, newPlan);
      assertNotEquals(creationSnapshot, newMergeBase);
      assertEquals(postMergeSnapshot, newMergeBase);
    }

    /**
     * First, check that find_MB throws an error if the first ID is invalid.
     * Then, check that find_MB throws an error if the second ID is invalid.
     * As a side effect validates invalid IDs for get_snapshot_history_from_plan and get_snapshot_history
     */
    @Test
    void mergeBaseFailsForInvalidPlanIds() throws SQLException {
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int snapshotId = createSnapshot(planId);

      try(final var statement = connection.createStatement()) {
        statement.execute(
            """
            select get_merge_base(%d, -1);
            """.formatted(planId)
        );
      }
      catch (SQLException sqlEx){
        if(!sqlEx.getMessage().contains("Snapshot ID "+-1 +" is not present in plan_snapshot table."))
          throw sqlEx;
      }

      try(final var statement = connection.createStatement()) {
        statement.execute(
            """
            select get_merge_base(-2, %d);
            """.formatted(snapshotId)
        );
      }
      catch (SQLException sqlEx){
        if(!sqlEx.getMessage().contains("Snapshot ID "+-2 +" is not present in plan_snapshot table."))
          throw sqlEx;
      }
    }

    /**
     * If there are multiple valid merge bases, get_merge_base only returns the one with the highest id.
     */
    @Test
    void multipleValidMergeBases() throws SQLException {
      final int plan1 = merlinHelper.insertPlan(missionModelId);
      final int plan2 = merlinHelper.insertPlan(missionModelId);

      final int plan1Snapshot = createSnapshot(plan1);
      final int plan2Snapshot = createSnapshot(plan2);

      //Create artificial Merge Bases
      try(final var statement = connection.createStatement()){
        statement.execute(
            """
            insert into plan_latest_snapshot(plan_id, snapshot_id) VALUES (%d, %d);
            """.formatted(plan2, plan1Snapshot)
        );
        statement.execute(
            """
            insert into plan_latest_snapshot(plan_id, snapshot_id) VALUES (%d, %d);
            """.formatted(plan1, plan2Snapshot)
        );

        //Plan2Snapshot is created after Plan1Snapshot, therefore it must have a higher id
        assertEquals(plan2Snapshot, getMergeBaseFromPlanIds(plan1, plan2));

        statement.execute(
            """
            delete from plan_latest_snapshot
            where snapshot_id = %d;
            """.formatted(plan2Snapshot)
        );

        assertEquals(plan1Snapshot, getMergeBaseFromPlanIds(plan1, plan2));
      }
    }

    /**
     * The MB between two plans that are unrelated is null.
     */
    @Test
    void noValidMergeBases() throws SQLException{
      final int plan1 = merlinHelper.insertPlan(missionModelId);
      final int plan2 = merlinHelper.insertPlan(missionModelId);

      createSnapshot(plan1);
      final int plan2Snapshot = createSnapshot(plan2);

      try(final var statement = connection.createStatement()){
        final var res = statement.executeQuery(
            """
            select get_merge_base(%d, %d);
            """.formatted(plan1, plan2Snapshot)
        );
        assertTrue(res.next());
        assertNull(res.getObject(1));
      }
    }
  }

  @Nested
  class MergeRequestTests{
    /**
     * First, check that it fails if plan_id_supplying is invalid and plan_id_receiving is valid
     * Then, check that it fails if plan_id_supplying is valid and plan_id_receiving is invalid
     */
    @Test
    void createRequestFailsForNonexistentPlans() throws SQLException {
      final int planId = merlinHelper.insertPlan(missionModelId);

      try{
        createMergeRequest(planId, -1);
        fail();
      }
      catch (SQLException sqEx){
        if(!sqEx.getMessage().contains("Plan supplying changes (Plan -1) does not exist."))
          throw sqEx;
      }

      try{
        createMergeRequest(-1, planId);
        fail();
      }
      catch (SQLException sqEx){
        if(!sqEx.getMessage().contains("Plan receiving changes (Plan -1) does not exist."))
          throw sqEx;
      }
    }

    @Test
    void createRequestFailsForUnrelatedPlans() throws SQLException {
      final int plan1 = merlinHelper.insertPlan(missionModelId);
      final int plan2 = merlinHelper.insertPlan(missionModelId);

      //Creating a snapshot so that the error comes from create_merge_request, not get_merge_base
      createSnapshot(plan1);

      try{
        createMergeRequest(plan1, plan2);
        fail();
      }
      catch (SQLException sqEx){
        if(!sqEx.getMessage().contains("Cannot create merge request between unrelated plans."))
          throw sqEx;
      }
    }

    @Test
    void createRequestFailsBetweenPlanAndSelf() throws SQLException {
      final int plan = merlinHelper.insertPlan(missionModelId);
      try{
        createMergeRequest(plan, plan);
        fail();
      } catch (SQLException sqEx){
        if(!sqEx.getMessage().contains("Cannot create a merge request between a plan and itself."))
          throw sqEx;
      }

    }

    @Test
    void withdrawFailsForNonexistentRequest() throws SQLException {
      try{
        withdrawMergeRequest(-1);
        fail();
      }
      catch (SQLException sqEx){
        if(!sqEx.getMessage().contains("Merge request -1 does not exist. Cannot withdraw request."))
          throw sqEx;
      }
    }
  }

  /**
   * Note: Test names in this class are written as:
   * [Difference between Receiving and MergeBase][Difference between Supplying and MergeBase]ResolvesAs[Resolution]
   */
  @Nested
  class BeginMergeTests {
    @Test
    void beginMergeFailsOnInvalidRequestId() throws SQLException {
      try{
        beginMerge(-1);
        fail();
      }catch (SQLException sqlEx){
        if(!sqlEx.getMessage().contains("Request ID -1 is not present in merge_request table."))
          throw sqlEx;
      }
    }

    @Test
    void beginMergeUpdatesMergeBase() throws SQLException {
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int childId = duplicatePlan(planId, "Child Plan");
      merlinHelper.insertActivity(childId); // Insert to avoid the NO-OP case in begin_merge
      final MergeRequest mergeRQ = getMergeRequest(createMergeRequest(planId, childId));
      assertEquals(getMergeBaseFromPlanIds(planId, childId), mergeRQ.mergeBaseSnapshot);

      // Artificially inject a new merge base.
      final int newMB = createSnapshot(planId);
      try(final var statement = connection.createStatement()){
        statement.execute(
            """
            insert into plan_snapshot_parent(snapshot_id, parent_snapshot_id)
                VALUES (%d, %d);
            """.formatted(mergeRQ.supplyingSnapshot, newMB)
        );
      }
      assertEquals(newMB, getMergeBaseFromPlanIds(planId, childId));

      beginMerge(mergeRQ.requestId);
      final MergeRequest updatedMergeRQ = getMergeRequest(mergeRQ.requestId);
      assertEquals(newMB, updatedMergeRQ.mergeBaseSnapshot);

      unlockPlan(planId);
    }

    @Test
    void beginMergeNoChangesThrowsError() throws SQLException {
      final int planId = merlinHelper.insertPlan(missionModelId);
      merlinHelper.insertActivity(planId);
      final int childPlan = duplicatePlan(planId, "Child");

      try {
        beginMerge(createMergeRequest(planId,childPlan));
        fail();
      } catch (SQLException sqlex) {
          if(!sqlex.getMessage().contains("Cannot begin merge. The contents of the two plans are identical.")){
            throw sqlex;
          }
      }
      // Assert that the plan was not locked
      try (final var statement = connection.createStatement()) {
        final var res = statement.executeQuery(
            """
            select is_locked
            from plan
            where id = %d;
            """.formatted(planId)
        );
        assertTrue(res.next());
        assertFalse(res.getBoolean(1));
      }
    }

    @Test
    void addReceivingResolvesAsNone() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      final int activityId = merlinHelper.insertActivity(basePlan);
      // Insert to avoid NO-OP case in begin_merge
      final int noopDodger = merlinHelper.insertActivity(childPlan);
      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(conflicts.isEmpty());
      assertFalse(stagedActs.isEmpty());
      assertEquals(2, stagedActs.size());
      assertEquals(activityId, stagedActs.get(0).activityId);
      assertEquals("none", stagedActs.get(0).changeType);
      assertEquals(noopDodger, stagedActs.get(1).activityId);
      assertEquals("add", stagedActs.get(1).changeType);

      unlockPlan(basePlan);
    }

    @Test
    void addSupplyingResolvesAsAdd() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      final int activityId = merlinHelper.insertActivity(childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(conflicts.isEmpty());
      assertFalse(stagedActs.isEmpty());
      assertEquals(1, stagedActs.size());
      assertEquals(activityId, stagedActs.get(0).activityId);
      assertEquals("add", stagedActs.get(0).changeType);

      unlockPlan(basePlan);
    }

    @Test
    void noneNoneResolvesAsNone() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      // Insert to avoid NO-OP case in begin_merge
      final int noopDodger = merlinHelper.insertActivity(childPlan);
      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(conflicts.isEmpty());
      assertFalse(stagedActs.isEmpty());
      assertEquals(2, stagedActs.size());
      assertEquals(activityId, stagedActs.get(0).activityId);
      assertEquals("none", stagedActs.get(0).changeType);
      assertEquals(noopDodger, stagedActs.get(1).activityId);
      assertEquals("add", stagedActs.get(1).changeType);

      unlockPlan(basePlan);
    }

    @Test
    void noneModifyResolvesAsModify() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      final String newName = "Test";

      updateActivityName(newName, activityId, childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(conflicts.isEmpty());
      assertFalse(stagedActs.isEmpty());
      assertEquals(1, stagedActs.size());
      assertEquals(activityId, stagedActs.get(0).activityId);
      assertEquals("modify", stagedActs.get(0).changeType);

      unlockPlan(basePlan);
    }

    @Test
    void noneDeleteResolvesAsDelete() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");

      deleteActivityDirective(childPlan, activityId);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(conflicts.isEmpty());
      assertFalse(stagedActs.isEmpty());
      assertEquals(1, stagedActs.size());
      assertEquals(activityId, stagedActs.get(0).activityId);
      assertEquals("delete", stagedActs.get(0).changeType);

      unlockPlan(basePlan);
    }

    @Test
    void modifyNoneResolvesAsNone() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      final String newName = "Test";

      updateActivityName(newName, activityId, basePlan);

      // Insert to avoid NO-OP case in begin_merge
      final int noopDodger = merlinHelper.insertActivity(childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(conflicts.isEmpty());
      assertFalse(stagedActs.isEmpty());
      assertEquals(2, stagedActs.size());
      assertEquals(activityId, stagedActs.get(0).activityId);
      assertEquals("none", stagedActs.get(0).changeType);
      assertEquals(noopDodger, stagedActs.get(1).activityId);
      assertEquals("add", stagedActs.get(1).changeType);

      unlockPlan(basePlan);
    }

    @Test
    void identicalModifyModifyResolvesAsNone() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      final String newName = "Test";

      updateActivityName(newName, activityId, basePlan);
      updateActivityName("Different Revision Proof", activityId, childPlan);
      updateActivityName(newName, activityId, childPlan);

      // Insert to avoid NO-OP case in begin_merge
      final int noopDodger = merlinHelper.insertActivity(childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(conflicts.isEmpty());
      assertFalse(stagedActs.isEmpty());
      assertEquals(2, stagedActs.size());
      assertEquals(activityId, stagedActs.get(0).activityId);
      assertEquals("none", stagedActs.get(0).changeType);
      assertEquals(noopDodger, stagedActs.get(1).activityId);
      assertEquals("add", stagedActs.get(1).changeType);

      unlockPlan(basePlan);
    }

    @Test
    void differentModifyModifyResolvesAsConflict() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      final String newName = "Test";

      updateActivityName(newName, activityId, basePlan);
      updateActivityName("Different", activityId, childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(stagedActs.isEmpty());
      assertFalse(conflicts.isEmpty());
      assertEquals(1, conflicts.size());
      assertEquals(activityId, conflicts.get(0).activityId);
      assertEquals("modify", conflicts.get(0).changeTypeReceiving);
      assertEquals("modify", conflicts.get(0).changeTypeSupplying);

      unlockPlan(basePlan);
    }

    @Test
    void modifyDeleteResolvesAsConflict() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      final String newName = "Test";

      updateActivityName(newName, activityId, basePlan);
      deleteActivityDirective(childPlan, activityId);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(stagedActs.isEmpty());
      assertFalse(conflicts.isEmpty());
      assertEquals(1, conflicts.size());
      assertEquals(activityId, conflicts.get(0).activityId);
      assertEquals("modify", conflicts.get(0).changeTypeReceiving);
      assertEquals("delete", conflicts.get(0).changeTypeSupplying);

      unlockPlan(basePlan);
    }

    @Test
    void deleteNoneIsExcludedFromStageAndConflict() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");

      deleteActivityDirective(basePlan, activityId);

      // Insert to avoid NO-OP case in begin_merge
      final int noopDodger = merlinHelper.insertActivity(childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertEquals(1, stagedActs.size());
      assertEquals(noopDodger, stagedActs.get(0).activityId);
      assertEquals("add", stagedActs.get(0).changeType);
      assertTrue(conflicts.isEmpty());

      unlockPlan(basePlan);
    }

    @Test
    void deleteModifyIsAConflict() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");
      final String newName = "Test";

      deleteActivityDirective(basePlan, activityId);
      updateActivityName(newName, activityId, childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertTrue(stagedActs.isEmpty());
      assertFalse(conflicts.isEmpty());
      assertEquals(1, conflicts.size());
      assertEquals(activityId, conflicts.get(0).activityId);
      assertEquals("delete", conflicts.get(0).changeTypeReceiving);
      assertEquals("modify", conflicts.get(0).changeTypeSupplying);

      unlockPlan(basePlan);
    }

    @Test
    void deleteDeleteIsExcludedFromStageAndConflict() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child Plan");

      deleteActivityDirective(basePlan, activityId);
      deleteActivityDirective(childPlan, activityId);

      // Insert to avoid NO-OP case in begin_merge
      final int noopDodger = merlinHelper.insertActivity(childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      final var stagedActs = getStagingAreaActivities(mergeRQ);
      final var conflicts = getConflictingActivities(mergeRQ);

      assertEquals(1, stagedActs.size());
      assertEquals(noopDodger, stagedActs.get(0).activityId);
      assertEquals("add", stagedActs.get(0).changeType);
      assertTrue(conflicts.isEmpty());

      unlockPlan(basePlan);
    }

  }

  @Nested
  class CommitMergeTests{
    @Test
    void commitMergeFailsForNonexistentId() throws SQLException {
      try {
        commitMerge(-1);
        fail();
      } catch (SQLException sqlex){
        if(!sqlex.getMessage().contains("Invalid merge request id -1."))
          throw sqlex;
      }
    }

    @Test
    void commitMergeFailsIfConflictsExist() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child");

      updateActivityName("BasePlan", activityId, basePlan);
      updateActivityName("ChildPlan", activityId, childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      try{
        commitMerge(mergeRQ);
        fail();
      } catch (SQLException sqlex){
        if(!sqlex.getMessage().contains("There are unresolved conflicts in merge request "+mergeRQ+". Cannot commit merge."))
          throw sqlex;
      }
    }

    @Test
    void commitMergeSucceedsIfAllConflictsAreResolved() throws SQLException{
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int modifyModifyActivityId = merlinHelper.insertActivity(basePlan);
      final int modifyDeleteActivityId = merlinHelper.insertActivity(basePlan);
      final int deleteModifyActivityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child");

      updateActivityName("BaseActivity1", modifyModifyActivityId, basePlan);
      updateActivityName("ChildActivity1", modifyModifyActivityId, childPlan);

      updateActivityName("BaseActivity2", modifyDeleteActivityId, basePlan);
      deleteActivityDirective(childPlan, modifyDeleteActivityId);

      deleteActivityDirective(basePlan, deleteModifyActivityId);
      updateActivityName("ChildActivity2", deleteModifyActivityId, childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);

      final var conflicts = getConflictingActivities(mergeRQ);
      final var stagingArea = getStagingAreaActivities(mergeRQ);
      assertFalse(conflicts.isEmpty());
      assertEquals(3, conflicts.size());
      assertTrue(stagingArea.isEmpty());

      for(final var conflict : conflicts){
        setResolution(mergeRQ, conflict.activityId, "receiving");
      }

      commitMerge(mergeRQ);
    }

    /**
     * This method tests the commit merge succeeds even if all non-conflicting changes are present.
     * This is also the test that checks the complete workflow with the largest amount of activities (425)
     */
    @Test
    void commitMergeSucceedsIfNoConflictsExist() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int[] baseActivities = new int[200];
      for(int i = 0; i < baseActivities.length; ++i){
        baseActivities[i] = merlinHelper.insertActivity(basePlan, "00:00:"+(i%60));
      }

      final int childPlan = duplicatePlan(basePlan, "Child");
      for(int i = 0; i < 200; ++i){
        merlinHelper.insertActivity(childPlan, "00:00:"+(i%60));
      }

      /*
        Does the following non-conflicting updates:
         -- leave the first 50 activities untouched
         -- delete the next 25 from the parent
         -- delete the next 25 from the child
         -- modify the next 50 in the parent
         -- modify the next 50 in the child
         -- add 25 activities to the parent
       */
      for(int i = 50;  i < 75;  ++i) { deleteActivityDirective(basePlan, baseActivities[i]); }
      for(int i = 75;  i < 100; ++i) { deleteActivityDirective(childPlan, baseActivities[i]); }
      for(int i = 100; i < 150; ++i) { updateActivityName("Renamed Activity " + i, baseActivities[i], basePlan); }
      for(int i = 150; i < 200; ++i) { updateActivityName("Renamed Activity " + i, baseActivities[i], childPlan); }
      for(int i = 0;   i < 25;  ++i) { merlinHelper.insertActivity(basePlan); }

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);
      commitMerge(mergeRQ);
      //assert that all activities are included now
      assertEquals(375, getActivities(basePlan).size()); //425-50
    }

    @Test
    void modifyAndDeletesApplyCorrectly() throws SQLException {
      /*
      Checks:
      -- modify uncontested supplying applies
      -- delete uncontested supplying applies
      -- modify contested supplying applies
      -- modify contested receiving does nothing
      -- delete contested supplying applies
      -- delete contested receiving applies
       */

      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int modifyUncontestedActId = merlinHelper.insertActivity(basePlan);
      final int deleteUncontestedActId = merlinHelper.insertActivity(basePlan);
      final int modifyContestedSupplyingActId = merlinHelper.insertActivity(basePlan);
      final int modifyContestedReceivingActId = merlinHelper.insertActivity(basePlan);
      final int deleteContestedSupplyingResolveSupplyingActId = merlinHelper.insertActivity(basePlan);
      final int deleteContestedSupplyingResolveReceivingActId = merlinHelper.insertActivity(basePlan);
      final int deleteContestedReceivingResolveReceivingActId = merlinHelper.insertActivity(basePlan);
      final int deleteContestedReceivingResolveSupplyingActId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child");

      assertEquals(8, getActivities(basePlan).size());
      assertEquals(8, getActivities(childPlan).size());

      updateActivityName("Test", modifyUncontestedActId, childPlan);

      deleteActivityDirective(childPlan, deleteUncontestedActId);

      updateActivityName("Modify Contested Supplying Parent", modifyContestedSupplyingActId, basePlan);
      updateActivityName("Modify Contested Supplying Child", modifyContestedSupplyingActId, childPlan);

      updateActivityName("Modify Contested Receiving Parent", modifyContestedReceivingActId, basePlan);
      updateActivityName("Modify Contested Receiving Child", modifyContestedReceivingActId, childPlan);

      updateActivityName("Delete Contested Supplying Parent Resolve Supplying", deleteContestedSupplyingResolveSupplyingActId, basePlan);
      deleteActivityDirective(childPlan, deleteContestedSupplyingResolveSupplyingActId);

      updateActivityName("Delete Contested Supplying Parent Resolve Receiving", deleteContestedSupplyingResolveReceivingActId, basePlan);
      deleteActivityDirective(childPlan, deleteContestedSupplyingResolveReceivingActId);

      deleteActivityDirective(basePlan, deleteContestedReceivingResolveReceivingActId);
      updateActivityName("Delete Contested Receiving Child Resolve Receiving", deleteContestedReceivingResolveReceivingActId, childPlan);

      deleteActivityDirective(basePlan, deleteContestedReceivingResolveSupplyingActId);
      updateActivityName("Delete Contested Receiving Child Resolve Supplying", deleteContestedReceivingResolveSupplyingActId, childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);

      assertEquals(6, getConflictingActivities(mergeRQ).size());
      setResolution(mergeRQ, modifyContestedSupplyingActId, "supplying");
      setResolution(mergeRQ, modifyContestedReceivingActId, "receiving");
      setResolution(mergeRQ, deleteContestedSupplyingResolveSupplyingActId, "supplying");
      setResolution(mergeRQ, deleteContestedSupplyingResolveReceivingActId, "receiving");
      setResolution(mergeRQ, deleteContestedReceivingResolveReceivingActId, "receiving");
      setResolution(mergeRQ, deleteContestedReceivingResolveSupplyingActId, "supplying");

      final Activity muActivityBefore = getActivity(basePlan, modifyUncontestedActId);
      final Activity mcsActivityBefore = getActivity(basePlan, modifyContestedSupplyingActId);
      final Activity mcrActivityBefore = getActivity(basePlan, modifyContestedReceivingActId);
      final Activity dcsActivityBefore = getActivity(basePlan, deleteContestedSupplyingResolveReceivingActId);

      commitMerge(mergeRQ);

      final var postMergeActivities = getActivities(basePlan);

      assertEquals(5, postMergeActivities.size());
      assertEquals(5, getActivities(childPlan).size());

      for (Activity activity : postMergeActivities) {
        if (activity.activityId == muActivityBefore.activityId) {
          final var muActivityChild = getActivity(childPlan, modifyUncontestedActId);
          // validate all shared properties
          assertActivityEquals(muActivityChild, activity);
        } else if (activity.activityId == mcsActivityBefore.activityId) {
          final var mcsActivityChild = getActivity(childPlan, modifyContestedSupplyingActId);
          // validate all shared properties
          assertActivityEquals(mcsActivityChild, activity);
        } else if (activity.activityId == mcrActivityBefore.activityId) {
          // validate all shared properties
          assertActivityEquals(mcrActivityBefore, activity);
        } else if (activity.activityId == deleteContestedSupplyingResolveReceivingActId) {
          // validate all shared properties
          assertActivityEquals(dcsActivityBefore, activity);
        } else if (activity.activityId == deleteContestedReceivingResolveSupplyingActId) {
          // validate all shared properties
          assertActivityEquals(getActivity(childPlan, deleteContestedReceivingResolveSupplyingActId), activity);
        } else fail();
      }
    }

    @Test
    void commitMergeCleansUpSuccessfully() throws SQLException{
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int conflictActivityId = merlinHelper.insertActivity(basePlan);
      final int childPlan = duplicatePlan(basePlan, "Child");
      for(int i = 0; i < 5; ++i){ merlinHelper.insertActivity(basePlan, "00:00:"+(i%60)); }
      for(int i = 0; i < 5; ++i){ merlinHelper.insertActivity(basePlan, "00:00:"+(i%60)); }

      updateActivityName("Conflict!", conflictActivityId, basePlan);
      updateActivityName("Conflict >:-)", conflictActivityId, childPlan);

      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);

      final var conflicts = getConflictingActivities(mergeRQ);
      final var stagingArea = getStagingAreaActivities(mergeRQ);
      assertFalse(conflicts.isEmpty());
      assertEquals(1, conflicts.size());
      assertFalse(stagingArea.isEmpty());
      assertEquals(10, stagingArea.size());

      setResolution(mergeRQ, conflictActivityId, "receiving");
      commitMerge(mergeRQ);

      assertTrue(getConflictingActivities(mergeRQ).isEmpty());
      assertTrue(getConflictingActivities(mergeRQ).isEmpty());
      try(final var statement = connection.createStatement()){
        final var res = statement.executeQuery("""
             SELECT plan.id as plan_id, is_locked, status
             FROM plan
             JOIN merge_request
             ON plan_id_receiving_changes = plan.id
             WHERE plan.id = %d;
             """.formatted(basePlan)
        );
        assertTrue(res.next());
        assertEquals(basePlan, res.getInt("plan_id"));
        assertFalse(res.getBoolean("is_locked"));
        assertEquals("accepted", res.getString("status"));
      }
    }
  }

  @Nested
  class MergeStateMachineTests{
    @Test
    void cancelFailsForInvalidId() throws SQLException{
      try{
        cancelMerge(-1);
        fail();
      } catch (SQLException sqlException) {
        if(!sqlException.getMessage().contains("Invalid merge request id -1."))
          throw sqlException;
      }
    }

    @Test
    void denyFailsForInvalidId() throws SQLException {
      try{
        denyMerge(-1);
        fail();
      } catch (SQLException sqlException) {
        if(!sqlException.getMessage().contains("Invalid merge request id -1."))
          throw sqlException;
      }
    }

    @Test
    void withdrawFailsForInvalidId() throws SQLException {
      try{
        withdrawMergeRequest(-1);
        fail();
      } catch (SQLException sqlException){
        if(!sqlException.getMessage().contains("Merge request -1 does not exist. Cannot withdraw request."))
          throw sqlException;
      }
    }

    @Test
    void defaultStateOfMergeRequestIsPendingStatus() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int childPlan = duplicatePlan(basePlan, "Child");
      final MergeRequest mergeRequest = getMergeRequest(createMergeRequest(basePlan, childPlan));
      assertEquals("pending", mergeRequest.status);
    }

    @Test
    void beginMergeOnlySucceedsOnPendingStatus() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int childPlan = duplicatePlan(basePlan, "Child");
      merlinHelper.insertActivity(childPlan);
      final int mergeRQ = createMergeRequest(basePlan, childPlan);

      setMergeRequestStatus(mergeRQ, "withdrawn");
      try {
        beginMerge(mergeRQ);
      } catch (SQLException sqlEx){
        if (!sqlEx.getMessage().contains("Cannot begin request. Merge request "+mergeRQ+" is not in pending state."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "accepted");
      try {
        beginMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot begin request. Merge request "+mergeRQ+" is not in pending state."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "rejected");
      try {
        beginMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot begin request. Merge request "+mergeRQ+" is not in pending state."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "in-progress");
      try {
        beginMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot begin request. Merge request "+mergeRQ+" is not in pending state."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "pending");
      beginMerge(mergeRQ);
    }

    @Test
    void withdrawOnlySucceedsOnPendingOrWithdrawnStatus() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int childPlan = duplicatePlan(basePlan, "Child");
      merlinHelper.insertActivity(childPlan);
      final int mergeRQ = createMergeRequest(basePlan, childPlan);

      setMergeRequestStatus(mergeRQ, "pending");
      withdrawMergeRequest(mergeRQ);

      setMergeRequestStatus(mergeRQ, "withdrawn");
      withdrawMergeRequest(mergeRQ);

      setMergeRequestStatus(mergeRQ, "accepted");
      try {
        withdrawMergeRequest(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot withdraw request."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "rejected");
      try {
        withdrawMergeRequest(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot withdraw request."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "in-progress");
      try {
        withdrawMergeRequest(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot withdraw request."))
          throw sqlEx;
      }
    }

    @Test
    void cancelOnlySucceedsOnInProgressOrPendingStatus() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int childPlan = duplicatePlan(basePlan, "Child");
      merlinHelper.insertActivity(childPlan);
      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);

      setMergeRequestStatus(mergeRQ, "pending");
      cancelMerge(mergeRQ);

      setMergeRequestStatus(mergeRQ, "withdrawn");
      try {
        cancelMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot cancel merge."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "accepted");
      try {
        cancelMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot cancel merge."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "rejected");
      try {
        cancelMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot cancel merge."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "in-progress");
      cancelMerge(mergeRQ);
    }

    @Test
    void denyOnlySucceedsOnInProgressStatus() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int childPlan = duplicatePlan(basePlan, "Child");
      merlinHelper.insertActivity(childPlan);
      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);

      setMergeRequestStatus(mergeRQ, "pending");
      try {
        denyMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot reject merge not in progress."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "withdrawn");
      try {
        denyMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot reject merge not in progress."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "accepted");
      try {
        denyMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot reject merge not in progress."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "rejected");
      try {
        denyMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot reject merge not in progress."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "in-progress");
      denyMerge(mergeRQ);
    }

    @Test
    void commitOnlySucceedsOnInProgressStatus() throws SQLException {
      final int basePlan = merlinHelper.insertPlan(missionModelId);
      final int childPlan = duplicatePlan(basePlan, "Child");
      merlinHelper.insertActivity(childPlan);
      final int mergeRQ = createMergeRequest(basePlan, childPlan);
      beginMerge(mergeRQ);

      setMergeRequestStatus(mergeRQ, "pending");
      try {
        commitMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot commit a merge request that is not in-progress."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "withdrawn");
      try {
        commitMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot commit a merge request that is not in-progress."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "accepted");
      try {
        commitMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot commit a merge request that is not in-progress."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "rejected");
      try {
        commitMerge(mergeRQ);
        fail();
      } catch (SQLException sqlEx) {
        if (!sqlEx.getMessage().contains("Cannot commit a merge request that is not in-progress."))
          throw sqlEx;
      }

      setMergeRequestStatus(mergeRQ, "in-progress");
      commitMerge(mergeRQ);
    }

    /**
     * Validates that the status of only the withdrawn request is set to "withdrawn"
     */
    @Test
    void withdrawCleansUpSuccessfully() throws SQLException {
      final int basePlan1 = merlinHelper.insertPlan(missionModelId);
      final int basePlan2 = merlinHelper.insertPlan(missionModelId);
      final int childPlan1 = duplicatePlan(basePlan1, "Child of Base Plan 1");
      final int childPlan2 = duplicatePlan(basePlan2, "Child of Base Plan 2");
      final int mergeRQ1 = createMergeRequest(basePlan1, childPlan1);
      final int mergeRQ2 = createMergeRequest(basePlan2, childPlan2);

      assertEquals("pending", getMergeRequest(mergeRQ1).status);
      assertEquals("pending", getMergeRequest(mergeRQ2).status);

      withdrawMergeRequest(mergeRQ1);

      assertEquals("withdrawn", getMergeRequest(mergeRQ1).status);
      assertEquals("pending", getMergeRequest(mergeRQ2).status);
    }

    /**
     * Validates that the status of only the denied request is set to "rejected"
     * And that the plan receiving changes is unlocked
     * And that the Merge Staging Area and Conflicting Activities no longer contains data relevant for the denied merge.
     */
    @Test
    void denyCleansUpSuccessfully() throws SQLException {
      final int basePlan1 = merlinHelper.insertPlan(missionModelId);
      final int basePlan2 = merlinHelper.insertPlan(missionModelId);
      final int baseActivity1 = merlinHelper.insertActivity(basePlan1);
      final int baseActivity2 = merlinHelper.insertActivity(basePlan2);

      final int childPlan1 = duplicatePlan(basePlan1, "Child of Base Plan 1");
      final int childPlan2 = duplicatePlan(basePlan2, "Child of Base Plan 2");
      final int childActivity1 = merlinHelper.insertActivity(childPlan1);
      final int childActivity2 = merlinHelper.insertActivity(childPlan2);

      updateActivityName("Conflict 1 Base", baseActivity1, basePlan1);
      updateActivityName("Conflict 2 Base", baseActivity2, basePlan2);

      updateActivityName("Conflict 1 Child", baseActivity1, childPlan1);
      updateActivityName("Conflict 2 Child", baseActivity2, childPlan2);

      final int mergeRQ1 = createMergeRequest(basePlan1, childPlan1);
      final int mergeRQ2 = createMergeRequest(basePlan2, childPlan2);

      beginMerge(mergeRQ1);
      beginMerge(mergeRQ2);

      //Assert the request status
      assertEquals("in-progress", getMergeRequest(mergeRQ1).status);
      assertEquals("in-progress", getMergeRequest(mergeRQ2).status);

      //Assert the staging area
      assertEquals(1, getStagingAreaActivities(mergeRQ1).size());
      assertEquals(childActivity1, getStagingAreaActivities(mergeRQ1).get(0).activityId);
      assertEquals(1, getStagingAreaActivities(mergeRQ2).size());
      assertEquals(childActivity2, getStagingAreaActivities(mergeRQ2).get(0).activityId);

      //Assert the conflict
      assertEquals(1, getConflictingActivities(mergeRQ1).size());
      assertEquals(baseActivity1, getConflictingActivities(mergeRQ1).get(0).activityId);
      assertEquals(1, getConflictingActivities(mergeRQ2).size());
      assertEquals(baseActivity2, getConflictingActivities(mergeRQ2).get(0).activityId);

      //Assert both plans are locked
      try(final var statement = connection.createStatement()){
        var res = statement.executeQuery(
              """
              SELECT is_locked
              FROM plan
              WHERE id = %d;
              """.formatted(basePlan1)
        );
        assertTrue(res.next());
        assertTrue(res.getBoolean(1));

        res = statement.executeQuery(
            """
            SELECT is_locked
            FROM plan
            WHERE id = %d;
            """.formatted(basePlan2)
        );
        assertTrue(res.next());
        assertTrue(res.getBoolean(1));
      }

      denyMerge(mergeRQ1);

      //Assert the request status
      assertEquals("rejected", getMergeRequest(mergeRQ1).status);
      assertEquals("in-progress", getMergeRequest(mergeRQ2).status);

      //Assert the staging area
      assertTrue(getStagingAreaActivities(mergeRQ1).isEmpty());
      assertEquals(1, getStagingAreaActivities(mergeRQ2).size());
      assertEquals(childActivity2, getStagingAreaActivities(mergeRQ2).get(0).activityId);

      //Assert the conflict
      assertTrue(getConflictingActivities(mergeRQ1).isEmpty());
      assertEquals(1, getConflictingActivities(mergeRQ2).size());
      assertEquals(baseActivity2, getConflictingActivities(mergeRQ2).get(0).activityId);

      //Assert only the in-progress merge is now locked
      try(final var statement = connection.createStatement()){
        var res = statement.executeQuery(
            """
            SELECT is_locked
            FROM plan
            WHERE id = %d;
            """.formatted(basePlan1)
        );
        assertTrue(res.next());
        assertFalse(res.getBoolean(1));

        res = statement.executeQuery(
            """
            SELECT is_locked
            FROM plan
            WHERE id = %d;
            """.formatted(basePlan2)
        );
        assertTrue(res.next());
        assertTrue(res.getBoolean(1));
      }
    }

    /**
     * Validates that the status of only the denied request is set to "pending"
     * And that the plan receiving changes is unlocked
     * And that the Merge Staging Area and Conflicting Activities no longer contains data relevant for the canceled merge.
     */
    @Test
    void cancelCleansUpSuccessfully() throws SQLException {
      final int basePlan1 = merlinHelper.insertPlan(missionModelId);
      final int basePlan2 = merlinHelper.insertPlan(missionModelId);
      final int baseActivity1 = merlinHelper.insertActivity(basePlan1);
      final int baseActivity2 = merlinHelper.insertActivity(basePlan2);

      final int childPlan1 = duplicatePlan(basePlan1, "Child of Base Plan 1");
      final int childPlan2 = duplicatePlan(basePlan2, "Child of Base Plan 2");
      final int childActivity1 = merlinHelper.insertActivity(childPlan1);
      final int childActivity2 = merlinHelper.insertActivity(childPlan2);

      updateActivityName("Conflict 1 Base", baseActivity1, basePlan1);
      updateActivityName("Conflict 2 Base", baseActivity2, basePlan2);

      updateActivityName("Conflict 1 Child", baseActivity1, childPlan1);
      updateActivityName("Conflict 2 Child", baseActivity2, childPlan2);

      final int mergeRQ1 = createMergeRequest(basePlan1, childPlan1);
      final int mergeRQ2 = createMergeRequest(basePlan2, childPlan2);

      beginMerge(mergeRQ1);
      beginMerge(mergeRQ2);

      //Assert the request status
      assertEquals("in-progress", getMergeRequest(mergeRQ1).status);
      assertEquals("in-progress", getMergeRequest(mergeRQ2).status);

      //Assert the staging area
      assertEquals(1, getStagingAreaActivities(mergeRQ1).size());
      assertEquals(childActivity1, getStagingAreaActivities(mergeRQ1).get(0).activityId);
      assertEquals(1, getStagingAreaActivities(mergeRQ2).size());
      assertEquals(childActivity2, getStagingAreaActivities(mergeRQ2).get(0).activityId);

      //Assert the conflict
      assertEquals(1, getConflictingActivities(mergeRQ1).size());
      assertEquals(baseActivity1, getConflictingActivities(mergeRQ1).get(0).activityId);
      assertEquals(1, getConflictingActivities(mergeRQ2).size());
      assertEquals(baseActivity2, getConflictingActivities(mergeRQ2).get(0).activityId);

      //Assert both plans are locked
      try(final var statement = connection.createStatement()){
        var res = statement.executeQuery(
            """
            SELECT is_locked
            FROM plan
            WHERE id = %d;
            """.formatted(basePlan1)
        );
        assertTrue(res.next());
        assertTrue(res.getBoolean(1));

        res = statement.executeQuery(
            """
            SELECT is_locked
            FROM plan
            WHERE id = %d;
            """.formatted(basePlan2)
        );
        assertTrue(res.next());
        assertTrue(res.getBoolean(1));
      }

      cancelMerge(mergeRQ1);

      //Assert the request status
      assertEquals("pending", getMergeRequest(mergeRQ1).status);
      assertEquals("in-progress", getMergeRequest(mergeRQ2).status);

      //Assert the staging area
      assertTrue(getStagingAreaActivities(mergeRQ1).isEmpty());
      assertEquals(1, getStagingAreaActivities(mergeRQ2).size());
      assertEquals(childActivity2, getStagingAreaActivities(mergeRQ2).get(0).activityId);

      //Assert the conflict
      assertTrue(getConflictingActivities(mergeRQ1).isEmpty());
      assertEquals(1, getConflictingActivities(mergeRQ2).size());
      assertEquals(baseActivity2, getConflictingActivities(mergeRQ2).get(0).activityId);

      //Assert only the in-progress merge is now locked
      try(final var statement = connection.createStatement()){
        var res = statement.executeQuery(
            """
            SELECT is_locked
            FROM plan
            WHERE id = %d;
            """.formatted(basePlan1)
        );
        assertTrue(res.next());
        assertFalse(res.getBoolean(1));

        res = statement.executeQuery(
            """
            SELECT is_locked
            FROM plan
            WHERE id = %d;
            """.formatted(basePlan2)
        );
        assertTrue(res.next());
        assertTrue(res.getBoolean(1));
      }
    }
  }

  @Nested
  class AnchorMergeTests{
    @Test
    void cantMergeCycle() throws SQLException{
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityA = merlinHelper.insertActivity(planId);
      final int activityB = merlinHelper.insertActivity(planId);

      final int childPlan = duplicatePlan(planId, "Cycle Test Plan");

      // Plan chain: B -> A
      merlinHelper.setAnchor(activityA, true, activityB, planId);
      // ChildPlan chain: A -> B
      merlinHelper.setAnchor(activityB, true, activityA, childPlan);

      // Merge fails as it would establish B -> A -> B cycle
      final int mergeRQ = createMergeRequest(planId, childPlan);
      beginMerge(mergeRQ);
      try {
        commitMerge(mergeRQ);
        fail();
      } catch (SQLException ex) {
        if(!ex.getMessage().contains("Cycle detected. Cannot apply changes.")){
          throw ex;
        }
      }
    }

    @Test
    void anchorMustBeInTargetPlanAtEndOfMerge() throws SQLException{
      // Can't merge in a version of an activity that is anchored to an activity that is deleted from the target plan.
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityA = merlinHelper.insertActivity(planId);
      final int activityB = merlinHelper.insertActivity(planId);

      final int childPlan = duplicatePlan(planId, "Anchor Delete Test");
      merlinHelper.setAnchor(activityA, true, activityB, childPlan);

      deleteActivityDirective(planId, activityA);

      final int mergeRQ = createMergeRequest(planId, childPlan);
      beginMerge(mergeRQ);
      try{
        commitMerge(mergeRQ);
        fail();
      } catch (SQLException ex){
        if(!ex.getMessage().contains(
            "insert or update on table \"activity_directive\" violates foreign key constraint \"anchor_in_plan\"")){
          throw ex;
        }
      }
    }

    @Test
    void deleteSubtreeDoesNotImpactRelatedPlans() throws SQLException{
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityAId = merlinHelper.insertActivity(planId);
      final int activityBId = merlinHelper.insertActivity(planId);
      merlinHelper.setAnchor(activityAId, true, activityBId, planId);
      final int childPlanId = duplicatePlan(planId, "Delete Chain Test");

      final Activity activityABefore = getActivity(childPlanId, activityAId);
      final Activity activityBBefore = getActivity(childPlanId, activityBId);

      try(final var statement = connection.createStatement()) {
        statement.execute(
            """
             select hasura_functions.delete_activity_by_pk_delete_subtree(%d, %d)
             """.formatted(activityAId, planId));
      }
      assertEquals(0, getActivities(planId).size());
      assertEquals(2, getActivities(childPlanId).size());
      assertActivityEquals(activityABefore, getActivity(childPlanId, activityAId));
      assertActivityEquals(activityBBefore, getActivity(childPlanId, activityBId));
    }

    @Test
    void deletePlanReanchorDoesNotImpactRelatedPlans() throws SQLException{
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityAId = merlinHelper.insertActivity(planId);
      final int activityBId = merlinHelper.insertActivity(planId);
      merlinHelper.setAnchor(activityAId, true, activityBId, planId);
      final int childPlanId = duplicatePlan(planId, "Delete Chain Test");

      final Activity activityABefore = getActivity(childPlanId, activityAId);
      final Activity activityBBefore = getActivity(childPlanId, activityBId);

      try(final var statement = connection.createStatement()) {
        statement.execute(
            """
             select hasura_functions.delete_activity_by_pk_reanchor_plan_start(%d, %d)
             """.formatted(activityAId, planId));
      }
      assertEquals(1, getActivities(planId).size());
      assertEquals(2, getActivities(childPlanId).size());
      assertActivityEquals(activityABefore, getActivity(childPlanId, activityAId));
      assertActivityEquals(activityBBefore, getActivity(childPlanId, activityBId));
    }
    @Test
    void deleteActivityReanchorDoesNotImpactRelatedPlans() throws SQLException{
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityAId = merlinHelper.insertActivity(planId);
      final int activityBId = merlinHelper.insertActivity(planId);
      final int activityCId = merlinHelper.insertActivity(planId);
      merlinHelper.setAnchor(activityAId, true, activityBId, planId);
      merlinHelper.setAnchor(activityCId, true, activityAId, planId);
      final int childPlanId = duplicatePlan(planId, "Delete Chain Test");

      final Activity activityABefore = getActivity(childPlanId, activityAId);
      final Activity activityBBefore = getActivity(childPlanId, activityBId);
      final Activity activityCBefore = getActivity(childPlanId, activityCId);

      try(final var statement = connection.createStatement()) {
        statement.execute(
            """
             select hasura_functions.delete_activity_by_pk_reanchor_to_anchor(%d, %d)
             """.formatted(activityAId, planId));
      }
      assertEquals(2, getActivities(planId).size());
      assertEquals(3, getActivities(childPlanId).size());
      assertActivityEquals(activityABefore, getActivity(childPlanId, activityAId));
      assertActivityEquals(activityBBefore, getActivity(childPlanId, activityBId));
      assertActivityEquals(activityCBefore, getActivity(childPlanId, activityCId));
    }
  }

  @Nested
  class PresetTests{
    private final gov.nasa.jpl.aerie.database.PresetTests presetTests = new gov.nasa.jpl.aerie.database.PresetTests();

    // Activities added in branches keep their preset information when merged
    @Test
    void presetPersistsWithAdd() throws SQLException{
      presetTests.setConnection(helper);
      merlinHelper.insertActivityType(missionModelId, "test-activity");
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int branchId = duplicatePlan(planId, "Add Preset Branch");
      final int presetId = presetTests.insertPreset(missionModelId, "Demo Preset", "test-activity");
      final int activityId = merlinHelper.insertActivity(branchId);
      presetTests.assignPreset(presetId, activityId, branchId);

      // Merge
      final int mergeRQId = createMergeRequest(planId, branchId);
      beginMerge(mergeRQId);
      commitMerge(mergeRQId);

      // Assertions
      final var presetActivities = presetTests.getActivitiesWithPreset(presetId);
      assertEquals(2, presetActivities.size());
      assertEquals(presetId, presetTests.getPresetAssignedToActivity(activityId, planId).id());
      assertEquals(presetId, presetTests.getPresetAssignedToActivity(activityId, branchId).id());
    }

    // The preset set in the supplying activity persists
    @Test
    void presetPersistsWithModify() throws SQLException{
      presetTests.setConnection(helper);
      merlinHelper.insertActivityType(missionModelId, "test-activity");
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(planId);
      final int branchId = duplicatePlan(planId, "Modify Preset Branch");
      final int presetId = presetTests.insertPreset(missionModelId, "Demo Preset", "test-activity", "{\"destination\": \"Mars\"}");
      presetTests.assignPreset(presetId, activityId, branchId);

      // Merge
      final int mergeRQId = createMergeRequest(planId, branchId);
      beginMerge(mergeRQId);
      commitMerge(mergeRQId);

      // Assertions
      final var presetActivities = presetTests.getActivitiesWithPreset(presetId);
      assertEquals(2, presetActivities.size());
      assertEquals(presetId, presetTests.getPresetAssignedToActivity(activityId, planId).id());
      assertEquals(presetId, presetTests.getPresetAssignedToActivity(activityId, branchId).id());
    }

    // If the preset used in a snapshot is deleted during the merge, the activity does not have a preset after the merge.
    @Test
    void postMergeNoPresetIfPresetDeleted() throws SQLException{
      presetTests.setConnection(helper);
      merlinHelper.insertActivityType(missionModelId, "test-activity");
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int branchId = duplicatePlan(planId, "Delete Preset Branch");
      final int presetId = presetTests.insertPreset(missionModelId, "Demo Preset", "test-activity");
      final int activityId = merlinHelper.insertActivity(branchId);
      presetTests.assignPreset(presetId, activityId, branchId);

      // Merge
      final int mergeRQId = createMergeRequest(planId, branchId);
      beginMerge(mergeRQId);
      presetTests.deletePreset(presetId);
      commitMerge(mergeRQId);

      // Assertions
      final var presetActivities = presetTests.getActivitiesWithPreset(presetId);
      assertTrue(presetActivities.isEmpty());
      assertNull(presetTests.getPresetAssignedToActivity(activityId, planId));
      assertNull(presetTests.getPresetAssignedToActivity(activityId, branchId));
    }
  }

  @Nested
  class TagsTests {
    private final gov.nasa.jpl.aerie.database.TagsTests tagsHelper = new gov.nasa.jpl.aerie.database.TagsTests();
    { tagsHelper.setConnection(helper);}

    // Checks that both activity directive and plan tags are copied
    @Test
    void duplicateCopiesTags() throws SQLException {
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityCount = 200;
      final var activityIds = new HashSet<>();
      final int farmTagId = tagsHelper.insertTag("Farm");
      final int tractorTagId = tagsHelper.insertTag("Tractor");
      for (var i = 0; i < activityCount; i++) {
        final int activityId = merlinHelper.insertActivity(planId);
        activityIds.add(activityId);
        if(i % 3 == 0) {
          tagsHelper.assignTagToActivity(activityId, planId, farmTagId);
        }
        if(i % 3 == 1) {
          tagsHelper.assignTagToActivity(activityId, planId, tractorTagId);
        }
      }
      tagsHelper.assignTagToPlan(planId, farmTagId);

      final var planActivities = getActivities(planId);
      final var planTags = tagsHelper.getTagsOnPlan(planId);

      final var childPlan = duplicatePlan(planId, "My new duplicated plan");
      final var childActivities = getActivities(childPlan);
      final var childTags = tagsHelper.getTagsOnPlan(childPlan);

      // Assert Plan Tags were copied
      assertEquals(planTags, childTags);

      for (int i = 0; i < activityCount; ++i) {
        assertTrue(activityIds.contains(planActivities.get(i).activityId));
        assertTrue(activityIds.contains(childActivities.get(i).activityId));

        // Assert Activity Tags were copied
        assertEquals(planActivities.get(i).activityId, childActivities.get(i).activityId);
        assertEquals(tagsHelper.getTagsOnActivity(planActivities.get(i).activityId, planId),
                     tagsHelper.getTagsOnActivity(childActivities.get(i).activityId, childPlan));

        activityIds.remove(planActivities.get(i).activityId);
      }
      assert activityIds.isEmpty();
    }

    @Test
    void snapshotCopiesTags() throws SQLException {
      final var planId = merlinHelper.insertPlan(missionModelId);
      final int activityCount = 200;
      final var activityIds = new HashSet<>();
      final int farmTagId = tagsHelper.insertTag("Farm");
      final int tractorTagId = tagsHelper.insertTag("Tractor");
      for (var i = 0; i < activityCount; i++) {
        final int activityId = merlinHelper.insertActivity(planId);
        activityIds.add(activityId);
        if (i % 3 == 0) {
          tagsHelper.assignTagToActivity(activityId, planId, farmTagId);
        }
        if (i % 3 == 1) {
          tagsHelper.assignTagToActivity(activityId, planId, tractorTagId);
        }
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
        // validate tags were copied
        assertEquals(planActivities.get(i).activityId, snapshotActivities.get(i).activityId);
        assertEquals(
            tagsHelper.getTagsOnActivity(planActivities.get(i).activityId, planId),
            tagsHelper.getTagsOnActivitySnapshot(snapshotActivities.get(i).activityId, snapshotId));
        activityIds.remove(planActivities.get(i).activityId);
      }
      assert activityIds.isEmpty();
    }

    @Test
    void tagsPersistWithAdd() throws SQLException {
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int untaggedActivityId = merlinHelper.insertActivity(planId);
      final int branchId = duplicatePlan(planId, "Add Tag Branch");
      final int tagId = tagsHelper.insertTag("Farm");
      final int taggedActivityId = merlinHelper.insertActivity(branchId);
      tagsHelper.assignTagToActivity(taggedActivityId, branchId, tagId);

      // Merge
      final int mergeRQId = createMergeRequest(planId, branchId);
      beginMerge(mergeRQId);
      commitMerge(mergeRQId);

      // Assertions
      final var activities = getActivities(planId);
      assertEquals(2, activities.size());
      assertEquals(new ArrayList<Tag>(), tagsHelper.getTagsOnActivity(untaggedActivityId, planId));
      final var expectedTags = new ArrayList<Tag>();
      expectedTags.add(new Tag(tagId, "Farm", null, -1));
      assertEquals(expectedTags, tagsHelper.getTagsOnActivity(taggedActivityId, planId));
    }

    // If the tags on an activity aren't updated but another part of the activity is, the tags should not change
    @Test
    void tagsPersistWithModify() throws SQLException {
      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(planId);
      final int tagId = tagsHelper.insertTag("Farm");
      tagsHelper.assignTagToActivity(activityId, planId, tagId);
      final int branchId = duplicatePlan(planId, "Modify Tags Branch");
      updateActivityName("New Name", activityId, branchId);

      // Merge
      final int mergeRQId = createMergeRequest(planId, branchId);
      beginMerge(mergeRQId);
      commitMerge(mergeRQId);

      // Assertions
      final var expectedTag = new ArrayList<Tag>();
      expectedTag.add(new Tag(tagId, "Farm", null, -1));
      assertEquals(expectedTag, tagsHelper.getTagsOnActivity(activityId, planId));
    }

    // If the tags on an activity are updated, the tags on the activity post-merge should reflect that
    @Test
    void tagsUpdatedWithModify() throws SQLException {
      final int farmTagId = tagsHelper.insertTag("Farm");
      final int tractorTagId = tagsHelper.insertTag("Tractor");
      final int barnTagId = tagsHelper.insertTag("Barn");

      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(activityId, planId, farmTagId);
      tagsHelper.assignTagToActivity(activityId, planId, tractorTagId);

      final int branchId = duplicatePlan(planId, "Modify Tags Branch");
      tagsHelper.assignTagToActivity(activityId, branchId, barnTagId);
      tagsHelper.removeTagFromActivity(activityId, branchId, tractorTagId);

      // Merge
      final int mergeRQId = createMergeRequest(planId, branchId);
      beginMerge(mergeRQId);
      commitMerge(mergeRQId);

      // Assertions
      final var tags = tagsHelper.getTagsOnActivity(activityId, planId);
      final var expectedTags = new ArrayList<Tag>();
      expectedTags.add(new Tag(farmTagId, "Farm", null, -1));
      expectedTags.add(new Tag(barnTagId, "Barn", null, -1));
      assertEquals(2, tags.size());
      assertEquals(expectedTags, tags);
    }

    // In these modify-delete conflicts, the "modify" option is always picked
    @Test
    void tagsPersistWithModifyDeleteConflict() throws SQLException {
      final int farmTagId = tagsHelper.insertTag("Farm");
      final int tractorTagId = tagsHelper.insertTag("Tractor");
      final int barnTagId = tagsHelper.insertTag("Barn");

      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(activityId, planId, farmTagId);
      final int branchId = duplicatePlan(planId, "Tags Modify Delete");

      tagsHelper.assignTagToActivity(activityId, planId, tractorTagId);
      tagsHelper.assignTagToActivity(activityId, planId, barnTagId);
      tagsHelper.removeTagFromActivity(activityId, planId, farmTagId);
      deleteActivityDirective(branchId, activityId);

      final int mergeRQ = createMergeRequest(planId, branchId);
      beginMerge(mergeRQ);
      setResolution(mergeRQ, activityId, "receiving");
      commitMerge(mergeRQ);

      final var activities = getActivities(planId);
      assertEquals(1, activities.size());
      final var expectedTags = new ArrayList<Tag>();
      expectedTags.add(new Tag(tractorTagId, "Tractor", null, -1));
      expectedTags.add(new Tag(barnTagId, "Barn", null, -1));

      assertEquals(expectedTags, tagsHelper.getTagsOnActivity(activityId, planId));
    }

    @Test
    void tagsPersistWithDeleteModifyConflict() throws SQLException {
      final int farmTagId = tagsHelper.insertTag("Farm");
      final int tractorTagId = tagsHelper.insertTag("Tractor");
      final int barnTagId = tagsHelper.insertTag("Barn");

      final int planId = merlinHelper.insertPlan(missionModelId);
      final int activityId = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(activityId, planId, farmTagId);
      final int branchId = duplicatePlan(planId, "Tags Modify Delete");

      tagsHelper.assignTagToActivity(activityId, branchId, tractorTagId);
      tagsHelper.assignTagToActivity(activityId, branchId, barnTagId);
      tagsHelper.removeTagFromActivity(activityId, branchId, farmTagId);
      deleteActivityDirective(planId, activityId);

      final int mergeRQ = createMergeRequest(planId, branchId);
      beginMerge(mergeRQ);
      setResolution(mergeRQ, activityId, "supplying");
      commitMerge(mergeRQ);

      final var activities = getActivities(planId);
      assertEquals(1, activities.size());
      final var expectedTags = new ArrayList<Tag>();
      expectedTags.add(new Tag(tractorTagId, "Tractor", null, -1));
      expectedTags.add(new Tag(barnTagId, "Barn", null, -1));

      assertEquals(expectedTags, tagsHelper.getTagsOnActivity(activityId, planId));
    }

    @Test
    void tagsPersistWithModifyModifyConflict() throws SQLException {
      final int farmTagId = tagsHelper.insertTag("Farm");
      final int tractorTagId = tagsHelper.insertTag("Tractor");
      final int barnTagId = tagsHelper.insertTag("Barn");

      final int planId = merlinHelper.insertPlan(missionModelId);
      final int sourceActivityId = merlinHelper.insertActivity(planId);
      final int targetActivityId = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(sourceActivityId, planId, farmTagId);
      tagsHelper.assignTagToActivity(targetActivityId, planId, farmTagId);
      final int branchId = duplicatePlan(planId, "Tags Modify Delete");

      tagsHelper.assignTagToActivity(sourceActivityId, planId, tractorTagId);
      tagsHelper.assignTagToActivity(sourceActivityId, branchId, barnTagId);

      tagsHelper.assignTagToActivity(targetActivityId, planId, tractorTagId);
      tagsHelper.removeTagFromActivity(targetActivityId, branchId, farmTagId);

      final int mergeRQ = createMergeRequest(planId, branchId);
      beginMerge(mergeRQ);
      setResolution(mergeRQ, sourceActivityId, "receiving");
      setResolution(mergeRQ, targetActivityId, "supplying");
      commitMerge(mergeRQ);

      final var activities = getActivities(planId);
      assertEquals(2, activities.size());

      final var expectedSourceTags = new ArrayList<Tag>();
      expectedSourceTags.add(new Tag(farmTagId, "Farm", null, -1));
      expectedSourceTags.add(new Tag(tractorTagId, "Tractor", null, -1));

      final var expectedTargetTags = new ArrayList<Tag>();

      assertEquals(expectedSourceTags, tagsHelper.getTagsOnActivity(sourceActivityId, planId));
      assertEquals(expectedTargetTags, tagsHelper.getTagsOnActivity(targetActivityId, planId));
    }

    // If the tag in a snapshot is deleted mid-merge, it should not be present in the final activity directive
    @Test
    void postMergeTagIsAbsentIfTagDeleted() throws SQLException {
      final int farmTagId = tagsHelper.insertTag("Farm");
      final int tractorTagId = tagsHelper.insertTag("Tractor");

      final int planId = merlinHelper.insertPlan(missionModelId);
      final int oneTagActivityId = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(oneTagActivityId, planId, farmTagId);
      final int noTagsActivityId = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(noTagsActivityId, planId, farmTagId);
      final int branchId = duplicatePlan(planId, "Tags Modify Delete");
      tagsHelper.assignTagToActivity(oneTagActivityId, branchId, tractorTagId);

      final int mergeRQ = createMergeRequest(planId, branchId);
      beginMerge(mergeRQ);
      tagsHelper.deleteTag(farmTagId);
      commitMerge(mergeRQ);

      final var expectedTags = new ArrayList<Tag>();
      expectedTags.add(new Tag(tractorTagId, "Tractor", null, -1));

      assertEquals(expectedTags, tagsHelper.getTagsOnActivity(oneTagActivityId, planId));
      assertEquals(new ArrayList<Tag>(), tagsHelper.getTagsOnActivity(noTagsActivityId, planId));
    }

    // Tags do not get shuffled during merge
    // Multi-activity test
    @Test
    void tagsAreNotShuffledDuringMerge() throws SQLException {
      /*
      Activity Cases:
      0. Unchanged activity, no tag
      1. Unchanged activity, tag
      2. Modified activity, added tag (no tag)
      3. Modified activity, added tag (had tag)
      4. Modified activity, removed tag
      5. Deleted activity (no tag)
      6. Deleted activity (had tag)
      7. Added activity, no tag
      8. Added activity, tag
       */
      final int farmTagId = tagsHelper.insertTag("Farm");
      final Tag farmTag = new Tag(farmTagId, "Farm", null, -1);
      final int tractorTagId = tagsHelper.insertTag("Tractor");
      final Tag tractorTag = new Tag(tractorTagId, "Tractor", null, -1);
      final int barnTagId = tagsHelper.insertTag("Barn");
      final Tag barnTag = new Tag(barnTagId, "Barn", null, -1);

      final int planId = merlinHelper.insertPlan(missionModelId);

      final int case0Id = merlinHelper.insertActivity(planId);
      final int case1Id = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(case1Id, planId, farmTagId);
      final int case2Id = merlinHelper.insertActivity(planId);
      final int case3Id = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(case3Id, planId, tractorTagId);
      final int case4Id = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(case4Id, planId, tractorTagId);
      final int case5Id = merlinHelper.insertActivity(planId);
      final int case6Id = merlinHelper.insertActivity(planId);
      tagsHelper.assignTagToActivity(case6Id, planId, barnTagId);

      final int branchId = duplicatePlan(planId, "MultiTags Test");
      final int case7Id = merlinHelper.insertActivity(branchId);
      final int case8Id = merlinHelper.insertActivity(branchId);
      tagsHelper.assignTagToActivity(case8Id, branchId, barnTagId);
      deleteActivityDirective(branchId, case5Id);
      deleteActivityDirective(branchId, case6Id);

      tagsHelper.assignTagToActivity(case2Id, planId, barnTagId);
      tagsHelper.assignTagToActivity(case3Id, branchId, barnTagId);
      tagsHelper.removeTagFromActivity(case4Id, planId, tractorTagId);

      final int mergeRQ = createMergeRequest(planId, branchId);
      beginMerge(mergeRQ);
      commitMerge(mergeRQ);

      final var activities = getActivities(planId);
      activities.sort(Comparator.comparingInt(a -> a.activityId));
      assertEquals(7, activities.size());

      assertEquals(case0Id, activities.get(0).activityId);
      assertTrue(tagsHelper.getTagsOnActivity(case0Id, planId).isEmpty());

      final var case1Tags = new ArrayList<Tag>();
      case1Tags.add(farmTag);
      assertEquals(case1Id, activities.get(1).activityId);
      assertEquals(case1Tags, tagsHelper.getTagsOnActivity(case1Id, planId));

      final var case2Tags = new ArrayList<Tag>();
      case2Tags.add(barnTag);
      assertEquals(case2Id, activities.get(2).activityId);
      assertEquals(case2Tags, tagsHelper.getTagsOnActivity(case2Id, planId));

      final var case3Tags = new ArrayList<Tag>();
      case3Tags.add(tractorTag);
      case3Tags.add(barnTag);
      assertEquals(case3Id, activities.get(3).activityId);
      assertEquals(case3Tags, tagsHelper.getTagsOnActivity(case3Id, planId));

      assertEquals(case4Id, activities.get(4).activityId);
      assertTrue(tagsHelper.getTagsOnActivity(case4Id, planId).isEmpty());

      assertEquals(case7Id, activities.get(5).activityId);
      assertTrue(tagsHelper.getTagsOnActivity(case7Id, planId).isEmpty());

      final var case8Tags = new ArrayList<Tag>();
      case8Tags.add(barnTag);
      assertEquals(case8Id, activities.get(6).activityId);
      assertEquals(case8Tags, tagsHelper.getTagsOnActivity(case8Id, planId));
    }
  }
}
