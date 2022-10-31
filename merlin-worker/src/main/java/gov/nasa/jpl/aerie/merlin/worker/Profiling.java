package gov.nasa.jpl.aerie.merlin.worker;

import com.impossibl.postgres.jdbc.PGDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.config.PostgresStore;
import gov.nasa.jpl.aerie.merlin.server.config.Store;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostProfilesAction;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.SimulationRecord;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static gov.nasa.jpl.aerie.merlin.server.http.ProfileParsers.realDynamicsP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository.makePartitions;

public final class Profiling {
  public static void main(String[] args) throws Exception {
    profileFullWrite();
//    profileSerializationOnly();
//    profileDatabaseInsertsOnly();
  }

  private static void profileSerializationOnly() {
    final var results = makeSimResults();
    for (final var profileEntry : results.realProfiles.entrySet()) {
      for (final var segment : profileEntry.getValue().getRight()) {
        realDynamicsP.unparse(segment.getRight());
      }
    }
  }

  private static void profileDatabaseInsertsOnly() throws SQLException, ExecutionException, InterruptedException {
    final var configuration = loadConfiguration();
    final var store = configuration.store();

    if (!(store instanceof final PostgresStore postgresStore)) {
      throw new UnexpectedSubtypeError(Store.class, store);
    }
    final var pgDataSource = new PGDataSource();
    pgDataSource.setServerName(postgresStore.server());
    pgDataSource.setPortNumber(postgresStore.port());
    pgDataSource.setDatabaseName(postgresStore.database());
    pgDataSource.setApplicationName("Merlin Server");
    pgDataSource.setApiTrace(true);
    pgDataSource.setApiTraceFile("/Users/dailis/projects/AERIE/aerie/trace-api.log");
    System.out.println("API trace file: " + pgDataSource.getApiTraceFile());
    pgDataSource.setSqlTrace(true);
    pgDataSource.setSqlTraceFile("/Users/dailis/projects/AERIE/aerie/trace-sql.log");
    System.out.println("SQL trace file: " + pgDataSource.getSqlTraceFile());

    final var hikariConfig = new HikariConfig();
    hikariConfig.setUsername(postgresStore.user());
    hikariConfig.setPassword(postgresStore.password());
    hikariConfig.setDataSource(pgDataSource);

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    final int datasetId;
    {
      final var connection = hikariDataSource.getConnection();
      try (final var statement = connection.createStatement()) {
        final var resultSet = statement.executeQuery("""
            insert into dataset                                                                                                                                                              \s
            default values                                                                                                                                                                   \s
            returning id;
         """);
        resultSet.next();
        datasetId = resultSet.getInt("id");
      }
    }

    makePartitions(hikariDataSource.getConnection(), datasetId);

    final var results = makeSimResults();
    final var serializationStartTime = System.nanoTime();
    final Map<String, Pair<ValueSchema, List<Pair<Duration, String>>>> realProfiles = new HashMap<>();
    for (final var entry : results.realProfiles.entrySet()) {
      final var profile = new ArrayList<Pair<Duration, String>>();
      var accumulatedOffset = Duration.ZERO;
      for (final var segment : entry.getValue().getRight()) {
        profile.add(Pair.of(accumulatedOffset, serializeDynamics(segment.getRight(), realDynamicsP)));
        accumulatedOffset = Duration.add(accumulatedOffset, segment.getLeft());
      }
      realProfiles.put(entry.getKey(), Pair.of(entry.getValue().getLeft(), profile));
    }
    final var serializationEndTime = (System.nanoTime() - serializationStartTime) / 1000000000.0;
    System.out.println("-----------------------------------");
    System.out.printf("Serialization completed after %f seconds%n", serializationEndTime);

    final var startTime = System.nanoTime();
    try (final var threadPool = Executors.newFixedThreadPool(2)) {
      final var connection = hikariDataSource.getConnection();
      final var statement = connection.prepareStatement("""
          insert into profile_segment (dataset_id, profile_id, start_offset, dynamics)
          values (?, ?, ?, ?)
        """);
      try (final var postProfilesAction = new PostProfilesAction(connection)) {
        final var records = postProfilesAction.apply(
            datasetId,
            results.realProfiles,
            results.discreteProfiles);
        for (final var entry : records.entrySet()) {
          final var record =  entry.getValue();
          final var resource =  entry.getKey();
          final var type = record.type().getLeft();
          if ("real".equals(type)) {
            // Each profile segment's duration part is the duration for which the dynamics hold
            // before the next one begins. Since order in the database is not guaranteed
            // we need to convert to offsets from the simulation start so order can be preserved
            threadPool.submit(() -> realProfiles
                .get(resource)
                .getRight()
                .parallelStream()
                .forEach(pair -> {
                  try {
                    final var connection$ = hikariDataSource.getConnection();
                    connection$.setAutoCommit(false);
                    final var startOffset = pair.getLeft();
                    final var serializedDynamics = pair.getRight();
                    final var iso8601duration = java.time.Duration.of(
                        startOffset.in(Duration.MICROSECONDS),
                        ChronoUnit.MICROS).toString();

                    statement.setLong(1, datasetId);
                    statement.setLong(2, record.id());
                    statement.setString(3, iso8601duration);
                    statement.setString(4, serializedDynamics);

                    statement.addBatch();

                    connection$.commit();
                  } catch (SQLException e) {
                    throw new RuntimeException(e);
                  }
                }))
                .get();

//            for (final var pair : realProfiles.get(resource).getRight()) {
//              final var startOffset = pair.getLeft();
//              final var serializedDynamics = pair.getRight();
////              final var timestamp = simulationStart.plusMicros(accumulatedOffset.dividedBy(Duration.MICROSECOND));
//              final var iso8601duration = java.time.Duration.of(startOffset.in(Duration.MICROSECONDS), ChronoUnit.MICROS).toString();
//
//              statement.setLong(1, datasetId);
//              statement.setLong(2, record.id());
////              setTimestamp(statement, 3, timestamp);
////              setTimestamp(statement, 4, simulationStart);
//              statement.setString(3, iso8601duration);
//              statement.setString(4, serializedDynamics);
//
//              statement.addBatch();
//            }

//            final var results1 = statement.executeBatch();
//            connection.commit();
//            for (final var result : results1) {
//              if (result == Statement.EXECUTE_FAILED) throw new FailedInsertException("profile_segment");
//            }
          } else if ("discrete".equals(type)) {
//            try (final var postProfileSegmentsAction = new PostProfileSegmentsAction(connection)) {
//              postProfileSegmentsAction.apply(
//                  datasetId,
//                  record,
//                  discreteProfiles.get(resource).getRight(),
//                  new Timestamp(Instant.EPOCH), serializedValueP);
//            }
            throw new Error("This test only has real profiles");
          } else {
            throw new Error("Unrecognized profile type " + record.type().getLeft());
          }
        }
      }
    }
    final var endTime = (System.nanoTime() - startTime) / 1000000000.0;
    System.out.printf("Writing completed after %f seconds%n", endTime);
    System.out.println("-----------------------------------");
  }

