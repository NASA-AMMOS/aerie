package gov.nasa.jpl.aerie.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.UUID;

@SuppressWarnings("SqlSourceToSinkFlow")
final class MerlinDatabaseTestHelper {
  private final Connection connection;
  final User admin;
  final User user;
  final User viewer;

  MerlinDatabaseTestHelper(Connection connection) throws SQLException {
    this.connection = connection;
    admin = insertUser("MerlinAdmin");
    user = insertUser("MerlinUser", "user");
    viewer = insertUser("MerlinViewer", "viewer");
  }

  record User(String name, String defaultRole, String session) {}
  protected record ExternalEvent(String key, String event_type_name, String source_key, String derivation_group_name, String start_time, String duration, String properties) {}
  protected record ExternalSource(String key, String source_type_name, String derivation_group_name, String valid_at, String start_time, String end_time, String created_at, String metadata){}

  User insertUser(final String username) throws SQLException {
    return insertUser(username, "aerie_admin");
  }

  User insertUser(final String username, final String defaultRole) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
            //language=sql
            """
            INSERT INTO permissions.users (username, default_role)
            VALUES ('%s', '%s');
            """.formatted(username, defaultRole)
          );
    }
    return new User(
        username,
        defaultRole,
        "{ \"x-hasura-user-id\": \"%s\", \"x-hasura-default-role\": \"%s\" }"
            .formatted(username, defaultRole));
  }

  int insertFileUpload() throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              //language=sql
              """
              INSERT INTO merlin.uploaded_file (path, name)
              VALUES ('test-path', 'test-name-%s')
              RETURNING id;
              """.formatted(UUID.randomUUID().toString())
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertMissionModel(final int fileId) throws SQLException {
    return insertMissionModel(fileId, admin.name);
  }

  int insertMissionModel(final int fileId, final String username) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              //language=sql
              """
              INSERT INTO merlin.mission_model (name, mission, owner, version, jar_id)
              VALUES ('test-mission-model-%s', 'test-mission', '%s', '0', %s)
              RETURNING id;
              """.formatted(UUID.randomUUID().toString(), username, fileId)
          );
      res.next();
      return res.getInt("id");
    }
  }

  int insertPlan(final int missionModelId) throws SQLException {
    return insertPlan(missionModelId, admin.name);
  }

  int insertPlan(final int missionModelId, final String username) throws SQLException {
    return insertPlan(missionModelId, username, "test-plan-"+UUID.randomUUID(), "2020-1-1 00:00:00+00");
  }

  int insertPlan(final int missionModelId, final String username, final  String planName) throws SQLException {
    return insertPlan(missionModelId, username, planName, "2020-1-1 00:00:00+00");
  }

  int insertPlan(final int missionModelId, final String username, final String planName, final String start_time) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          //language=sql
          """
          INSERT INTO merlin.plan (name, model_id, duration, start_time, owner)
          VALUES ('%s', '%s', '0', '%s', '%s')
          RETURNING id;
          """.formatted(planName, missionModelId, start_time, username));
      res.next();
      return res.getInt("id");
    }
  }

  void deletePlan(final int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          //language=sql
          """
          DELETE FROM merlin.plan
          WHERE id = %d;
          """.formatted(planId));
    }
  }

  void insertPlanCollaborator(final int planId, final String username) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.execute(
          //language=sql
          """
          INSERT INTO merlin.plan_collaborators (plan_id, collaborator)
          VALUES (%d, '%s');
          """.formatted(planId, username)
      );
    }
  }

  int insertActivity(final int planId) throws SQLException {
    return insertActivity(planId, "00:00:00");
  }

  int insertActivity(final int planId, final String startOffset) throws SQLException {
    return insertActivity(planId, startOffset, "{}");
  }

  int insertActivity(final int planId, final String startOffset, final String arguments) throws SQLException {
    return insertActivity(planId, startOffset, arguments, admin);
  }

  int insertActivity(final int planId, final String startOffset, final String arguments, User user) throws SQLException {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              //language=sql
              """
              INSERT INTO merlin.activity_directive (type, plan_id, start_offset, arguments, last_modified_by, created_by)
              VALUES ('test-activity', '%s', '%s', '%s', '%s', '%s')
              RETURNING id;
              """.formatted(planId, startOffset, arguments, user.name, user.name)
          );

      res.next();
      return res.getInt("id");
    }
  }

  void updateActivityName(String newName, int activityId, int planId) throws SQLException {
    try(final var statement = connection.createStatement()) {
      statement.execute(
          //language=sql
          """
          update merlin.activity_directive
          set name = '%s'
          where id = %d and plan_id = %d;
          """.formatted(newName, activityId, planId));
    }
  }

  void deleteActivityDirective(final int planId, final int activityId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      statement.executeUpdate(
        //language=sql
        """
        delete from merlin.activity_directive where id = %s and plan_id = %s
        """.formatted(activityId, planId));
    }
  }

  /**
   * To anchor an activity to the plan, set "anchorId" equal to -1.
   */
  void setAnchor(int anchorId, boolean anchoredToStart, int activityId, int planId) throws SQLException {
    try (final var statement = connection.createStatement()) {
      if (anchorId == -1) {
        statement.execute(
            //language=sql
            """
            update merlin.activity_directive
            set anchor_id = null,
                anchored_to_start = %b
            where id = %d and plan_id = %d;
            """.formatted(anchoredToStart, activityId, planId));
      } else {
        statement.execute(
            //language=sql
            """
            update merlin.activity_directive
              set anchor_id = %d,
                  anchored_to_start = %b
            where id = %d and plan_id = %d;
            """.formatted(anchorId, anchoredToStart, activityId, planId));
      }
    }
  }

  void insertActivityType(final int modelId, final String name) throws SQLException {
    try(final var statement = connection.createStatement()) {
      statement.execute(
          //language=sql
          """
          INSERT INTO merlin.activity_type (model_id, name, parameters, required_parameters, computed_attributes_value_schema)
          VALUES (%d, '%s', '{}', '[]', '{}');
          """.formatted(modelId, name)
      );
    }
  }

  int insertPreset(int modelId, String name, String associatedActivityType) throws SQLException {
    return insertPreset(modelId, name, associatedActivityType, admin.name, "{}");
  }

  int insertPreset(int modelId, String name, String associatedActivityType, String username)
  throws SQLException
  {
    return insertPreset(modelId, name, associatedActivityType, username, "{}");
  }

  int insertPreset(int modelId, String name, String associatedActivityType, String username, String arguments)
  throws SQLException
  {
    try (final var statement = connection.createStatement()) {
      final var res = statement
          .executeQuery(
              //language=sql
              """
              INSERT INTO merlin.activity_presets (model_id, name, associated_activity_type, arguments, owner)
              VALUES (%d, '%s', '%s', '%s', '%s')
              RETURNING id;
              """.formatted(modelId, name, associatedActivityType, arguments, username)
          );
      res.next();
      return res.getInt("id");
    }
  }

  void assignPreset(int presetId, int activityId, int planId, String userSession) throws SQLException {
    try(final var statement = connection.createStatement()){
      statement.execute(
         //language=sql
         """
         select hasura.apply_preset_to_activity(%d, %d, %d, '%s'::json);
         """.formatted(presetId, activityId, planId, userSession));
    }
  }

