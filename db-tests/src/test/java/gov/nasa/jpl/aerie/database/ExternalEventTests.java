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


  private record ExternalEventType(String name){}
  private record ExternalSourceType(String name){}
  private record DerivationGroup(String name, String source_type_name){}
  private record DerivedExternalEvent(String source_key, String derivation_group_name, String event_key, String event_type_name, String start_time, String source_range, String valid_at){}

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

  // test each derived event rule individually
  //  + verify that external source overlap works as expected
  //  + rule 1: An External Event superceded by nothing will be present in the final, derived result.
  //     - test solitary event, test event with stuff occurring before and after
  //  + rule 2: An External Event partially superceded by a later External Source, but whose start time occurs before
  //            the start of said External Source(s), will be present in the final, derived result.
  //     - test apart covered in 1, so just test start_b > start_a, so a and b should show up (b is from a more valid source)
  //  + rule 3: An External Event whose start is superseded by another External Source, even if its end occurs after the
  //            end of said External Source, will be replaced by the contents of that External Source (whether they are
  //            blank spaces, or other events).
  //  + rule 4: An External Event who shares an ID with an External Event in a later External Source will always be
  //            replaced.
  //  + test ranges of sources
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
    ExternalEvent createEvent(String key, String start_time, String duration, String properties, ExternalSource source) {
      return new ExternalEvent(
          key,
          "RuleTest",
          source.key(),
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

        ExternalEvent e = createEvent("e", "2024-01-01T01:00:00Z", "01:00:00", "{}", defaultES);
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
        assertEquals("e", res.getString("event_key"));
        assertFalse(res.next());
      });
    }

    // test a bookended event, none of the surrounding sources and their events can/should overwrite it
    @Test
    void rule1_bookended() {
      assertDoesNotThrow(() -> {
        ExternalSource beforeES = new ExternalSource("rule_test-1.json", "RuleTest", "RuleTest Default", "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", "2024-01-01T00:00:00Z", "{}");
        ExternalSource afterES = new ExternalSource("rule_test+1.json", "RuleTest", "RuleTest Default", "2024-01-02T00:00:00Z", "2024-01-01T02:00:00Z", "2024-01-01T03:00:00Z", "2024-01-01T00:00:00Z", "{}");

        ExternalEvent e = createEvent("e", "2024-01-01T01:00:00Z", "01:00:00", "{}", defaultES);
        ExternalEvent before = createEvent("before", "2024-01-01T00:00:00Z", "01:00:00", "{}", beforeES);
        ExternalEvent after = createEvent("after", "2024-01-01T02:00:00Z", "01:00:00", "{}", afterES);
        merlinHelper.insertTypesForEvent(e, defaultES);
        merlinHelper.insertTypesForEvent(before, beforeES);
        merlinHelper.insertTypesForEvent(after, afterES);
      });

      assertDoesNotThrow(() -> {
        final var statement = connection.createStatement();
        final var res = statement.executeQuery(
            // language-sql
            """
            SELECT * FROM merlin.derived_events ORDER BY start_time;
            """
        );

        String[] expected_keys = {"before", "e", "after"};
        compareLists(expected_keys, res, "event_key");
      });
    }

    // repeat of the above but with empty sources - the point is that the replacement math is not between events but sources.
    @Test
    void rule1_bookended_empty() {
      assertDoesNotThrow(() -> {
        ExternalSource beforeES = new ExternalSource("rule_test-1.json", "RuleTest", "RuleTest Default", "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T03:00:00Z", "2024-01-01T00:00:00Z", "{}");
        ExternalSource eES = new ExternalSource("rule_test.json", "RuleTest", "RuleTest Default", "2024-01-02T00:00:00Z", "2024-01-01T01:00:00Z", "2024-01-01T01:30:00Z", "2024-01-01T00:00:00Z", "{}");
        ExternalSource afterES = new ExternalSource("rule_test+1.json", "RuleTest", "RuleTest Default", "2024-01-03T00:00:00Z", "2024-01-01T02:00:00Z", "2024-01-01T03:00:00Z", "2024-01-01T00:00:00Z", "{}");

        ExternalEvent before = createEvent("before", "2024-01-01T00:00:00Z", "01:00:00", "{}", beforeES);
        ExternalEvent e = createEvent("e", "2024-01-01T01:00:00Z", "01:00:00", "{}", eES); // TODO: THIS SHOULD FAIL...still included even though it spills out of its source's limits? WHEN THAT TEST IS FIXED MAKE THIS A 30 MINUTE EVENT...
        ExternalEvent after = createEvent("after", "2024-01-01T02:00:00Z", "01:00:00", "{}", afterES);
        merlinHelper.insertTypesForEvent(e, eES);
        merlinHelper.insertTypesForEvent(before, beforeES);
        merlinHelper.insertTypesForEvent(after, afterES);
      });

      assertDoesNotThrow(() -> {
        final var statement = connection.createStatement();
        final var res = statement.executeQuery(
            // language-sql
            """
            SELECT * FROM merlin.derived_events ORDER BY start_time;
            """
        );

        String[] expected_ranges = {
            "{[\"2024-01-01 00:00:00+00\",\"2024-01-01 01:00:00+00\"),[\"2024-01-01 01:30:00+00\",\"2024-01-01 02:00:00+00\")}", // gets trimmed
            "{[\"2024-01-01 01:00:00+00\",\"2024-01-01 01:30:00+00\")}",
            "{[\"2024-01-01 02:00:00+00\",\"2024-01-01 03:00:00+00\")}"
        };
        compareLists(expected_ranges, res, "source_range");
      });
    }

    // TODO: add test to ensure if event spills out of bounds of source, error thrown

    // TODO: add tests about external source bounds

    ////////////////////////// RULE 2 //////////////////////////

    // TODO: rule 3 but with a totally empty source (should work)
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

  // TODO: add test demonstrating that a source can have two events occurring at the same time, of the same type and all

  // TODO: add test demonstrating that two events, even if totally sparse, bearing same key in same source not possible

  // TODO: add test demonstrating for sources that end time can't be < start time but can be =

  // TODO: add test throwing error if event is out of bounds of source (i.e. starts or ends before, starts or ends after)

  // test all constraints (namely duplicates and deletions)
  // extra test for derived events that manages several derivation groups

}
