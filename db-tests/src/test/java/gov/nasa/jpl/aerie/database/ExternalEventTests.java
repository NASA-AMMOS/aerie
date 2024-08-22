package gov.nasa.jpl.aerie.database;

import gov.nasa.jpl.aerie.database.PlanCollaborationTests.Activity;
import org.junit.jupiter.api.*;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGobject;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.SQLData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import gov.nasa.jpl.aerie.database.MerlinDatabaseTestHelper.ExternalEvent;
import gov.nasa.jpl.aerie.database.MerlinDatabaseTestHelper.ExternalSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("SqlSourceToSinkFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExternalEventTests {
  private DatabaseTestHelper helper;
  private MerlinDatabaseTestHelper merlinHelper;
  private Connection connection;

  @AfterEach
  void afterEach() throws SQLException {
    helper.clearSchema("merlin");
  }

  @BeforeAll
  void beforeAll() throws SQLException, IOException, InterruptedException {
    helper = new DatabaseTestHelper("external_event", "Activity Directive Changelog Tests");
    connection = helper.connection();
    merlinHelper = new MerlinDatabaseTestHelper(connection);
  }

  @AfterAll
  void afterAll() throws SQLException, IOException, InterruptedException {
    helper.close();
    connection = null;
    helper = null;
  }


  private record ExternalEventType(String name){}
  private record ExternalSourceType(String name){}
  private record DerivationGroup(String name, String source_type_name){}
  private record DerivedExternalEvent(String source_key, String derivation_group_name, String event_key, String event_type_name, String start_time, String source_range, String valid_at){}

  void upload_source() throws SQLException {
    try (final var statement = connection.createStatement()) {
      // insert external source types
      statement.executeUpdate(
          // language=sql
          """
          INSERT INTO merlin.external_source_type VALUES ('Derivation Test');
          """
      );

      // insert external event types
      statement.executeUpdate(
          // language=sql
          """
          INSERT INTO merlin.external_event_type VALUES ('DerivationD');
          INSERT INTO merlin.external_event_type VALUES ('DerivationC');
          INSERT INTO merlin.external_event_type VALUES ('DerivationB');
          INSERT INTO merlin.external_event_type VALUES ('DerivationA');
          """
      );

      // insert derivation groups
      statement.executeUpdate(
          // language=sql
          """
          INSERT INTO merlin.derivation_group VALUES ('Derivation Test Default', 'Derivation Test');
          """
      );

      // insert external sources
      statement.executeUpdate(
          // language=sql
          """
          INSERT INTO merlin.external_source VALUES ('Derivation_Test_00.json', 'Derivation Test', 'Derivation Test Default', '2024-01-18 00:00:00+00', '2024-01-05 00:00:00+00', '2024-01-11 00:00:00+00', '2024-08-21 22:36:12.858009+00', '{}');
          INSERT INTO merlin.external_source VALUES ('Derivation_Test_01.json', 'Derivation Test', 'Derivation Test Default', '2024-01-19 00:00:00+00', '2024-01-01 00:00:00+00', '2024-01-07 00:00:00+00', '2024-08-21 22:36:19.381275+00', '{}');
          INSERT INTO merlin.external_source VALUES ('Derivation_Test_02.json', 'Derivation Test', 'Derivation Test Default', '2024-01-20 00:00:00+00', '2024-01-03 00:00:00+00', '2024-01-10 00:00:00+00', '2024-08-21 22:36:23.340941+00', '{}');
          INSERT INTO merlin.external_source VALUES ('Derivation_Test_03.json', 'Derivation Test', 'Derivation Test Default', '2024-01-21 00:00:00+00', '2024-01-01 12:00:00+00', '2024-01-02 12:00:00+00', '2024-08-21 22:36:28.365244+00', '{}');
          """
      );

      // insert external events
      statement.executeUpdate(
          // language=sql
          """
          INSERT INTO merlin.external_event VALUES ('2', 'DerivationD', 'Derivation_Test_00.json', 'Derivation Test Default', '2024-01-05 23:00:00+00', '01:10:00', '{"notes": "subsumed by test 01, even though end lies outside of 01, also replaced by test 01 by key", "rules": [3, 4], "should_present": false}');
          INSERT INTO merlin.external_event VALUES ('7', 'DerivationC', 'Derivation_Test_00.json', 'Derivation Test Default', '2024-01-09 23:00:00+00', '02:00:00', '{"notes": "subsumed by test 02, even though end lies outside of 02, because start time during 01", "rules": [3], "should_present": false}');
          INSERT INTO merlin.external_event VALUES ('8', 'DerivationB', 'Derivation_Test_00.json', 'Derivation Test Default', '2024-01-10 11:00:00+00', '01:05:00', '{"notes": "after everything, subsumed by nothing despite being from oldest file", "rules": [1], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('1', 'DerivationA', 'Derivation_Test_01.json', 'Derivation Test Default', '2024-01-01 00:00:00+00', '02:10:00', '{"notes": "before everything, subsumed by nothing", "rules": [1], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('2', 'DerivationA', 'Derivation_Test_01.json', 'Derivation Test Default', '2024-01-01 12:00:00+00', '02:10:00', '{"notes": "overwritten by key in later file, even with type change", "rules": [4], "should_present": false}');
          INSERT INTO merlin.external_event VALUES ('3', 'DerivationB', 'Derivation_Test_01.json', 'Derivation Test Default', '2024-01-02 23:00:00+00', '03:00:00', '{"notes": "starts before next file though occurs during next file, still included", "rules": [2], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('4', 'DerivationB', 'Derivation_Test_01.json', 'Derivation Test Default', '2024-01-05 21:00:00+00', '03:00:00', '{"notes": "start subsumed by 02, not included in final result", "rules": [3], "should_present": false}');
          INSERT INTO merlin.external_event VALUES ('5', 'DerivationC', 'Derivation_Test_02.json', 'Derivation Test Default', '2024-01-05 23:00:00+00', '01:10:00', '{"notes": "not subsumed, optionally change this event to have key 6 and ensure this test fails", "rules": [1], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('6', 'DerivationC', 'Derivation_Test_02.json', 'Derivation Test Default', '2024-01-06 12:00:00+00', '02:00:00', '{"notes": "not subsumed", "rules": [1], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('2', 'DerivationB', 'Derivation_Test_02.json', 'Derivation Test Default', '2024-01-09 11:00:00+00', '01:05:00', '{"notes": "replaces 2 in test 01, despite different event type", "rules": [4], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('9', 'DerivationC', 'Derivation_Test_03.json', 'Derivation Test Default', '2024-01-02 00:00:00+00', '01:00:00', '{"notes": "not subsumed", "rules": [1], "should_present": true}');
          """
      );
    }
  }

  // verify a simple upload works
  @Test
  void uploadWithoutError() {
    assertDoesNotThrow(this::upload_source);
  }

  // test the upload works by confirming derived events associated with upload perform as expected
  @Test
  void basicDerivedEvents() {
    // upload all source data
    assertDoesNotThrow(this::upload_source);

    // check that derived events in our prewritten case has the correct keys
    assertDoesNotThrow(() -> {
      final var statement = connection.createStatement();
      final var res = statement.executeQuery(
          // language-sql
          """
          SELECT * FROM merlin.derived_events ORDER BY start_time;
          """
      );

      String[] expected_keys = {"1", "9", "3", "5", "6", "2", "8"};
      ArrayList<String> resultRaw = new ArrayList<>();
      while (res.next()) {
        resultRaw.add(res.getString("event_key"));
      }

      assertEquals(expected_keys.length, resultRaw.size());
      for (int i = 0; i < expected_keys.length; i++) {
        assertEquals(expected_keys[i], resultRaw.get(i));
      }
    });
  }

  // test the upload works by confirming derivation_group_comp view associated with upload performs as expected
  // this test is rigorous enough for derivation_group_comp view.
  @Test
  void derivationGroupComp() {
    // upload all source data
    assertDoesNotThrow(this::upload_source);

    // check that derivation_group_comp has 1 entry, with 4 sources and 7 events
    assertDoesNotThrow(() -> {
      final var statement = connection.createStatement();
      final var res = statement.executeQuery(
          // language-sql
          """
          SELECT * FROM merlin.derivation_group_comp;
          """
      );

      // we expect only 1 result
      assertTrue(res.next());

      // res.getArray() doesn't give you a String[] array - have to do some manipulation to get its length as a result
      System.out.println(res.getArray("sources").toString());
      assertEquals(4, res.getArray("sources").toString().split("\",\"").length);
      assertEquals(7, res.getInt("derived_total"));

      // we expect only 1 result
      assertFalse(res.next());
    });
  }

  // verify deletion works without fail
  @Test
  void verifyDeletion() {
    // we will only rigorously test deletions in one order. testing in another order is logically equivalent to testing
    //    constraints, which we relegate to later tests. we follow the correct order of:
    //      - external_event
    //      - external_event_type
    //      - external_source
    //      - derivation_group
    //      - external_source_type
    // though it is possible to rearrange this order, so long as events are deleted before their types, sources deleted
    //    before their types but after events, and derivation groups deleted after linked sources removed but before
    //    source types removed
    assertDoesNotThrow(this::upload_source);
    assertDoesNotThrow(() -> {
      final var statement = connection.createStatement();

      statement.executeUpdate(
        // language-sql
        """
        DELETE FROM merlin.external_event;
        DELETE FROM merlin.external_event_type;
        DELETE FROM merlin.external_source;
        DELETE FROM merlin.derivation_group;
        DELETE FROM merlin.external_source_type;
        """
      );
    });

    // any other order throws an error; arbitrarily we delete external source types before derivation groups, breaking them:
    assertDoesNotThrow(this::upload_source);
    assertThrows(SQLException.class, () -> {
      final var statement = connection.createStatement();

      statement.executeUpdate(
          // language-sql
          """
          DELETE FROM merlin.external_event;
          DELETE FROM merlin.external_event_type;
          DELETE FROM merlin.external_source;
          DELETE FROM merlin.external_source_type;
          DELETE FROM merlin.derivation_group;
          """
      );
    });
  }

  // test each derived event rule individually
  //  + rule 1: An External Event superceded by nothing will be present in the final, derived result.
  //     - test solitary event, test event with stuff occurring before and after
  //  + rule 2: An External Event partially superceded by a later External Source, but whose start time occurs before
  //            the start of said External Source(s), will be present in the final, derived result.
  //  + rule 3: An External Event whose start is superseded by another External Source, even if its end occurs after the
  //            end of said External Source, will be replaced by the contents of that External Source (whether they are
  //            blank spaces, or other events).
  //  + rule 4: An External Event who shares an ID with an External Event in a later External Source will always be
  //            replaced.
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class DerivedEventsTests {

    ExternalSource defaultES = new ExternalSource(
        "rule_test.json",
        "RuleTest",
        "RuleTest Default",
        "2024-01-01T00:00:00Z",
        "2024-01-01T00:00:00Z",
        "2024-01-07T00:00:00Z",
        "2024-01-01T00:00:00Z",
        "{}"
    );

    // quicker external event creator, leveraging constants from defaultES
    ExternalEvent createEvent(String key, String start_time, String duration, String properties) {
      return new ExternalEvent(
          key,
          "RuleTest",
          "rule_test.json",
          "RuleTest Default",
          start_time,
          duration,
          properties
      );
    }

    ////////////////////////// RULE 1 //////////////////////////
    // test a solitary event, it vacuously shouldn't get superceded
    @Test
    void rule1_solitary() {
      assertDoesNotThrow(() -> {
        ExternalEvent e = createEvent("1", "2024-01-01T01:00:00Z", "01:00:00", "{}");
        merlinHelper.insertTypesForEvent(e, defaultES);
      });

      assertDoesNotThrow(() -> {
        final var statement = connection.createStatement();
        final var res = statement.executeQuery(
            // language-sql
            """
            SELECT * FROM merlin.derived_events;
            """
        );

        // only 1 result expected in set, it isn't superceded at all
        assertTrue(res.next());
        assertEquals("1", res.getString("event_key"));
        assertFalse(res.next());
      });
    }
  }


  // test all constraints (namely duplicates and deletions)
  // extra test for derived events that manages several derivation groups

}