void unassignPreset(int presetId, int activityId, int planId) throws SQLException {
    try(final var statement = connection.createStatement()){
      statement.execute(
         //language=sql
         """
         delete from merlin.preset_to_directive
         where (preset_id, activity_id, plan_id) = (%d, %d, %d);
         """.formatted(presetId, activityId, planId));
    }
  }


  int insertConstraint(String name, String definition, User user) throws SQLException {
    try(final var statement = connection.createStatement()) {
      final var res = statement.executeQuery(
          //language=sql
          """
          WITH metadata(id, owner) AS (
            INSERT INTO merlin.constraint_metadata(name, description, owner, updated_by)
            VALUES ('%s', 'Merlin DB Test Constraint', '%s', '%s')
            RETURNING id, owner
          )
          INSERT INTO merlin.constraint_definition(constraint_id, definition, author)
          SELECT m.id, '%s', m.owner
          FROM metadata m
          RETURNING constraint_id;
          """.formatted(name, user.name, user.name, definition));
      res.next();
      return res.getInt("constraint_id");
    }
  }

  public void insertTypesForEvent(ExternalEvent externalEvent, ExternalSource externalSource) throws SQLException {
    try(final var statement = connection.createStatement()) {
      // create the event type
      statement.executeUpdate(
          // language-sql
          """
          INSERT INTO
            merlin.external_event_type
          VALUES ('%s')
          ON CONFLICT(name) DO NOTHING;
          """.formatted(externalEvent.event_type_name)
      );

      // create the source type
      statement.executeUpdate(
          // language-sql
          """
          INSERT INTO
            merlin.external_source_type
          VALUES ('%s')
          ON CONFLICT(name) DO NOTHING;
          """.formatted(externalSource.source_type_name)
      );

      // create the derivation_group
      statement.executeUpdate(
          // language-sql
          """
          INSERT INTO
            merlin.derivation_group
          VALUES ('%s', '%s')
          ON CONFLICT(name, source_type_name) DO NOTHING;
          """.formatted(externalEvent.derivation_group_name, externalSource.source_type_name)
      );

      // create the source
      statement.executeUpdate(
          // language-sql
          """
          INSERT INTO
            merlin.external_source
          VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
          ON CONFLICT(key, derivation_group_name) DO NOTHING;
          """.formatted(
              externalSource.key,
              externalSource.source_type_name,
              externalSource.derivation_group_name,
              externalSource.valid_at,
              externalSource.start_time,
              externalSource.end_time,
              externalSource.created_at,
              externalSource.metadata
          )
      );

      // create the event
      statement.executeUpdate(
          // language-sql
          """
          INSERT INTO
            merlin.external_event
          VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s')
          ON CONFLICT(key, source_key, derivation_group_name, event_type_name) DO NOTHING;
          """.formatted(
              externalEvent.key,
              externalEvent.event_type_name,
              externalEvent.source_key,
              externalEvent.derivation_group_name,
              externalEvent.start_time,
              externalEvent.duration,
              externalEvent.properties
          )
      );

    }
  }

  // borrowed directly from: https://stackoverflow.com/questions/24229442/print-the-data-in-resultset-along-with-column-names
  // useful in making new tests
  public static void printRows(ResultSet resultSet) throws SQLException {
    ResultSetMetaData rsmd = resultSet.getMetaData();
    int columnsNumber = rsmd.getColumnCount();
    while (resultSet.next()) {
      for (int i = 1; i <= columnsNumber; i++) {
        if (i > 1) System.out.print(",  ");
        String columnValue = resultSet.getString(i);
        System.out.print(columnValue + " " + rsmd.getColumnName(i));
      }
      System.out.println("");
    }
  }
}