  private static <Dynamics> String serializeDynamics(final Dynamics dynamics, final JsonParser<Dynamics> dynamicsP) {
    return dynamicsP.unparse(dynamics).toString();
  }

  private static void profileFullWrite() throws SQLException {
    final var configuration = loadConfiguration();
    final var store = configuration.store();

    if (!(store instanceof final PostgresStore postgresStore)) {
      throw new UnexpectedSubtypeError(Store.class, store);
    }
    final var pgDataSource = new PGDataSource();
    pgDataSource.setServerName(postgresStore.server());
    pgDataSource.setPortNumber(postgresStore.port());
    pgDataSource.setDatabaseName(postgresStore.database());
    pgDataSource.setApplicationName("Merlin Server");

    final var hikariConfig = new HikariConfig();
    hikariConfig.setUsername(postgresStore.user());
    hikariConfig.setPassword(postgresStore.password());
    hikariConfig.setDataSource(pgDataSource);

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    final int datasetId;
    {
      final var connection = hikariDataSource.getConnection();
      try (final var statement = connection.createStatement()) {
        final var resultSet = statement.executeQuery("""
            insert into dataset                                                                                                                                                              \s
            default values                                                                                                                                                                   \s
            returning id;
         """);
        resultSet.next();
        datasetId = resultSet.getInt("id");
      }
    }

    makePartitions(hikariDataSource.getConnection(), datasetId);

    final var owner = new PostgresResultsCellRepository.PostgresResultsCell(
        hikariDataSource,
        new SimulationRecord(1, 1, 1, Optional.empty(), Map.of()),
        datasetId,
        new Timestamp(Instant.EPOCH));

    final ResultsProtocol.WriterRole writer = owner;
    final var results = makeSimResults();

    final var startTime = System.nanoTime();
    writer.succeedWith(results);
    final var endTime = (System.nanoTime() - startTime) / 1000000000.0;

    System.out.println("-----------------------------------");
    System.out.printf("Writing completed after %f seconds%n", endTime);
    System.out.println("datasetId = " + datasetId);
    System.out.println("-----------------------------------");
  }

  @NotNull
  private static SimulationResults makeSimResults() {
    final var numRealProfiles = 1;
    final var realProfileLength = 200000;
    final Map<String, Pair<ValueSchema, List<Pair<Duration, RealDynamics>>>> realProfiles = new HashMap<>();
    for (var i = 0; i < numRealProfiles; i++) {
      final var profileSegments = new ArrayList<Pair<Duration, RealDynamics>>();
      for (var j = 0; j < realProfileLength; j++) {
        profileSegments.add(Pair.of(Duration.HOUR, RealDynamics.constant(j)));
      }
      realProfiles.put("/real/profile/" + i, Pair.of(ValueSchema.ofStruct(Map.of(
          "initial", ValueSchema.REAL,
          "rate", ValueSchema.REAL)), profileSegments));
    }
    return new SimulationResults(
        realProfiles,
        Map.of(),
        Map.of(),
        Map.of(),
        Instant.EPOCH,
        List.of(),
        new TreeMap<>()
    );
  }

  private static String getEnv(final String key, final String fallback){
    final var env = System.getenv(key);
    return env == null ? fallback : env;
  }

  private static WorkerAppConfiguration loadConfiguration() {
    return new WorkerAppConfiguration(
        Path.of(getEnv("MERLIN_WORKER_LOCAL_STORE", "/usr/src/app/merlin_file_store")),
        new PostgresStore(getEnv("MERLIN_WORKER_DB_SERVER", "localhost"),
                          getEnv("MERLIN_WORKER_DB_USER", "postgres"),
                          Integer.parseInt(getEnv("MERLIN_WORKER_DB_PORT", "5432")),
                          getEnv("MERLIN_WORKER_DB_PASSWORD", "postgres"),
                          getEnv("MERLIN_WORKER_DB", "aerie_merlin")),
        Instant.EPOCH
    );
  }
}
