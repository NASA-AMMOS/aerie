package gov.nasa.jpl.aerie.database;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

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

  /*
    ---------- GENERIC DATABASE TESTING SETUP ----------
   */
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


  /*
    ---------- COMMONLY REPEATED VARIABLES ----------
   */
  // Source Type (st)
  String st = "Test";

  // Derivation Group (dg)
  String dg = "Test Default";

  // Event Type (et)
  String et = "Test";

  // Metadata/Properties (mt)
  String mt = "{}";

  // Created At (ca)
  String ca = "2024-01-01T00:00:00Z";


  /*
    ---------- COMMONLY REPEATED FUNCTIONS ----------
   */
  /**
    * Quick external event creator, leveraging constants from a provided source object (source).
    */
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

  /**
   * A simple function to compare a SQL result (res) for a given key (key) against a list of expected entries (expected)
   */
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

  /**
   * Returns a java.sql.Statement object, from which queries can be run. The exact idiom, including the assertion,
   *    are repeated fairly often, so it was extracted into a method for ease of readability and consistency.
   * As we need to declare it outside of the assertion and assign it _in_ the assertion (else it cannot be returned)
   *    we must wrap it as an AtomicReference object. It helps to isolate and check this assertion separately as if
   *    _this_ fails we know our error is not with the underlying schema, but rather the connection.
   */
  AtomicReference<Statement> getStatement() {
    final AtomicReference<Statement> ret = new AtomicReference<>();
    assertDoesNotThrow(() -> {
      ret.set(connection.createStatement());
    });
    return ret;
  }

  /**
   * Repeated function that uploads a source and various events, as well as related types.
   * Used in:
   *  - BASIC TESTS (uploadWithoutError, basicDerivedEvents, derivationGroupComp, verifyDeletion)
   *  - final duplication tests (duplicateSource, duplicatedDG)
   *  - superDerivedEvents
   *
   * Data here is based on the SSMO-MPS/mission-data-sandbox/derivation_test examples.
   */
  void upload_source(String dg) throws SQLException {
    // First, define the sources.
    ExternalSource sourceOne = new ExternalSource(
        "Derivation_Test_00.json",
        st,
        dg,
        "2024-01-18 00:00:00+00",
        "2024-01-05 00:00:00+00",
        "2024-01-11 00:00:00+00",
        "2024-08-21 22:36:12.858009+00",
        "{}"
    );
    ExternalSource sourceTwo = new ExternalSource(
        "Derivation_Test_01.json",
        st,
        dg,
        "2024-01-19 00:00:00+00",
        "2024-01-01 00:00:00+00",
        "2024-01-07 00:00:00+00",
        "2024-08-21 22:36:19.381275+00",
        "{}"
    );
    ExternalSource sourceThree = new ExternalSource(
        "Derivation_Test_02.json",
        st,
        dg,
        "2024-01-20 00:00:00+00",
        "2024-01-03 00:00:00+00",
        "2024-01-10 00:00:00+00",
        "2024-08-21 22:36:23.340941+00",
        "{}"
    );
    ExternalSource sourceFour = new ExternalSource(
        "Derivation_Test_03.json",
        st,
        dg,
        "2024-01-21 00:00:00+00",
        "2024-01-01 12:00:00+00",
        "2024-01-02 12:00:00+00",
        "2024-08-21 22:36:28.365244+00",
        "{}"
    );

    // Second, define the events, spaced by sources 1-4
    ExternalEvent twoA = new ExternalEvent("2", "DerivationD", "Derivation_Test_00.json", dg, "2024-01-05 23:00:00+00", "01:10:00", "{\"notes\": \"subsumed by test 01, even though end lies outside of 01, also replaced by test 01 by key\", \"rules\": [3, 4], \"should_present\": false}");
    ExternalEvent seven = new ExternalEvent("7", "DerivationC", "Derivation_Test_00.json", dg, "2024-01-09 23:00:00+00", "02:00:00", "{\"notes\": \"subsumed by test 02, even though end lies outside of 02, because start time during 01\", \"rules\": [3], \"should_present\": false}");
    ExternalEvent eight = new ExternalEvent("8", "DerivationB", "Derivation_Test_00.json", dg, "2024-01-10 11:00:00+00", "01:05:00", "{\"notes\": \"after everything, subsumed by nothing despite being from oldest file\", \"rules\": [1], \"should_present\": true}");

    ExternalEvent one = new ExternalEvent("1", "DerivationA", "Derivation_Test_01.json", dg, "2024-01-01 00:00:00+00", "02:10:00", "{\"notes\": \"before everything, subsumed by nothing\", \"rules\": [1], \"should_present\": true}");
    ExternalEvent twoB = new ExternalEvent("2", "DerivationA", "Derivation_Test_01.json", dg, "2024-01-01 12:00:00+00", "02:10:00", "{\"notes\": \"overwritten by key in later file, even with type change\", \"rules\": [4], \"should_present\": false}");
    ExternalEvent three = new ExternalEvent("3", "DerivationB", "Derivation_Test_01.json", dg, "2024-01-02 23:00:00+00", "03:00:00", "{\"notes\": \"starts before next file though occurs during next file, still included\", \"rules\": [2], \"should_present\": true}");
    ExternalEvent four = new ExternalEvent("4", "DerivationB", "Derivation_Test_01.json", dg, "2024-01-05 21:00:00+00", "03:00:00", "{\"notes\": \"start subsumed by 02, not included in final result\", \"rules\": [3], \"should_present\": false}");

    ExternalEvent five = new ExternalEvent("5", "DerivationC", "Derivation_Test_02.json", dg, "2024-01-05 23:00:00+00", "01:10:00", "{\"notes\": \"not subsumed, optionally change this event to have key 6 and ensure this test fails\", \"rules\": [1], \"should_present\": true}");
    ExternalEvent six = new ExternalEvent("6", "DerivationC", "Derivation_Test_02.json", dg, "2024-01-06 12:00:00+00", "02:00:00", "{\"notes\": \"not subsumed\", \"rules\": [1], \"should_present\": true}");
    ExternalEvent twoC = new ExternalEvent("2", "DerivationB", "Derivation_Test_02.json", dg, "2024-01-09 11:00:00+00", "01:05:00", "{\"notes\": \"replaces 2 in test 01, despite different event type\", \"rules\": [4], \"should_present\": true}");

    ExternalEvent nine = new ExternalEvent("9", "DerivationC", "Derivation_Test_03.json", dg, "2024-01-02 00:00:00+00", "01:00:00", "{\"notes\": \"not subsumed\", \"rules\": [1], \"should_present\": true}");

    // insert them and any related types (skipping overlaps)
    merlinHelper.insertTypesForEvent(twoA, sourceOne);
    merlinHelper.insertTypesForEvent(seven, sourceOne);
    merlinHelper.insertTypesForEvent(eight, sourceOne);
    merlinHelper.insertTypesForEvent(one, sourceTwo);
    merlinHelper.insertTypesForEvent(twoB, sourceTwo);
    merlinHelper.insertTypesForEvent(three, sourceTwo);
    merlinHelper.insertTypesForEvent(four, sourceTwo);
    merlinHelper.insertTypesForEvent(five, sourceThree);
    merlinHelper.insertTypesForEvent(six, sourceThree);
    merlinHelper.insertTypesForEvent(twoC, sourceThree);
    merlinHelper.insertTypesForEvent(nine, sourceFour);
  }


  /*
    ---------- BASIC TESTS ----------
   */

  /**
   * A set of basic tests that verify the general functionality of uploading sources, derived events, the
   *    derivation_group_comp view, and deletion.
   * Aside from the derivation_group_comp view test, these tests are not rigorous (more rigorous tests follow in the
   *    ConstraintsTests class).
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class BasicTests {
    /**
     * This first test is to verify that a simple upload works, regardless of if its output is correct.
     * As we do not upload a file we just run the queries that would be run if files were uploaded,
     * namely tests 01-04 from SSMO-MPS/mission-data-sandbox/derivation_test.
     */
    @Test
    void uploadWithoutError() {
      assertDoesNotThrow(() -> upload_source(dg));
    }


    /**
     * This test uploads data (tests 01-04 from SSMO-MPS/mission-data-sandbox/derivation_test) and then confirms derived
     * events associated with the upload perform as expected. All desired keys should be present.
     * This is an extension of uploadWithoutError.
     */
    @Test
    void basicDerivedEvents() {
      // upload all source data
      assertDoesNotThrow(() -> upload_source(dg));
      final var statement = getStatement();

      // check that derived events in our prewritten case has the correct keys
      assertDoesNotThrow(() -> {
        final var res = statement.get().executeQuery(
            // language-sql
            """
                SELECT * FROM merlin.derived_events ORDER BY start_time;
                """
        );

        String[] expected_keys = {"1", "9", "3", "5", "6", "2", "8"};
        compareLists(expected_keys, res, "event_key");
      });
    }

    /**
     * This test uploads data (tests 01-04 from SSMO-MPS/mission-data-sandbox/derivation_test) and then confirms
     * derivation_group_comp view associated with upload performs as expected.
     * This test alone is rigorous enough for derivation_group_comp view, and is an extension of uploadWithoutError.
     */
    @Test
    void derivationGroupComp() {
      // upload all source data
      assertDoesNotThrow(() -> upload_source(dg));
      final var statement = getStatement();

      // check that derivation_group_comp has 1 entry, with 4 sources and 7 events
      assertDoesNotThrow(() -> {
        final var res = statement.get().executeQuery(
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

    /**
     * This test uploads data (tests 01-04 from SSMO-MPS/mission-data-sandbox/derivation_test) and then confirms
     * deletion works as expected. It is not a rigorous test, as we only focus on one order (any other order is
     * logically equivalent to testing constraints, which is guaranteed by PSQL and tested later). We follow the
     * correct order of:
     * - external_event
     * - external_event_type
     * - external_source
     * - derivation_group
     * - external_source_type
     * though it is possible to rearrange this order, so long as events are deleted before their types, sources deleted
     * before their types but after events, and derivation groups deleted after linked sources removed but before
     * source types removed, things work as expected.
     *
     * For completeness, another arbitrary ordering (event, event type, source, source type, derivation group (hence an
     * error, as source type is deleted before derivation group)) is included as a basic example of failure.
     */
    @Test
    void verifyDeletion() {
      final var statement = getStatement();

      // correct deletion order (a small amount of variance is allowed here, see documentation of this function).
      assertDoesNotThrow(() -> upload_source(dg));
      assertDoesNotThrow(() -> {
        statement.get().executeUpdate(
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
      assertDoesNotThrow(() -> upload_source(dg));
      assertThrows(SQLException.class, () -> {
        statement.get().executeUpdate(
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
  }


  /*
    ---------- DERIVATION TESTS ----------
   */
  /**
   * The focus of this class is to test specifically the derivation function for events. This includes overlapping of
   *    source windows, as well as reconciling differences inside the sources themselves (by testing each of the 4
   *    derivation rules).
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class DerivedEventsTests {

    /**
     * This class focuses on testing sources' windows to verify that derivation will work as expected. These are
     *    separate, extracted tests that don't really evaluate events and instead just that sources play together
     *    correctly. As such we test "empty" sources to make sure their overlapped windows work correctly.
     * We add events that span a very short duration simply so that the sources show up in derived_events, but we aren't
     *    testing any properties of said events.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class DerivedSourcesTests {

      // Commonly Repeated:
      String duration = "00:00:00.000001";

      /**
       * The first test is a basic, non-overlapping case. We must ensure that gaps are preserved:
       *
       *    A:             ++++++++
       *    B:  +++++++
       *        BBBBBBB    AAAAAAAA
       */
      @Test
      void testSparseCoverage() {
        final var statement = getStatement();

        // create our sources and their per-window events
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T03:00:00Z", "2024-01-01T04:00:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, a.start_time(), duration, mt);
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T01:00:00Z", "2024-01-01T02:00:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);

        // verify the ranges are as expected
        assertDoesNotThrow(() -> {
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);

          var res = statement.get().executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // both ranges should only have a single element and be fully present
          String[] expectedResults = {
              "{[\"2024-01-01 03:00:00+00\",\"2024-01-01 04:00:00+00\")}",
              "{[\"2024-01-01 01:00:00+00\",\"2024-01-01 02:00:00+00\")}"
          };
          compareLists(expectedResults, res, "source_range");
        });
      }

      /**
       * This test is an overlapping case wherein the source of higher precedence (the more recently valid one - B)
       *    should shorten the range that A applies over. That is, a source is succeeded before in time by a more valid
       *    source.
       *
       *    A:     ++++++++
       *    B:  +++++++
       *        BBBBBBBAAAA
       */
      @Test
      void testForwardOverlap() {
        final var statement = getStatement();

        // create our sources and their per-window events
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T02:00:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, "2024-01-01T1:10:00Z", duration, mt); // have to manually pick this
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);

        // verify the ranges are as expected
        assertDoesNotThrow(() -> {
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);

          var res = statement.get().executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // verify the range for A is shorter than what is specified in the definition of "a" - it should start later.
          String[] expectedResults = {
              "{[\"2024-01-01 01:00:00+00\",\"2024-01-01 02:00:00+00\")}",
              "{[\"2024-01-01 00:00:00+00\",\"2024-01-01 01:00:00+00\")}"
          };
          compareLists(expectedResults, res, "source_range");
        });
      }

      /**
       * This test is an overlapping case wherein the source of higher precedence (the more recently valid one - B)
       *    should shorten the range that A applies over. In this case, a source is succeeded after in time by a more
       *    valid source.
       *
       *    A:  +++++++
       *    B:     ++++++++
       *        AAABBBBBBBB
       */
      @Test
      void testBackwardOverlap() {
        final var statement = getStatement();

        // create our sources and their per-window events
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, "2024-01-01T00:00:00Z", duration, mt); // have to manually pick this
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);

        // verify the ranges are as expected
        assertDoesNotThrow(() -> {
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);

          var res = statement.get().executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // verify the range for A is shorter than what is specified in the definition of "a" - it should end sooner.
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
      /**
       * This test is an overlapping case wherein there is a source preceding all sources in validity, that covers a
       *    larger, more inclusive interval. This source should be ever present, even if chopped into several
       *    subintervals:
       *
       *    A:      +++++++++++++++++++++
       *    B:  ++++++
       *    C:                +++++++
       *        BBBBBBAAAAAAAACCCCCCCAAAA
       */
      @Test
      void testBackground() {
        final var statement = getStatement();

        // create our sources and their per-window events
        ExternalSource a = new ExternalSource("A", st, dg, "2024-01-01T00:00:00Z", "2024-01-01T00:30:00Z", "2024-01-01T03:00:00Z", ca, mt);
        ExternalEvent aE = new ExternalEvent(a.key() + "_event", st, a.key(), dg, "2024-01-01T01:10:00Z", duration, mt); // just need 1 that shows up and source range will still show correctly
        ExternalSource b = new ExternalSource("B", st, dg, "2024-01-02T00:00:00Z", "2024-01-01T00:00:00Z", "2024-01-01T01:00:00Z", ca, mt);
        ExternalEvent bE = new ExternalEvent(b.key() + "_event", st, b.key(), dg, b.start_time(), duration, mt);
        ExternalSource c = new ExternalSource("C", st, dg, "2024-01-03T00:00:00Z", "2024-01-01T01:30:00Z", "2024-01-01T02:00:00Z", ca, mt);
        ExternalEvent cE = new ExternalEvent(c.key() + "_event", st, c.key(), dg, c.start_time(), duration, mt);

        // verify the ranges are as expected
        assertDoesNotThrow(() -> {
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);
          merlinHelper.insertTypesForEvent(cE, c);

          var res = statement.get().executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // verify the range for the first source is split into intervals
          String[] expectedResults = {
              "{[\"2024-01-01 01:00:00+00\",\"2024-01-01 01:30:00+00\"),[\"2024-01-01 02:00:00+00\",\"2024-01-01 03:00:00+00\")}",
              "{[\"2024-01-01 00:00:00+00\",\"2024-01-01 01:00:00+00\")}",
              "{[\"2024-01-01 01:30:00+00\",\"2024-01-01 02:00:00+00\")}"
          };
          compareLists(expectedResults, res, "source_range");
        });

      }

      /**
       * The first 4 tests are exhaustive of source window cases. This final test, then, is included as an overall,
       *    cumulative case:
       *    A:     ++++++++++++
       *    B:  ++++++
       *    C:                   ++++++
       *    D:    +++++++
       *    E:               +++++++
       *    F:      +++
       *    G:             +
       *        BBDDFFFDDAAGAEEEEEEECCC
       */
      @Test
      void testAmalgamation() {
        final var statement = getStatement();

        // create our sources and their per-window events
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

        // verify the ranges are as expected
        assertDoesNotThrow(() -> {
          merlinHelper.insertTypesForEvent(aE, a);
          merlinHelper.insertTypesForEvent(bE, b);
          merlinHelper.insertTypesForEvent(cE, c);
          merlinHelper.insertTypesForEvent(dE, d);
          merlinHelper.insertTypesForEvent(eE, e);
          merlinHelper.insertTypesForEvent(fE, f);
          merlinHelper.insertTypesForEvent(gE, g);

          var res = statement.get().executeQuery("SELECT * FROM merlin.derived_events ORDER BY source_key");

          // ranges should be shortened and broken just like the preceding comment suggests.
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

    /**
     * This class focuses on testing each of the four rules involved in the derivation operation. To do so, new sources
     *    and events are created, and their presence and intervals are tested to ensure correctness.
     * The rules are as follows. Refer to the docs for more details on why they are what they are:
     *    1. An External Event superceded by nothing will be present in the final, derived result.
     *    2. An External Event partially superceded by a later External Source, but whose start time occurs before
     *          the start of said External Source(s), will be present in the final, derived result.
     *    3. An External Event whose start is superseded by another External Source, even if its end occurs after the
     *          end of said External Source, will be replaced by the contents of that External Source (whether they are
     *          blank spaces, or other events).
     *    4. An External Event who shares a key with an External Event in a later External Source will always be
     *          replaced.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class DerivedEventRuleTests {

      ////////////////////////// RULE 1 //////////////////////////
      /**
       * This test examines the lack of supercession by testing a solitary event. It vacuously shouldn't get superceded.
       *
       *    A:   aa
       */
      @Test
      void rule1_solitary() {
        final var statement = getStatement();

        // insert the event(s) (and their source(s))
        assertDoesNotThrow(() -> {
          ExternalSource eS = new ExternalSource(
              "A",
              st,
              dg,
              "2024-01-01T00:00:00Z",
              "2024-01-01T00:00:00Z",
              "2024-01-01T01:00:00Z",
              ca,
              mt);

          ExternalEvent e = createEvent("A.1", "2024-01-01T00:00:00Z", "01:00:00", eS);

          merlinHelper.insertTypesForEvent(e, eS);
        });

        // ensure the result has the right size and keys
        assertDoesNotThrow(() -> {
          final var res = statement.get().executeQuery(
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

      /**
       * This test more thoroughly examines the lack of supercession with other sources. The event is bookended, but
       *    none of the surrounding sources and their events can/should overwrite it.
       *
       *    A:     +++++aa++++++
       *    B:  bb+++
       *    C:                 +cc++++
       */
      @Test
      void rule1_bookended() {
        final var statement = getStatement();

        // insert the event(s) (and their source(s))
        assertDoesNotThrow(() -> {
          ExternalSource A = new ExternalSource(
              "A",
              st,
              dg,
              "2024-01-01T00:00:00Z",
              "2024-01-01T00:30:00Z",
              "2024-01-01T02:00:00Z",
              ca,
              mt);
          ExternalSource B = new ExternalSource(
              "B",
              st,
              dg,
              "2024-01-02T00:00:00Z",
              "2024-01-01T00:00:00Z",
              "2024-01-01T01:00:00Z",
              ca,
              mt);
          ExternalSource C = new ExternalSource(
              "C",
              st,
              dg,
              "2024-01-03T00:00:00Z",
              "2024-01-01T01:30:00Z",
              "2024-01-01T03:00:00Z",
              ca,
              mt);

          ExternalEvent e = createEvent("a", "2024-01-01T01:10:00Z", "00:10:00", A);
          ExternalEvent before = createEvent("b", "2024-01-01T00:00:00Z", "00:30:00", B);
          ExternalEvent after = createEvent("c", "2024-01-01T01:30:00Z", "01:00:00", C);

          merlinHelper.insertTypesForEvent(e, A);
          merlinHelper.insertTypesForEvent(before, B);
          merlinHelper.insertTypesForEvent(after, C);
        });

        // verify the expected keys are included
        assertDoesNotThrow(() -> {
          final var res = statement.get().executeQuery(
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
      /**
       * This test examines that supercession doesn't occur if the start time of an event in a less valid (earlier
       *    valid_at) source is less than the start time of a newer source. This is explicitly what rule 2 states.
       *
       *    A:  +++aaaaa
       *    B:      b+bb++++
       *    (a and both b's should be in result)
       */
      @Test
      void rule2() {
        final var statement = getStatement();

        // insert the event(s) (and their source(s))
        assertDoesNotThrow(() -> {
          ExternalSource A = new ExternalSource(
              "A",
              st,
              dg,
              "2024-01-01T00:00:00Z",
              "2024-01-01T00:00:00Z",
              "2024-01-01T01:00:00Z",
              ca,
              mt);
          ExternalSource B = new ExternalSource(
              "B",
              st,
              dg,
              "2024-01-02T00:00:00Z",
              "2024-01-01T00:30:00Z",
              "2024-01-01T01:30:00Z",
              ca,
              mt);

          ExternalEvent e = createEvent("a", "2024-01-01T00:25:00Z", "00:10:00", A); // spills into B
          ExternalEvent b1 = createEvent("b1", "2024-01-01T00:30:00Z", "00:10:00", B);
          ExternalEvent b2 = createEvent("b2", "2024-01-01T00:45:00Z", "00:10:00", B);

          merlinHelper.insertTypesForEvent(e, A);
          merlinHelper.insertTypesForEvent(b1, B);
          merlinHelper.insertTypesForEvent(b2, B);
        });

        // verify the expected keys
        assertDoesNotThrow(() -> {
          final var res = statement.get().executeQuery(
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
      /**
       * Test a basic case of rule 3, wherein a superceding source (regardless of empty space) nullifies anything in A
       *    based on the _start time_ of the event occurring during the range of a newer source.
       *
       *    A:    +a+aaaaa
       *    B:  b+bb++++
       *    (only b's should be in result)
       */
      @Test
      void rule3_basic() {
        final var statement = getStatement();

        // insert the event(s) (and their source(s))
        assertDoesNotThrow(() -> {
          ExternalSource A = new ExternalSource(
              "A",
              st,
              dg,
              "2024-01-01T00:00:00Z",
              "2024-01-01T00:30:00Z",
              "2024-01-01T01:30:00Z",
              ca,
              mt);
          ExternalSource B = new ExternalSource(
              "B",
              st,
              dg,
              "2024-01-02T00:00:00Z",
              "2024-01-01T00:00:00Z",
              "2024-01-01T01:00:00Z",
              ca,
              mt);

          ExternalEvent e1 = createEvent("a1", "2024-01-01T00:40:00Z", "00:10:00", A); // negated by B, very clearly
          ExternalEvent e2 = createEvent(
              "a2",
              "2024-01-01T00:55:00Z",
              "00:35:00",
              A); // even empty space in B neg should negate
          ExternalEvent b1 = createEvent("b1", "2024-01-01T00:00:00Z", "00:10:00", B);
          ExternalEvent b2 = createEvent("b2", "2024-01-01T00:30:00Z", "00:20:00", B);

          merlinHelper.insertTypesForEvent(e1, A);
          merlinHelper.insertTypesForEvent(e2, A);
          merlinHelper.insertTypesForEvent(b1, B);
          merlinHelper.insertTypesForEvent(b2, B);
        });

        // verify the expected keys
        assertDoesNotThrow(() -> {
          final var res = statement.get().executeQuery(
              // language-sql
              """
              SELECT * FROM merlin.derived_events ORDER BY start_time;
              """
          );

          String[] expected_keys = {"b1", "b2"};
          compareLists(expected_keys, res, "event_key");
        });
      }

      /**
       * Tests rule 3, wherein a superceding source nullifies anything in A based on the _start time_ of the event
       *    occurring during the range of a newer source, but with an entirely empty source.
       *
       *    A:    +a+aaaaa
       *    B:  ++++++++
       *    (empty result)
       */
      @Test
      void rule3_empty() {
        final var statement = getStatement();

        // insert the event(s) (and their source(s))
        assertDoesNotThrow(() -> {
          ExternalSource A = new ExternalSource(
              "A",
              st,
              dg,
              "2024-01-01T00:00:00Z",
              "2024-01-01T00:30:00Z",
              "2024-01-01T01:30:00Z",
              ca,
              mt);
          ExternalSource B = new ExternalSource(
              "B",
              st,
              dg,
              "2024-01-02T00:00:00Z",
              "2024-01-01T00:00:00Z",
              "2024-01-01T01:00:00Z",
              ca,
              mt);

          ExternalEvent e1 = createEvent("a1", "2024-01-01T00:40:00Z", "00:10:00", A); // negated by empty space
          ExternalEvent e2 = createEvent("a2", "2024-01-01T00:55:00Z", "00:35:00", A); // negated by empty space

          merlinHelper.insertTypesForEvent(e1, A);
          merlinHelper.insertTypesForEvent(e2, A);

          // insert B as a source
          statement.get().executeUpdate(
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

        // verify expected keys (none)
        assertDoesNotThrow(() -> {
          final var res = statement.get().executeQuery(
              // language-sql
              """
              SELECT * FROM merlin.derived_events ORDER BY start_time;
              """
          );

          assertFalse(res.next());
        });
      }

      ////////////////////////// RULE 4 //////////////////////////
      /**
       * Tests rule 4, where we require that later sources with events bearing the same key as an event in an earlier
       *    source replace said events. To test this, while not conflating with any other cases tested above, we use
       *    completely sparse sources, just to purely illustrate rule 4 and that it works.
       *
       *    A:                   ++++aaa+++++
       *    B:                                  +++++aaaaa+
       *    C:   +++++aaaa+++++
       *    (one A, of specific duration)
       */
      @Test
      void rule4() {
        final var statement = getStatement();

        // insert the event(s) (and their source(s))
        assertDoesNotThrow(() -> {
          ExternalSource A = new ExternalSource(
              "A",
              st,
              dg,
              "2024-01-01T00:00:00Z",
              "2024-01-01T01:30:00Z",
              "2024-01-01T02:30:00Z",
              ca,
              mt);
          ExternalSource B = new ExternalSource(
              "B",
              st,
              dg,
              "2024-01-02T00:00:00Z",
              "2024-01-01T03:00:00Z",
              "2024-01-01T04:00:00Z",
              ca,
              mt);
          ExternalSource C = new ExternalSource(
              "C",
              st,
              dg,
              "2024-01-03T00:00:00Z",
              "2024-01-01T00:00:00Z",
              "2024-01-01T01:00:00Z",
              ca,
              mt);

          ExternalEvent e1 = createEvent("a", "2024-01-01T01:50:00Z", "00:10:00", A); // negated by empty space
          ExternalEvent e2 = createEvent("a", "2024-01-01T03:40:00Z", "00:15:00", B); // negated by empty space
          ExternalEvent e3 = createEvent("a", "2024-01-01T00:30:00Z", "00:20:00", C); // negated by empty space

          merlinHelper.insertTypesForEvent(e1, A);
          merlinHelper.insertTypesForEvent(e2, B);
          merlinHelper.insertTypesForEvent(e3, C);
        });

        // verify expected keys
        assertDoesNotThrow(() -> {
          final var res = statement.get().executeQuery(
              // language-sql
              """
              SELECT * FROM merlin.derived_events ORDER BY start_time;
              """
          );

          String[] expected_keys = {"a"};
          // only 1 expected result
          assertTrue(res.next());
          assertEquals("a", res.getString("event_key"));
          assertEquals("00:20:00", res.getString("duration"));
          assertEquals("C", res.getString("source_key"));
          assertEquals("{[\"2024-01-01 00:00:00+00\",\"2024-01-01 01:00:00+00\")}", res.getString("source_range"));
          assertFalse(res.next());
        });
      }
    }
  }


  /*
    ---------- CONSTRAINT TESTS ----------
   */
  /**
   * The focus of this class is to provide a final set of tests to test primary and significant constraints to prove
   *    that they work and how they work. These constraints are baked into the schema, so extensive tests of foreign or
   *    unique keys are not excluded except for common use cases/easily made mistakes, as PSQL guarantees functionality
   *    for us.
   * We seek to demonstrate in this class, and verify as a sanity check.
   */
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class ConstraintTests {

    /**
     * Generally, no two sources in a derivation group, regardless of a lack of overlap, can have the same valid_at. We
     *    demonstrate this with sparse sources. In this test, we don't even require events to demonstrate this behavior.
     */
    @Test
    void sameValid_at() {
      final var statement = getStatement();

      // construct the sources
      ExternalSource a = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T03:00:00Z",
          "2024-01-01T04:00:00Z",
          ca,
          mt);
      ExternalSource b = new ExternalSource(
          "B",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T01:00:00Z",
          "2024-01-01T02:00:00Z",
          ca,
          mt);

      // create types and first source
      assertDoesNotThrow(() -> {
        statement.get().executeUpdate(
            //language-sql
            """
            INSERT INTO merlin.external_source_type VALUES ('%s')
            """.formatted(st)
        );

        statement.get().executeUpdate(
            //language-sql
            """
            INSERT INTO merlin.derivation_group VALUES ('%s', '%s')
            """.formatted(dg, st)
        );

        statement.get().executeUpdate(
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

      // second source should fail
      assertThrowsExactly(PSQLException.class, () -> {
        statement.get().executeUpdate(
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

    /**
     * A given source can have two (or more) events occurring at the same time, with all fields except the keys
     *    (see noDuplicateEventsInSameSource). This test demonstrates that behavior without error.
     */
    @Test
    void nEventsAtSameTime() {
      final var statement = getStatement();

      // construct the sources and events
      ExternalSource A = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T00:00:00Z",
          "2024-01-01T01:30:00Z",
          ca,
          mt);

      ExternalEvent e1 = createEvent("a", "2024-01-01T00:00:00Z", "00:10:00", A);
      ExternalEvent e2 = createEvent("b", "2024-01-01T00:00:00Z", "00:05:00", A);
      ExternalEvent e3 = createEvent("c", "2024-01-01T00:00:00Z", "00:15:00", A);

      assertDoesNotThrow(() -> {
        merlinHelper.insertTypesForEvent(e1, A);
        merlinHelper.insertTypesForEvent(e2, A);
        merlinHelper.insertTypesForEvent(e3, A);
      });

      // all 3 keys should be present!
      assertDoesNotThrow(() -> {
        var res = statement.get().executeQuery("SELECT * FROM merlin.derived_events ORDER BY start_time, event_key ASC");
        String[] expected_keys = {"a", "b", "c"};
        compareLists(expected_keys, res, "event_key");
      });
    }

    /**
     * Two events, even if totally sparse, bearing same key in same source, cannot be input. We explicitly write a test
     *    to demonstrate that this behavior causes an error, despite being as simple as a primary key collision.
     */
    @Test
    void noDuplicateEventsInSameSource() {
      // construct the sources and events
      ExternalSource A = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T00:00:00Z",
          "2024-01-01T01:30:00Z",
          ca,
          mt);

      ExternalEvent e1 = createEvent("a", "2024-01-01T00:00:00Z", "00:10:00", A);
      ExternalEvent e2 = createEvent("a", "2024-01-01T00:55:00Z", "00:15:00", A); // illegal!

      // uploading is fine for the first event, naturally
      assertDoesNotThrow(() -> merlinHelper.insertTypesForEvent(e1, A));

      // but fails on collision!
      assertThrowsExactly(PSQLException.class, () -> merlinHelper.insertTypesForEvent(e2, A));
    }

    /**
     * For a given source, the end time must not be less than or equal to the start time. It must be greater than.
     */
    @Test
    void endTimeGEstartTime() {
      final var statement = getStatement();

      // construct the sources
      ExternalSource failing = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T01:00:00Z",
          "2024-01-01T00:30:00Z",
          ca,
          mt);
      ExternalSource failing2 = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T01:00:00Z",
          "2024-01-01T01:00:00Z",
          ca,
          mt);
      ExternalSource succeeding = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T01:00:00Z",
          "2024-01-01T01:00:00.000001Z",
          ca,
          mt);

      // add source type and derivation group
      assertDoesNotThrow(() -> {
        // create the source type
        statement.get().executeUpdate(
            // language-sql
            """
            INSERT INTO
              merlin.external_source_type
            VALUES ('%s')
            ON CONFLICT(name) DO NOTHING;
            """.formatted(failing.source_type_name())
        );

        // create the derivation_group
        statement.get().executeUpdate(
            // language-sql
            """
            INSERT INTO
              merlin.derivation_group
            VALUES ('%s', '%s')
            ON CONFLICT(name, source_type_name) DO NOTHING;
            """.formatted(failing.derivation_group_name(), failing.source_type_name())
        );
      });

      // if start time > end time, error
      assertThrowsExactly(PSQLException.class, () -> {
        statement.get().executeUpdate(
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

      // if start time = end time, error
      assertThrowsExactly(PSQLException.class, () -> {
        statement.get().executeUpdate(
            // language-sql
            """
            INSERT INTO
              merlin.external_source
            VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
            ON CONFLICT(key, derivation_group_name) DO NOTHING;
            """.formatted(
                failing2.key(),
                failing2.source_type_name(),
                failing2.derivation_group_name(),
                failing2.valid_at(),
                failing2.start_time(),
                failing2.end_time(),
                failing2.created_at(),
                failing2.metadata()
            )
        );
      });

      // else, no error
      assertDoesNotThrow(() -> {
        statement.get().executeUpdate(
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

    /**
     * An error should be encountered if an event is out of bounds of the source (i.e. the event:
     *    - starts or ends before, or
     *    - starts or ends after)
     *
     *    Source :      +++++++++++++++
     *    Before1: 1111
     *    Before2:    2222
     *    After1 :                   3333
     *    After2 :                      4444
     */
    @Test
    void externalEventSourceBounds() {
      // create sources and events
      ExternalSource A = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T01:00:00Z",
          "2024-01-01T02:00:00Z",
          ca,
          mt);

      ExternalEvent legal = createEvent("a", "2024-01-01T01:00:00Z", "00:10:00", A); // legal.
      ExternalEvent completelyBefore = createEvent("completelyBefore", "2024-01-01T00:00:00Z", "00:10:00", A); // illegal!
      ExternalEvent beforeIntersect = createEvent("beforeIntersect", "2024-01-01T00:55:00Z", "00:25:00", A); // illegal!
      ExternalEvent afterIntersect = createEvent("afterIntersect", "2024-01-01T01:45:00Z", "00:30:00", A); // illegal!
      ExternalEvent completelyAfter = createEvent("completelyAfter", "2024-01-01T02:10:00Z", "00:15:00", A); // illegal!

      // assert the legal event is okay (in the center of the source)
      assertDoesNotThrow(() -> {
        merlinHelper.insertTypesForEvent(legal, A);
      });

      // assert out of bounds failures
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

    /**
     * Within a derivation group two sources can bear the same key, but they can across different derivation groups (and
     *    therefore different source types).
     */
    @Test
    void duplicateSource() {
      final var statement = getStatement();

      ExternalSource failing = new ExternalSource(
          "Derivation_Test_00.json",
          st,
          dg,
          "2024-01-18 00:00:00+00",
          "2024-01-05 00:00:00+00",
          "2024-01-11 00:00:00+00",
          ca,
          mt
      ); // same name and dg
      ExternalSource succeeding = new ExternalSource(
          "Derivation_Test_00.json",
          st,
          dg + "_2",
          "2024-01-18 00:00:00+00",
          "2024-01-05 00:00:00+00",
          "2024-01-11 00:00:00+00",
          ca,
          mt
      ); // same name, diff dg

      // upload general data
      assertDoesNotThrow(() -> upload_source(dg));

      // upload a conflicting source (same name in a given dg)
      assertThrowsExactly(PSQLException.class, () -> {
        statement.get().executeUpdate(
            // language-sql
            """
            INSERT INTO
              merlin.external_source
            VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
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

      // upload a non-conflicting source (same name in a different dg)
      assertDoesNotThrow(() -> {
        statement.get().executeUpdate(
            // language-sql
            """
            INSERT INTO merlin.derivation_group VALUES ('%s', '%s')
            """.formatted(dg + "_2", st)
        );
        statement.get().executeUpdate(
            // language-sql
            """
            INSERT INTO
              merlin.external_source
            VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
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

    /**
     * No two derivation groups, even across different source types, can have the same name.
     */
    @Test
    void duplicatedDG() {
      final var statement = getStatement();

      assertDoesNotThrow(() -> {
        // create a derivation group of the Test type
        upload_source(dg);
        statement.set(connection.createStatement());
        statement.get().executeUpdate("INSERT INTO merlin.external_source_type VALUES ('New Name');");
      });

      assertThrowsExactly(PSQLException.class, () -> {
        // use the same name as before (Test Default) with a different source type (New Name) - fails
        // (This is noteworthy as this is newer behavior.)
        statement.get().executeUpdate("INSERT INTO merlin.derivation_group VALUES ('%s', 'New Name');".formatted(dg));
      });
    }

    /**
     * It is impossible to delete a derivation group if there is an entry in the plan-derivation group link table with
     *    it. This is caught explicitly in the frontend (aerie-ui), but for alternative frontends like a CLI, we should
     *    catch it at this (PSQL) level. We demonstrate that here.
     */
    @Test
    void deleteDGwithRemainingPlanLink() {
      final var statement = getStatement();

      // create all
      assertDoesNotThrow(() -> {
        statement.set(connection.createStatement());

        // create plan with minimal input (doesn't require a model!)
        statement.get().executeUpdate("INSERT INTO merlin.plan (name, duration, start_time) VALUES ('%s', '%s', '%s');"
                                          .formatted("sample_plan", "00:20:00", "2024-01-01"));

        // create a source type (no sources)
        statement.get().executeUpdate("INSERT INTO merlin.external_source_type VALUES ('%s');"
                                          .formatted(st));

        // create a Derivation Group (no sources)
        statement.get().executeUpdate(
            "INSERT INTO merlin.derivation_group (name, source_type_name) VALUES ('%s', '%s');"
                .formatted(dg, st));

        // create a link
        statement.get().executeUpdate("INSERT INTO merlin.plan_derivation_group VALUES ('%s', '%s');"
                                          .formatted("1", dg));
      });

      // delete the DG (expect error)
      assertThrowsExactly(PSQLException.class, () -> {
        statement.get().executeUpdate("DELETE FROM merlin.plan_derivation_group WHERE name='%s';"
                                          .formatted(dg));
      });
    }

    /**
     * It is impossible to delete a derivation group if there are any sources that are a part of it.
     */
    @Test
    void deleteDGwithRemainingSource() {
      final var statement = getStatement();

      // create the source
      ExternalSource src = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T00:00:00Z",
          "2024-01-01T00:30:00Z",
          ca,
          mt);

      // insert the source and all relevant groups and types
      assertDoesNotThrow(() -> {
        statement.set(connection.createStatement());

        // create a source type
        statement.get().executeUpdate("INSERT INTO merlin.external_source_type VALUES ('%s');"
                                          .formatted(st));

        // create a Derivation Group
        statement.get().executeUpdate(
            "INSERT INTO merlin.derivation_group (name, source_type_name) VALUES ('%s', '%s');"
                .formatted(dg, st));

        // create a source
        statement.get().executeUpdate(
            """
            INSERT INTO
              merlin.external_source
            VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
            ON CONFLICT(key, derivation_group_name) DO NOTHING;
            """.formatted(
                src.key(),
                src.source_type_name(),
                src.derivation_group_name(),
                src.valid_at(),
                src.start_time(),
                src.end_time(),
                src.created_at(),
                src.metadata()
            )
        );
      });

      // delete the DG (expect error)
      assertThrowsExactly(PSQLException.class, () -> {
        statement.get().executeUpdate("DELETE FROM merlin.derivation_group WHERE name='%s';"
                                          .formatted(dg));
      });
    }

    /**
     * It should not be possible to delete a source type with a source still associated.
     */
    @Test
    void deleteSourceTypeWithRemainingSource() {
      final var statement = getStatement();

      // create source
      ExternalSource src = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T00:00:00Z",
          "2024-01-01T00:30:00Z",
          ca,
          mt);

      // add types
      assertDoesNotThrow(() -> {
        statement.set(connection.createStatement());

        // create a source type
        statement.get().executeUpdate("INSERT INTO merlin.external_source_type VALUES ('%s');"
                                          .formatted(st));

        // create a Derivation Group
        statement.get().executeUpdate(
            "INSERT INTO merlin.derivation_group (name, source_type_name) VALUES ('%s', '%s');"
                .formatted(dg, st));

        // create a source
        statement.get().executeUpdate("""
                                          INSERT INTO
                                            merlin.external_source
                                          VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')
                                          ON CONFLICT(key, derivation_group_name) DO NOTHING;
                                          """.formatted(
            src.key(),
            src.source_type_name(),
            src.derivation_group_name(),
            src.valid_at(),
            src.start_time(),
            src.end_time(),
            src.created_at(),
            src.metadata()
        ));
      });

      // delete the source type (expect error)
      assertThrowsExactly(PSQLException.class, () -> {
        statement.get().executeUpdate("DELETE FROM merlin.external_source_type WHERE name='%s';"
                                          .formatted(st));
      });
    }

    /**
     * It should not be possible to delete an event type with an event still associated.
     */
    @Test
    void deleteEventTypeWithRemainingEvent() {
      final var statement = getStatement();

      // create source and event
      ExternalSource src = new ExternalSource(
          "A",
          st,
          dg,
          "2024-01-01T00:00:00Z",
          "2024-01-01T00:00:00Z",
          "2024-01-01T00:30:00Z",
          ca,
          mt);
      ExternalEvent evt = createEvent("A_1", "2024-01-01T00:00:00Z", "00:05:0", src);

      // insert the event and her types
      assertDoesNotThrow(() -> {
        statement.set(connection.createStatement());

        // insert the event and all types
        merlinHelper.insertTypesForEvent(evt, src);
      });

      // delete the event type (expect error)
      assertThrowsExactly(PSQLException.class, () -> {
        statement.get().executeUpdate("DELETE FROM merlin.external_event_type WHERE name='%s';"
                                          .formatted(et));
      });
    }
  }


  /*
    ---------- FINAL/MISC TESTS ----------
   */
  /**
   * Finally, we include an extra test. This is a comprehensive test for derived events that manages several derivation
   *    groups (which entails repeating basicDerivedEvents twice but the second round bears a new DG name. Then, we
   *    verify that there is no overlap! Note that this test is effectively vacuous but is a good sanity check.).
   */
  @Test
  void superDerivedEvents() {
    final var statement = getStatement();

    // upload all source data, but twice (using different dgs, to prove non overlap in derivation)
    String dg2 = dg + "_2";

    // upload the data once for the first derivation group
    assertDoesNotThrow(() -> upload_source(dg));

    // repeat (explicitly, for ease of implementation) with the second derivation group
    assertDoesNotThrow(() -> {
      // insert derivation groups
      statement.get().executeUpdate(
          // language=sql
          """
          INSERT INTO merlin.derivation_group VALUES ('%s', '%s');
          """.formatted(dg2, st)
      );

      // insert external sources
      statement.get().executeUpdate(
          // language=sql
          """
          INSERT INTO merlin.external_source VALUES ('Derivation_Test_00_1.json', '%s', '%s', '2024-01-18 00:00:00+00', '2024-01-05 00:00:00+00', '2024-01-11 00:00:00+00', '2024-08-21 22:36:12.858009+00', '{}');
          INSERT INTO merlin.external_source VALUES ('Derivation_Test_01_1.json', '%s', '%s', '2024-01-19 00:00:00+00', '2024-01-01 00:00:00+00', '2024-01-07 00:00:00+00', '2024-08-21 22:36:19.381275+00', '{}');
          INSERT INTO merlin.external_source VALUES ('Derivation_Test_02_1.json', '%s', '%s', '2024-01-20 00:00:00+00', '2024-01-03 00:00:00+00', '2024-01-10 00:00:00+00', '2024-08-21 22:36:23.340941+00', '{}');
          INSERT INTO merlin.external_source VALUES ('Derivation_Test_03_1.json', '%s', '%s', '2024-01-21 00:00:00+00', '2024-01-01 12:00:00+00', '2024-01-02 12:00:00+00', '2024-08-21 22:36:28.365244+00', '{}');
          """.formatted(st, dg2, st, dg2, st, dg2, st, dg2)
      );

      // insert external events
      statement.get().executeUpdate(
          // language=sql
          """
          INSERT INTO merlin.external_event VALUES ('2', 'DerivationD', 'Derivation_Test_00_1.json', '%s', '2024-01-05 23:00:00+00', '01:10:00', '{"notes": "subsumed by test 01, even though end lies outside of 01, also replaced by test 01 by key", "rules": [3, 4], "should_present": false}');
          INSERT INTO merlin.external_event VALUES ('7', 'DerivationC', 'Derivation_Test_00_1.json', '%s', '2024-01-09 23:00:00+00', '02:00:00', '{"notes": "subsumed by test 02, even though end lies outside of 02, because start time during 01", "rules": [3], "should_present": false}');
          INSERT INTO merlin.external_event VALUES ('8', 'DerivationB', 'Derivation_Test_00_1.json', '%s', '2024-01-10 11:00:00+00', '01:05:00', '{"notes": "after everything, subsumed by nothing despite being from oldest file", "rules": [1], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('1', 'DerivationA', 'Derivation_Test_01_1.json', '%s', '2024-01-01 00:00:00+00', '02:10:00', '{"notes": "before everything, subsumed by nothing", "rules": [1], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('2', 'DerivationA', 'Derivation_Test_01_1.json', '%s', '2024-01-01 12:00:00+00', '02:10:00', '{"notes": "overwritten by key in later file, even with type change", "rules": [4], "should_present": false}');
          INSERT INTO merlin.external_event VALUES ('3', 'DerivationB', 'Derivation_Test_01_1.json', '%s', '2024-01-02 23:00:00+00', '03:00:00', '{"notes": "starts before next file though occurs during next file, still included", "rules": [2], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('4', 'DerivationB', 'Derivation_Test_01_1.json', '%s', '2024-01-05 21:00:00+00', '03:00:00', '{"notes": "start subsumed by 02, not included in final result", "rules": [3], "should_present": false}');
          INSERT INTO merlin.external_event VALUES ('5', 'DerivationC', 'Derivation_Test_02_1.json', '%s', '2024-01-05 23:00:00+00', '01:10:00', '{"notes": "not subsumed, optionally change this event to have key 6 and ensure this test fails", "rules": [1], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('6', 'DerivationC', 'Derivation_Test_02_1.json', '%s', '2024-01-06 12:00:00+00', '02:00:00', '{"notes": "not subsumed", "rules": [1], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('2', 'DerivationB', 'Derivation_Test_02_1.json', '%s', '2024-01-09 11:00:00+00', '01:05:00', '{"notes": "replaces 2 in test 01, despite different event type", "rules": [4], "should_present": true}');
          INSERT INTO merlin.external_event VALUES ('9', 'DerivationC', 'Derivation_Test_03_1.json', '%s', '2024-01-02 00:00:00+00', '01:00:00', '{"notes": "not subsumed", "rules": [1], "should_present": true}');
          """.formatted(dg2, dg2, dg2, dg2, dg2, dg2, dg2, dg2, dg2, dg2, dg2)
      );
    });

    // check that derived events in our prewritten case has the correct keys
    assertDoesNotThrow(() -> {
      // verify everything is present
      var res = statement.get().executeQuery(
          // language-sql
          """
          SELECT * FROM merlin.derived_events ORDER BY start_time;
          """
      );
      String[] expected_keys = {"1", "1", "9", "9", "3", "3", "5", "5", "6", "6", "2", "2", "8", "8"};
      compareLists(expected_keys, res, "event_key");

      // verify for a given dg expected keys are correct, no overlap inside dg
      res = statement.get().executeQuery(
          // language-sql
          """
          SELECT * FROM merlin.derived_events WHERE derivation_group_name = '%s' ORDER BY start_time;
          """.formatted(dg2)
      );
      String[] expected_keys_2 = {"1", "9", "3", "5", "6", "2", "8"};
      compareLists(expected_keys_2, res, "event_key");
    });
  }
}
