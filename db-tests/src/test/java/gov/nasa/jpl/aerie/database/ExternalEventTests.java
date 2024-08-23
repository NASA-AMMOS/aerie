package gov.nasa.jpl.aerie.database;

import gov.nasa.jpl.aerie.database.PlanCollaborationTests.Activity;
import org.junit.jupiter.api.*;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGobject;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import gov.nasa.jpl.aerie.database.MerlinDatabaseTestHelper.ExternalEvent;
import gov.nasa.jpl.aerie.database.MerlinDatabaseTestHelper.ExternalSource;
import org.postgresql.util.PSQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
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



  String st = "Test";
  String dg = "Test Default";
  String et = "Test";
  String mt = "{}";
  String ca = "2024-01-01T00:00:00Z";

  // quicker external event creator, leveraging constants from source
  ExternalEvent createEvent(String key, String start_time, String duration, ExternalSource source) {
    return new ExternalEvent(
        key,
        et,
        source.key(),
        source.derivation_group_name(),
        start_time,
        duration,
        mt
    );
  }

  void compareLists(String[] expected, ResultSet res, String key) throws SQLException {
    ArrayList<String> actual = new ArrayList<>();
    while (res.next()) {
      actual.add(res.getString(key));
    }

    assertEquals(expected.length, actual.size());
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actual.get(i));
    }
  }

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
      compareLists(expected_keys, res, "event_key");
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

  // test each derived event rule individually, starting with ranges of sources
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class DerivedEventsTests {

    // Test "empty" sources to make sure their overlapped windows work correctly.
    // We add events that span a very short duration simply so that the sources show up in derived_events, but we aren't
    //    testing any properties of said events.
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class DerivedSourcesTests {

      String st = "Test";
      String dg = "Test Default";
      String ca = "2024-01-01T00:00:00Z";
      String mt = "{}";
      String duration = "00:00:00.000001";

      /*
        Basic, non-overlapping case (gaps preserved):
           A:             ++++++++
           B:  +++++++
               BBBBBBB    AAAAAAAA
       */
      @Test
      void testSparseCoverage() {
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T03:00:00Z", "2024-01-01T04:00:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, a.start_time(), duration, mt);
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T01:00:00Z", "2024-01-01T02:00:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);

        assertDoesNotThrow(() -> {
          final var statement = connection.createStatement();
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);

          var res = statement.executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // verify the ranges for each result are as expected
          String[] expectedResults = {
              "{[\"2024-01-01 03:00:00+00\",\"2024-01-01 04:00:00+00\")}",
              "{[\"2024-01-01 01:00:00+00\",\"2024-01-01 02:00:00+00\")}"
          };
          compareLists(expectedResults, res, "source_range");
        });
      }

      /*
        If an event is succeeded before in time by a more valid source:
           A:     ++++++++
           B:  +++++++
               BBBBBBBAAAA
       */
      @Test
      void testForwardOverlap() {
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T02:00:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, "2024-01-01T1:10:00Z", duration, mt); // have to manually pick this
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);

        assertDoesNotThrow(() -> {
          final var statement = connection.createStatement();
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);

          var res = statement.executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // verify the ranges for each result are as expected
          String[] expectedResults = {
              "{[\"2024-01-01 01:00:00+00\",\"2024-01-01 02:00:00+00\")}",
              "{[\"2024-01-01 00:00:00+00\",\"2024-01-01 01:00:00+00\")}"
          };
          compareLists(expectedResults, res, "source_range");
        });
      }

      /*
        If an event is succeeded after in time by a more valid source:
           A:  +++++++
           B:     ++++++++
               AAABBBBBBBB
       */
      @Test
      void testBackwardOverlap() {
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, "2024-01-01T00:00:00Z", duration, mt); // have to manually pick this
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);

        assertDoesNotThrow(() -> {
          final var statement = connection.createStatement();
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);

          var res = statement.executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // verify the ranges for each result are as expected
          String[] expectedResults = {
              "{[\"2024-01-01 00:00:00+00\",\"2024-01-01 00:30:00+00\")}",
              "{[\"2024-01-01 00:30:00+00\",\"2024-01-01 01:00:00+00\")}"
          };
          compareLists(expectedResults, res, "source_range");
        });
      }

      /*
        A source in present before all sources, in a gap, should be ever present, even if chopped into several
          subintervals:
          A:      +++++++++++++++++++++
          B:  ++++++
          C:                +++++++
              BBBBBBAAAAAAAACCCCCCCAAAA
       */
      @Test
      void testBackground() {
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T03:00:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, "2024-01-01T01:10:00Z", duration, mt); // just need 1 that shows up and source range will still show correctly
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);
        ExternalSource c = new ExternalSource("C", st, dg, "2024-01-03T00:00:00Z", "2024-01-01T01:30:00Z", "2024-01-01T02:00:00Z", ca, mt);
        ExternalEvent cE = new ExternalEvent(c.key() + "_event", st, c.key(), dg, c.start_time(), duration, mt);

        assertDoesNotThrow(() -> {
          final var statement = connection.createStatement();
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);
          merlinHelper.insertTypesForEvent(cE, c);

          var res = statement.executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // verify the ranges for each result are as expected
          String[] expectedResults = {
              "{[\"2024-01-01 01:00:00+00\",\"2024-01-01 01:30:00+00\"),[\"2024-01-01 02:00:00+00\",\"2024-01-01 03:00:00+00\")}",
              "{[\"2024-01-01 00:00:00+00\",\"2024-01-01 01:00:00+00\")}",
              "{[\"2024-01-01 01:30:00+00\",\"2024-01-01 02:00:00+00\")}"
          };
          compareLists(expectedResults, res, "source_range");
        });

      }

      /*
        The above 4 are pretty conclusive. However, just as an overall, cumulative case:
          A:     ++++++++++++
          B:  ++++++
          C:                   ++++++
          D:    +++++++
          E:               +++++++
          F:      +++
          G:             +
              BBDDFFFDDAAGAEEEEEEECCC
              01234567890123456789012
       */
      @Test
      void testAmalgamation() {
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:03:00Z", "2024-01-01T00:15:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, "2024-01-01T00:09:10Z", duration, mt);
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T00:06:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);
        ExternalSource c = new ExternalSource("C", st, dg, "2024-01-03T00:00:00Z", "2024-01-01T00:17:00Z", "2024-01-01T00:23:00Z", ca, mt);
        ExternalEvent cE = new ExternalEvent(c.key() + "_event", st, c.key(), dg, "2024-01-01T00:21:00Z", duration, mt);
        ExternalSource d = new ExternalSource("D", st, dg, "2024-01-04T00:00:00Z", "2024-01-01T00:02:00Z", "2024-01-01T00:09:00Z", ca, mt);
        ExternalEvent dE = new ExternalEvent(d.key() + "_event", st, d.key(), dg, d.start_time(), duration, mt);
        ExternalSource e = new ExternalSource("E", st, dg, "2024-01-05T00:00:00Z", "2024-01-01T00:13:00Z", "2024-01-01T00:20:00Z", ca, mt);
        ExternalEvent eE = new ExternalEvent(e.key() + "_event", st, e.key(), dg, e.start_time(), duration, mt);
        ExternalSource f = new ExternalSource("F", st, dg, "2024-01-06T00:00:00Z", "2024-01-01T00:04:00Z", "2024-01-01T00:07:00Z", ca, mt);
        ExternalEvent fE = new ExternalEvent(f.key() + "_event", st, f.key(), dg, f.start_time(), duration, mt);
        ExternalSource g = new ExternalSource("G", st, dg, "2024-01-07T00:00:00Z", "2024-01-01T00:11:00Z", "2024-01-01T00:12:00Z", ca, mt);
        ExternalEvent gE = new ExternalEvent(g.key() + "_event", st, g.key(), dg, g.start_time(), duration, mt);

        assertDoesNotThrow(() -> {
          final var statement = connection.createStatement();
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);
          merlinHelper.insertTypesForEvent(cE, c);
          merlinHelper.insertTypesForEvent(dE, d);
          merlinHelper.insertTypesForEvent(eE, e);
          merlinHelper.insertTypesForEvent(fE, f);
          merlinHelper.insertTypesForEvent(gE, g);

          var res = statement.executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // verify the ranges for each result are as expected
          String[] expectedResults = {
              "{[\"2024-01-01 00:09:00+00\",\"2024-01-01 00:11:00+00\"),[\"2024-01-01 00:12:00+00\",\"2024-01-01 00:13:00+00\")}",
              "{[\"2024-01-01 00:00:00+00\",\"2024-01-01 00:02:00+00\")}",
              "{[\"2024-01-01 00:20:00+00\",\"2024-01-01 00:23:00+00\")}",
              "{[\"2024-01-01 00:02:00+00\",\"2024-01-01 00:04:00+00\"),[\"2024-01-01 00:07:00+00\",\"2024-01-01 00:09:00+00\")}",
              "{[\"2024-01-01 00:13:00+00\",\"2024-01-01 00:20:00+00\")}",
              "{[\"2024-01-01 00:04:00+00\",\"2024-01-01 00:07:00+00\")}",
              "{[\"2024-01-01 00:11:00+00\",\"2024-01-01 00:12:00+00\")}"
          };
          compareLists(expectedResults, res, "source_range");
        });
      }
    }

    ////////////////////////// RULE 1 //////////////////////////
    //  An External Event superceded by nothing will be present in the final, derived result.
    //     - test solitary event
    //     - test event with sources occurring before and after

    // test a solitary event, it vacuously shouldn't get superceded
    @Test
    void rule1_solitary() {
      assertDoesNotThrow(() -> {

        ExternalSource eS = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent e = createEvent("A.1", "2024-01-01T00:00:00Z", "01:00:00", eS);
        merlinHelper.insertTypesForEvent(e, eS);
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
        assertEquals("A.1", res.getString("event_key"));
        assertFalse(res.next());
      });
    }

    /*
      Test a bookended event, none of the surrounding sources and their events can/should overwrite it:
        A:     +++++aa++++++
        B:  bb+++
        C:                 +cc++++
     */
    @Test
    void rule1_bookended() {
      assertDoesNotThrow(() -> {

        ExternalSource A = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T02:00:00Z", ca, mt);
        ExternalSource B = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalSource C = new ExternalSource("C", st, dg, "2024-01-03T00:00:00Z", "2024-01-01T01:30:00Z", "2024-01-01T03:00:00Z", ca, mt);

        ExternalEvent e = createEvent("a", "2024-01-01T01:10:00Z", "00:10:00", A);
        ExternalEvent before = createEvent("b", "2024-01-01T00:00:00Z", "00:30:00", B);
        ExternalEvent after = createEvent("c", "2024-01-01T01:30:00Z", "01:00:00", C);
        merlinHelper.insertTypesForEvent(e, A);
        merlinHelper.insertTypesForEvent(before, B);
        merlinHelper.insertTypesForEvent(after, C);
      });

      assertDoesNotThrow(() -> {
        final var statement = connection.createStatement();
        final var res = statement.executeQuery(
            // language-sql
            """
            SELECT * FROM merlin.derived_events ORDER BY start_time;
            """
        );

        String[] expected_keys = {"b", "a", "c"};
        compareLists(expected_keys, res, "event_key");
      });
    }

    ////////////////////////// RULE 2 //////////////////////////
    //  An External Event partially superceded by a later External Source, but whose start time occurs before
    //            the start of said External Source(s), will be present in the final, derived result.
    //    - test basic case of second source starting in middle of existing event

    /*
      Testing sparse events was already covered in 1, so just test start_b > start_a, so a and b should show up (b is
        from a more valid source):
          A:  +++aaaaa
          B:      b+bb++++
          (a and both b's should be in result)
     */
    @Test
    void rule2() {
      assertDoesNotThrow(() -> {
        ExternalSource A = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalSource B = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T01:30:00Z", ca, mt);

        ExternalEvent e = createEvent("a", "2024-01-01T00:25:00Z", "00:10:00", A); // spills into B
        ExternalEvent b1 = createEvent("b1", "2024-01-01T00:30:00Z", "00:10:00", B);
        ExternalEvent b2 = createEvent("b2", "2024-01-01T00:45:00Z", "00:10:00", B);
        merlinHelper.insertTypesForEvent(e, A);
        merlinHelper.insertTypesForEvent(b1, B);
        merlinHelper.insertTypesForEvent(b2, B);
      });

      assertDoesNotThrow(() -> {
        final var statement = connection.createStatement();
        final var res = statement.executeQuery(
            // language-sql
            """
            SELECT * FROM merlin.derived_events ORDER BY start_time;
            """
        );

        String[] expected_keys = {"a", "b1", "b2"};
        compareLists(expected_keys, res, "event_key");
      });
    }

    ////////////////////////// RULE 3 //////////////////////////
    //  An External Event whose start is superseded by another External Source, even if its end occurs after the
    //            end of said External Source, will be replaced by the contents of that External Source (whether they are
    //            blank spaces, or other events).
    //    - test basic case of superceded source starting event that spills past superceding source, still won't show up
    //    - test above case but with empty source (derived_events will be empty)

    /*
      Basic case, wherein superceding source nullifies anything in A:
          A:    +a+aaaaa
          B:  b+bb++++
          (only b's should be in result)
     */
    @Test
    void rule3_basic() {
      assertDoesNotThrow(() -> {
        ExternalSource A = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T01:30:00Z", ca, mt);
        ExternalSource B = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);

        ExternalEvent e1 = createEvent("a1", "2024-01-01T00:40:00Z", "00:10:00", A); // negated by B, very clearly
        ExternalEvent e2 = createEvent("a2", "2024-01-01T00:55:00Z", "00:35:00", A); // even empty space in B neg should negate
        ExternalEvent b1 = createEvent("b1", "2024-01-01T00:00:00Z", "00:10:00", B);
        ExternalEvent b2 = createEvent("b2", "2024-01-01T00:30:00Z", "00:20:00", B);
        merlinHelper.insertTypesForEvent(e1, A);
        merlinHelper.insertTypesForEvent(e2, A);
        merlinHelper.insertTypesForEvent(b1, B);
        merlinHelper.insertTypesForEvent(b2, B);
      });

      assertDoesNotThrow(() -> {
        final var statement = connection.createStatement();
        final var res = statement.executeQuery(
            // language-sql
            """
            SELECT * FROM merlin.derived_events ORDER BY start_time;
            """
        );

        String[] expected_keys = {"b1", "b2"};
        compareLists(expected_keys, res, "event_key");
      });
    }

    /*
      Empty source still nullifies all of A.
          A:    +a+aaaaa
          B:  ++++++++
          (empty result)
     */
    @Test
    void rule3_empty() {
      assertDoesNotThrow(() -> {
        ExternalSource A = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T01:30:00Z", ca, mt);
        ExternalSource B = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);

        ExternalEvent e1 = createEvent("a1", "2024-01-01T00:40:00Z", "00:10:00", A); // negated by empty space
        ExternalEvent e2 = createEvent("a2", "2024-01-01T00:55:00Z", "00:35:00", A); // negated by empty space
        merlinHelper.insertTypesForEvent(e1, A);
        merlinHelper.insertTypesForEvent(e2, A);

        // insert B as a source
        final var statement = connection.createStatement();
        statement.executeUpdate(
            // language-sql
            """
            INSERT INTO
              merlin.external_source
            VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
            ON CONFLICT(key, derivation_group_name) DO NOTHING;
            """.formatted(
                B.key(),
                B.source_type_name(),
                B.derivation_group_name(),
                B.valid_at(),
                B.start_time(),
                B.end_time(),
                B.created_at(),
                B.metadata()
            )
        );
      });

      assertDoesNotThrow(() -> {
        final var statement = connection.createStatement();
        final var res = statement.executeQuery(
            // language-sql
            """
            SELECT * FROM merlin.derived_events ORDER BY start_time;
            """
        );

        String[] expected_keys = {};
        compareLists(expected_keys, res, "event_key");
      });
    }

    ////////////////////////// RULE 4 //////////////////////////
    //  An External Event who shares an ID with an External Event in a later External Source will always be
    //            replaced.
    //    - demonstrate with completely sparse events, different types too, but latest one wins

    /*
        Completely sparse sources, just to purely illustrate rule 4 and that it works.
            A:                   ++++aaa+++++
            B:                                  +++++aaaaa+
            C:   +++++aaaa+++++
     */
    @Test
    void rule4() {
      assertDoesNotThrow(() -> {
        ExternalSource A = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T01:30:00Z", "2024-01-01T02:30:00Z", ca, mt);
        ExternalSource B = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T03:00:00Z", "2024-01-01T04:00:00Z", ca, mt);
        ExternalSource C = new ExternalSource("C", st, dg, "2024-01-03T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);

        ExternalEvent e1 = createEvent("a", "2024-01-01T01:50:00Z", "00:10:00", A); // negated by empty space
        ExternalEvent e2 = createEvent("a", "2024-01-01T03:40:00Z", "00:15:00", B); // negated by empty space
        ExternalEvent e3 = createEvent("a", "2024-01-01T00:30:00Z", "00:20:00", C); // negated by empty space
        merlinHelper.insertTypesForEvent(e1, A);
        merlinHelper.insertTypesForEvent(e2, B);
        merlinHelper.insertTypesForEvent(e3, C);
      });

      assertDoesNotThrow(() -> {
        final var statement = connection.createStatement();
        final var res = statement.executeQuery(
            // language-sql
            """
            SELECT * FROM merlin.derived_events ORDER BY start_time;
            """
        );

        String[] expected_keys = {"a"};
        // only 1 expected result
        assertTrue(res.next());
        assertEquals("a", res.getString("event_key"));
        assertEquals("C", res.getString("source_key"));
        assertEquals("{[\"2024-01-01 00:00:00+00\",\"2024-01-01 01:00:00+00\")}", res.getString("source_range"));
        assertFalse(res.next());
      });
    }
  }

  // Generally, no two sources, regardless of overlap, can have the same valid_at. We demonstrate this with sparse sources. Don't even need events to demonstrate.
  @Test
  void sameValid_at() {
    String st = "Test";
    String dg = "Test Default";
    String ca = "2024-01-01T00:00:00Z";
    String mt = "{}";
    ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T03:00:00Z", "2024-01-01T04:00:00Z", ca, mt);
    ExternalSource b = new ExternalSource("B", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", "2024-01-01T02:00:00Z", ca, mt);

    assertDoesNotThrow(() -> {
       final var statement = connection.createStatement();
       statement.executeUpdate(
           //language-sql
           """
               INSERT INTO merlin.external_source_type VALUES ('%s')
               """.formatted(st)
       );

       statement.executeUpdate(
           //language-sql
           """
               INSERT INTO merlin.derivation_group VALUES ('%s', '%s')
               """.formatted(dg, st)
       );

       statement.executeUpdate(
           // language-sql
           """
               INSERT INTO merlin.external_source VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
               """.formatted(
               a.key(),
               a.source_type_name(),
               a.derivation_group_name(),
               a.valid_at(),
               a.start_time(),
               a.end_time(),
               a.created_at(),
               a.metadata()
           )
       );
    });

    // fails now
    assertThrowsExactly(PSQLException.class, () -> {
      final var statement = connection.createStatement();
      statement.executeUpdate(
          // language-sql
          """
          INSERT INTO merlin.external_source VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
          """.formatted(
              b.key(),
              b.source_type_name(),
              b.derivation_group_name(),
              b.valid_at(),
              b.start_time(),
              b.end_time(),
              b.created_at(),
              b.metadata()
          )
      );
    });
  }

  // A source can have two (or more) events occurring at the same time, of the same type and all (BUT must have diff keys...see below)
  @Test
  void nEventsAtSameTime() {
    ExternalSource A = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:30:00Z", ca, mt);

    ExternalEvent e1 = createEvent("a", "2024-01-01T00:00:00Z", "00:10:00", A);
    ExternalEvent e2 = createEvent("b", "2024-01-01T00:00:00Z", "00:05:00", A);
    ExternalEvent e3 = createEvent("c", "2024-01-01T00:00:00Z", "00:15:00", A);

    assertDoesNotThrow(() -> {
      merlinHelper.insertTypesForEvent(e1, A);
      merlinHelper.insertTypesForEvent(e2, A);
      merlinHelper.insertTypesForEvent(e3, A);
    });

    assertDoesNotThrow(() -> {
      final var statement = connection.createStatement();
      var res = statement.executeQuery("SELECT * FROM merlin.derived_events ORDER BY start_time, event_key ASC");
      String[] expected_keys = {"a", "b", "c"};
      compareLists(expected_keys, res, "event_key");
    });
  }

  // Two events, even if totally sparse, bearing same key in same source not possible
  @Test
  void noDuplicateEventsInSameSource() {
    ExternalSource A = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:30:00Z", ca, mt);

    ExternalEvent e1 = createEvent("a", "2024-01-01T00:00:00Z", "00:10:00", A);
    ExternalEvent e2 = createEvent("a", "2024-01-01T00:55:00Z", "00:15:00", A); // illegal!

    assertDoesNotThrow(() -> {
      merlinHelper.insertTypesForEvent(e1, A);
    });

    assertThrowsExactly(PSQLException.class, () -> {
      merlinHelper.insertTypesForEvent(e2, A);
    });
  }

  // End time can't be < start time (but can be =)
  @Test
  void endTimeGEstartTime() {
    ExternalSource failing = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", "2024-01-01T00:30:00Z", ca, mt);
    ExternalSource succeeding = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", "2024-01-01T01:00:00Z", ca, mt);

    assertDoesNotThrow(() -> {
      final var statement = connection.createStatement();
      // create the source type
      statement.executeUpdate(
         // language-sql
         """
         INSERT INTO
           merlin.external_source_type
         VALUES ('%s')
         ON CONFLICT(name) DO NOTHING;
         """.formatted(failing.source_type_name())
      );

       // create the derivation_group
       statement.executeUpdate(
         // language-sql
         """
         INSERT INTO
           merlin.derivation_group
         VALUES ('%s', '%s')
         ON CONFLICT(name, source_type_name) DO NOTHING;
         """.formatted(failing.derivation_group_name(), failing.source_type_name())
       );
    });

    assertThrowsExactly(PSQLException.class, () -> {
      final var statement = connection.createStatement();
      statement.executeUpdate(
        // language-sql
        """
        INSERT INTO
          merlin.external_source
        VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
        ON CONFLICT(key, derivation_group_name) DO NOTHING;
        """.formatted(
          failing.key(),
          failing.source_type_name(),
          failing.derivation_group_name(),
          failing.valid_at(),
          failing.start_time(),
          failing.end_time(),
          failing.created_at(),
          failing.metadata()
        )
      );
    });

    assertDoesNotThrow(() -> {
      final var statement = connection.createStatement();
      statement.executeUpdate(
          // language-sql
          """
          INSERT INTO
            merlin.external_source
          VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
          ON CONFLICT(key, derivation_group_name) DO NOTHING;
          """.formatted(
              succeeding.key(),
              succeeding.source_type_name(),
              succeeding.derivation_group_name(),
              succeeding.valid_at(),
              succeeding.start_time(),
              succeeding.end_time(),
              succeeding.created_at(),
              succeeding.metadata()
          )
      );
    });
  }

  // An error is encountered if event is out of bounds of source (i.e. starts or ends before, starts or ends after)
  /*
      Source :      +++++++++++++++
      Before1: 1111
      Before2:    2222
      After1 :                   3333
      After2 :                      4444
   */
  @Test
  void externalEventSourceBounds() {
    ExternalSource A = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", "2024-01-01T02:00:00Z", ca, mt);

    ExternalEvent legal = createEvent("a", "2024-01-01T01:00:00Z", "00:10:00", A); // legal.
    ExternalEvent completelyBefore = createEvent("completelyBefore", "2024-01-01T00:00:00Z", "00:10:00", A); // illegal!
    ExternalEvent beforeIntersect = createEvent("beforeIntersect", "2024-01-01T00:55:00Z", "00:25:00", A); // illegal!
    ExternalEvent afterIntersect = createEvent("afterIntersect", "2024-01-01T01:45:00Z", "00:30:00", A); // illegal!
    ExternalEvent completelyAfter = createEvent("completelyAfter", "2024-01-01T02:10:00Z", "00:15:00", A); // illegal!

    assertDoesNotThrow(() -> {
      merlinHelper.insertTypesForEvent(legal, A);
    });

    assertThrowsExactly(PSQLException.class, () -> {
      merlinHelper.insertTypesForEvent(completelyBefore, A);
    });

    assertThrowsExactly(PSQLException.class, () -> {
      merlinHelper.insertTypesForEvent(beforeIntersect, A);
    });

    assertThrowsExactly(PSQLException.class, () -> {
      merlinHelper.insertTypesForEvent(afterIntersect, A);
    });

    assertThrowsExactly(PSQLException.class, () -> {
      merlinHelper.insertTypesForEvent(completelyAfter, A);
    });
  }

  // TODO: test all constraints (namely duplicates and deletions)
  //    - duplicated source (works across DG, not in DG)
  //    - duplicated DG (not at all allowable, even if diff source type)
  //    - deleting DG with existing plan link
  //    - deleting DG with source
  //    - deleting source type with source
  //    - deleting event type with event

  // TODO: extra test for derived events that manages several derivation groups (do derivation test twice but with new DG name, should be no overlap!)

}
