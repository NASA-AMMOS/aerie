package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TagsTests {
  private static final File initSqlScriptFile = new File("../merlin-server/sql/merlin/init.sql");
  private final String constraintDefinition = "export default (): Constraint => Real.Resource(\"/fruit\").equal(Real.Resource(\"/peel\"))";
  private DatabaseTestHelper helper;
  private MerlinDatabaseTestHelper merlinHelper;

  private Connection connection;
  int fileId;
  int missionModelId;
  int tagId;

  void setConnection(DatabaseTestHelper helper) {
    connection = helper.connection();
  }

  @BeforeEach
  void beforeEach() throws SQLException {
    fileId = merlinHelper.insertFileUpload();
    missionModelId = merlinHelper.insertMissionModel(fileId);
    tagId = insertTag("Farm");
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
    setConnection(helper);
    merlinHelper = new MerlinDatabaseTestHelper(connection);
  }

  @AfterAll
  void afterAll() throws SQLException, IOException, InterruptedException {
    helper.stopDatabase();
    connection = null;
    helper = null;
  }

  //region Helper Functions
  int insertTag(String name) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          """
              INSERT INTO metadata.tags (name, owner)
              VALUES ('%s', -1)
              RETURNING id;
              """.formatted(name)
      );
      res.next();
      return res.getInt("id");
    }
  }

  int insertTag(String name, String color) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          """
              INSERT INTO metadata.tags (name, color, owner)
              VALUES ('%s', '%s', -1)
              RETURNING id;
              """.formatted(name, color)
      );
      res.next();
      return res.getInt("id");
    }
  }

  Tag updateTagColor(int tagId, String color) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          """
          UPDATE metadata.tags
          SET color = '%s'
          WHERE id = %d
          RETURNING id, name, color, owner
          """.formatted(color, tagId)
      );
      res.next();
      return new Tag(
          res.getInt("id"),
          res.getString("name"),
          res.getString("color"),
          res.getInt("owner")
      );
    }
  }

  Tag updateTagName(int tagId, String name) throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          """
          UPDATE metadata.tags
          SET name = '%s'
          WHERE id = %d
          RETURNING id, name, color, owner
          """.formatted(name, tagId)
      );
      res.next();
      return new Tag(
          res.getInt("id"),
          res.getString("name"),
          res.getString("color"),
          res.getInt("owner")
      );
    }
  }

  Tag getTag(int id) throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          """
          SELECT id, name, color, owner
          FROM metadata.tags
          WHERE id = %d;
          """.formatted(id)
      );
      res.next();
      return new Tag(
        res.getInt("id"),
        res.getString("name"),
        res.getString("color"),
        res.getInt("owner"));
    }
  }

  ArrayList<Tag> getAllTags() throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var tags = new ArrayList<Tag>();
      final var res = statement.executeQuery(
          """
          SELECT id, name, color, owner
          FROM metadata.tags;
          """
      );
      while(res.next()) {
        tags.add(new Tag(
            res.getInt("id"),
            res.getString("name"),
            res.getString("color"),
            res.getInt("owner")));
      }
      return tags;
    }
  }

  void deleteTag(int id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
          DELETE FROM metadata.tags
          WHERE id = %d;
          """.formatted(id));
    }
  }

  void assignTagToActivity(int directive_id, int plan_id, int tag_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
          INSERT INTO metadata.activity_directive_tags (plan_id, directive_id, tag_id)
          VALUES (%d, %d, %d)
          """.formatted(plan_id,directive_id,tag_id));
    }
  }

  void removeTagFromActivity(int directive_id, int plan_id, int tag_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
          DELETE FROM metadata.activity_directive_tags
          WHERE plan_id = %d
            AND directive_id = %d
            AND tag_id = %d;
          """.formatted(plan_id, directive_id, tag_id));
    }
  }

  ArrayList<Tag> getTagsOnActivity(int directive_id, int plan_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var tags = new ArrayList<Tag>();
      final var res = statement.executeQuery(
          """
              SELECT id, name, color, owner
              FROM metadata.tags t, metadata.activity_directive_tags adt
              WHERE adt.tag_id = t.id
                AND adt.plan_id = %d
                AND adt.directive_id = %d
              ORDER BY id;
              """.formatted(plan_id, directive_id));
      while (res.next()) {
        tags.add(new Tag(
            res.getInt("id"),
            res.getString("name"),
            res.getString("color"),
            res.getInt("owner")));
      }
      return tags;
    }
  }

  ArrayList<Tag> getTagsOnActivitySnapshot(int directive_id, int snapshot_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var tags = new ArrayList<Tag>();
      final var res = statement.executeQuery(
          """
          SELECT id, name, color, owner
          FROM metadata.tags t, metadata.snapshot_activity_tags sat
          WHERE sat.tag_id = t.id
            AND sat.snapshot_id = %d
            AND sat.directive_id = %d
          ORDER BY id;
          """.formatted(snapshot_id, directive_id));
      while (res.next()) {
        tags.add(new Tag(
            res.getInt("id"),
            res.getString("name"),
            res.getString("color"),
            res.getInt("owner")));
      }
      return tags;
    }
  }

  void assignTagToPlan(int plan_id, int tag_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
          INSERT INTO metadata.plan_tags (plan_id, tag_id)
          VALUES (%d, %d)
          """.formatted(plan_id,tag_id));
    }
  }

  void removeTagFromPlan(int plan_id, int tag_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
          DELETE FROM metadata.plan_tags
          WHERE plan_id = %d
            AND tag_id = %d;
          """.formatted(plan_id, tag_id));
    }
  }

  ArrayList<Tag> getTagsOnPlan(int plan_id) throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var tags = new ArrayList<Tag>();
      final var res = statement.executeQuery(
          """
          SELECT id, name, color, owner
          FROM metadata.tags t, metadata.plan_tags pt
          WHERE pt.tag_id = t.id
            AND pt.plan_id = %d
          ORDER BY id;
          """.formatted(plan_id));
      while(res.next()) {
        tags.add(new Tag(
            res.getInt("id"),
            res.getString("name"),
            res.getString("color"),
            res.getInt("owner")));
      }
      return tags;
    }
  }

  void assignTagToConstraint(int constraint_id, int tag_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
          INSERT INTO metadata.constraint_tags (constraint_id, tag_id)
          VALUES (%d, %d)
          """.formatted(constraint_id, tag_id));
    }
  }

  void removeTagFromConstraint(int constraint_id, int tag_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          """
              DELETE FROM metadata.constraint_tags
              WHERE constraint_id = %d
                AND tag_id = %d;
              """.formatted(constraint_id, tag_id));
    }
  }

  ArrayList<Tag> getTagsOnConstraint(int constraint_id) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var tags = new ArrayList<Tag>();
      final var res = statement.executeQuery(
          """
              SELECT id, name, color, owner
              FROM metadata.tags t, metadata.constraint_tags ct
              WHERE ct.tag_id = t.id
                AND ct.constraint_id = %d
              ORDER BY id;
              """.formatted(constraint_id));
      while (res.next()) {
        tags.add(new Tag(
            res.getInt("id"),
            res.getString("name"),
            res.getString("color"),
            res.getInt("owner")));
      }
      return tags;
    }
  }

  int assignTagToActivityType(int modelId, String name, int tagId) throws SQLException{
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery("""
        UPDATE public.activity_type at
        SET subsystem = %d
        WHERE at.model_id = %d
          AND at.name = '%s'
        RETURNING subsystem;
        """.formatted(tagId, modelId, name));
      assertTrue(res.next());
      final int subsystem = res.getInt("subsystem");
      assertFalse(res.next());
      return subsystem;
    }
  }

  void removeTagFromActivityType(int modelId, String name) throws SQLException {
     try (final var statement = connection.createStatement()) {
       final var res = statement.executeQuery(
         """
         UPDATE activity_type
          SET subsystem = null
          WHERE model_id = %d
          AND name = '%s'
          RETURNING subsystem
         """.formatted(missionModelId, "GrowBanana"));
       assertTrue(res.next());
       assertNull(res.getObject("subsystem"));
       assertFalse(res.next());
     }
  }
  //endregion

  //region Records
  record Tag(int id, String name, String color, int owner) {}
  //endregion

  @Test
  void tagsAssociateCorrectly() throws SQLException {
    final var secondTagId = insertTag("Banana");
    final var planId = merlinHelper.insertPlan(missionModelId);
    final var activityId = merlinHelper.insertActivity(planId);
    final var constraintId = merlinHelper.insertConstraintPlan(planId, "Test Constraint", constraintDefinition);

    assignTagToPlan(planId, tagId);
    assignTagToActivity(activityId, planId, tagId);
    assignTagToConstraint(constraintId, tagId);

    assignTagToPlan(planId, secondTagId);

    final var planTags = getTagsOnPlan(planId);
    final var activityTags = getTagsOnActivity(activityId, planId);
    final var constraintTags = getTagsOnConstraint(constraintId);

    final ArrayList<Tag> expected = new ArrayList<>();
    expected.add(new Tag(tagId, "Farm", null, -1));

    final ArrayList<Tag> expectedPlan = new ArrayList<>(expected);
    expectedPlan.add(new Tag(secondTagId, "Banana", null, -1));


    assertEquals(expectedPlan, planTags);
    assertEquals(expected, activityTags);
    assertEquals(expected, constraintTags);
  }

  @Test
  void tagsDissociateCorrectly() throws SQLException {
    final var secondTagId = insertTag("Banana");
    final var planId = merlinHelper.insertPlan(missionModelId);
    final var activityId = merlinHelper.insertActivity(planId);
    final var constraintId = merlinHelper.insertConstraintPlan(planId, "Test Constraint", constraintDefinition);

    assignTagToPlan(planId, tagId);
    assignTagToActivity(activityId, planId, tagId);
    assignTagToConstraint(constraintId, tagId);

    assignTagToPlan(planId, secondTagId);

    removeTagFromPlan(planId, tagId);
    assertDoesNotThrow(() -> getTag(secondTagId));
    final var planTags = getTagsOnPlan(planId);
    final var activityTags = getTagsOnActivity(activityId, planId);
    final var constraintTags = getTagsOnActivity(activityId, planId);

    final var expected = new ArrayList<Tag>(1);
    expected.add(new Tag(tagId, "Farm", null, -1));

    final var expectedPlan = new ArrayList<Tag>(1);
    expectedPlan.add(new Tag(secondTagId, "Banana", null, -1));

    assertEquals(expectedPlan, planTags);
    assertEquals(expected, activityTags);
    assertEquals(expected, constraintTags);
  }

  @Test
  void cannotApplyNonexistentTag() throws SQLException {
    // Plan
    final var planId = merlinHelper.insertPlan(missionModelId);
    try {
      assignTagToPlan(planId, -1);
    } catch (SQLException e) {
      if(!e.getMessage().contains("insert or update on table \"plan_tags\" violates foreign key constraint \"plan_tags_tag_id_fkey\"")) {
        throw e;
      }
    }
    // Activity
    final var activityId = merlinHelper.insertActivity(planId);
    try {
      assignTagToActivity(activityId, planId, -1);
    } catch (SQLException e) {
      if(!e.getMessage().contains("insert or update on table \"activity_directive_tags\" violates foreign key constraint \"activity_directive_tags_tag_id_fkey\"")) {
        throw e;
      }
    }
    // Constraint
    final var constraintId = merlinHelper.insertConstraintPlan(planId, "Test Constraint", constraintDefinition);
    try {
      assignTagToConstraint(constraintId, -1);
    } catch (SQLException e) {
      if(!e.getMessage().contains("insert or update on table \"constraint_tags\" violates foreign key constraint \"constraint_tags_tag_id_fkey\"")) {
        throw e;
      }
    }
    // Activity Type
    merlinHelper.insertActivityType(missionModelId, "GrowBanana");
    try {
      assignTagToActivityType(missionModelId, "GrowBanana", -1);
    } catch (SQLException e) {
      if(!e.getMessage().contains("insert or update on table \"activity_type\" violates foreign key constraint \"activity_type_subsystem_fkey\"")) {
        throw e;
      }
    }
  }

  @Test
  void tagDeleteCascadesInMerlin() throws SQLException {
    final var secondTagId = insertTag("Banana");
    final var planId = merlinHelper.insertPlan(missionModelId);
    final var activityId = merlinHelper.insertActivity(planId);
    final var constraintId = merlinHelper.insertConstraintPlan(planId, "Test Constraint", constraintDefinition);

    assignTagToPlan(planId, tagId);
    assignTagToActivity(activityId, planId, tagId);
    assignTagToConstraint(constraintId, tagId);

    assignTagToPlan(planId, secondTagId);
    assignTagToActivity(activityId, planId, secondTagId);
    assignTagToConstraint(constraintId, secondTagId);

    deleteTag(tagId);

    final var planTags = getTagsOnPlan(planId);
    final var activityTags = getTagsOnActivity(activityId, planId);
    final var constraintTags = getTagsOnConstraint(constraintId);

    final ArrayList<Tag> expected = new ArrayList<>();
    expected.add(new Tag(secondTagId, "Banana", null, -1));

    assertEquals(expected, planTags);
    assertEquals(expected, activityTags);
    assertEquals(expected, constraintTags);
  }

  @Test
  void tagNamesAreUnique() throws SQLException {
    final Tag insertedTag = getTag(tagId);
    try {
      insertTag(insertedTag.name);
    } catch (SQLException e) {
      if(!e.getMessage().contains("duplicate key value violates unique constraint \"tags_name_key\"")){
        throw e;
      }
    }
    final int secondTagId = insertTag("Banana");
    try {
      updateTagName(secondTagId, insertedTag.name);
    } catch (SQLException e) {
      if(!e.getMessage().contains("duplicate key value violates unique constraint \"tags_name_key\"")){
        throw e;
      }
    }
  }

  @Test
  void tagColorMustBeHexOrNull() throws SQLException {
    assertDoesNotThrow(()->insertTag("Null Color Tag"));
    assertDoesNotThrow(()->insertTag("Hex Color Tag", "#ad6ef5"));
    assertDoesNotThrow(()->insertTag("Uppercase Hex Color Tag", "#AD6EF5"));
    final var javaColorString = String.format("#%02x%02x%02x", Color.PINK.getRed(), Color.PINK.getBlue(), Color.PINK.getGreen());
    assertDoesNotThrow(()->insertTag("Java Colors Tag", javaColorString));
    try {
      insertTag("Invalid Color", "bad color :(");
    } catch (SQLException e) {
      if (!e.getMessage()
          .contains("new row for relation \"tags\" violates check constraint \"color_is_hex_format\"\n")) {
        throw e;
      }
    }
    try {
      updateTagColor(tagId, "#######");
    } catch (SQLException e) {
      if (!e.getMessage()
          .contains("new row for relation \"tags\" violates check constraint \"color_is_hex_format\"\n")) {
        throw e;
      }
    }

  }

  @Test
  void subsystemTagRestrictsDelete() throws SQLException {
    merlinHelper.insertActivityType(missionModelId, "GrowBanana");
    // Assert Association
    final var subsystem = assignTagToActivityType(missionModelId, "GrowBanana", tagId);
    assertEquals(tagId, subsystem);

    // Attempt Delete
    try {
      deleteTag(tagId);
    } catch (SQLException e) {
      if(!e.getMessage().contains("update or delete on table \"tags\" violates foreign key constraint "
                                  + "\"activity_type_subsystem_fkey\" on table \"activity_type\"")) {
        throw e;
      }
    }

    // Clear Association
    removeTagFromActivityType(missionModelId, "GrowBanana");

    deleteTag(tagId);
  }
}
