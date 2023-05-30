package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivityId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ProfileRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.SimulationDatasetRecord;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository.getActivities;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository.getSimulationEvents;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository.getSimulationTopics;

public class PostgresSimulationResultsHandle implements SimulationResultsHandle {

  SimulationDatasetRecord record;
  DataSource dataSource;

  public PostgresSimulationResultsHandle(DataSource dataSource, SimulationDatasetRecord record) {
    this.dataSource = dataSource;
    this.record = record;
  }

  public SimulationResults getSimulationResults() {
    try (final var connection = this.dataSource.getConnection()) {
      final var startTimestamp = record.simulationStartTime();
      final var simulationStart = startTimestamp.toInstant();
      final var simulationDuration = Duration.of(
          startTimestamp.microsUntil(record.simulationEndTime()),
          Duration.MICROSECONDS);

      final var profiles = ProfileRepository.getProfiles(connection, record.datasetId());
      final var activities = getActivities(connection, record.datasetId(), startTimestamp);
      final var topics = getSimulationTopics(connection, record.datasetId());
      final var events = getSimulationEvents(connection, record.datasetId(), startTimestamp);

      return new SimulationResults(
          ProfileSet.unwrapOptional(profiles.realProfiles()),
          ProfileSet.unwrapOptional(profiles.discreteProfiles()),
          activities.getLeft(),
          activities.getRight(),
          simulationStart,
          simulationDuration,
          topics,
          events
      );
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ProfileSet getProfiles(final Iterable<String> profileNames) {
    try (final var connection = this.dataSource.getConnection()) {
      return ProfileRepository.getProfiles(connection, record.datasetId(), profileNames);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<SimulatedActivityId, SimulatedActivity> getSimulatedActivities() {
    try (final var connection = this.dataSource.getConnection()) {
      final var activities = getActivities(
          connection,
          record.datasetId(),
          record.simulationStartTime());
      return activities.getLeft();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Instant startTime() {
    return record.simulationStartTime().toInstant();
  }

  @Override
  public Duration duration() {
    return Duration.of(
        record.simulationStartTime().microsUntil(record.simulationEndTime()),
        Duration.MICROSECONDS);
  }
}
