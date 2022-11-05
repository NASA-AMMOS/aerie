package gov.nasa.jpl.aerie.merlin.server;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationFailure;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.UnfinishedActivity;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Consumer;

public final class ResultsProtocol {
  private ResultsProtocol() {}

  public sealed interface State {
    /** Simulation in enqueued. */
    record Pending(long simulationDatasetId) implements State {}

    /** Simulation in progress, but no results to share yet. */
    record Incomplete(long simulationDatasetId) implements State {}

    /** Simulation complete -- results now available. */
    record Success(long simulationDatasetId, SimulationResults results) implements State {}

    /** Simulation failed -- don't try to re-run without changing some of the inputs. */
    record Failed(long simulationDatasetId, SimulationFailure reason) implements State {}
  }

  public interface ReaderRole {
    State get();

    /** After calling cancel, `get` is no longer legal to invoke. */
    void cancel();
  }

  public interface WriterRole {
    boolean isCanceled();

    // If the writer aborts because it observes `isCanceled()`,
    //   it must still complete with `failWith()`.
    //   Otherwise, the reader would not be able to reclaim unique ownership
    //   of the underlying resource in order to deallocate it.
    void succeedWith(SimulationResults results);

    void succeed();

    void writeMetadata(SimulationMetadata metadata);

    void writeSegment(SimulationSegment segment);
    void writeActivityInfo(
        Map<ActivityInstanceId, SimulatedActivity> simulatedActivities,
        Map<ActivityInstanceId, UnfinishedActivity> unfinishedActivities);

    void failWith(SimulationFailure reason);

    default void failWith(final Consumer<SimulationFailure.Builder> builderConsumer) {
      final var builder = new SimulationFailure.Builder();
      builderConsumer.accept(builder);
      failWith(builder.build());
    }
  }

  public interface OwnerRole extends ReaderRole, WriterRole {}

  record SimulationMetadata(
      Instant startTime,
      List<Triple<Integer, String, ValueSchema>> topics,
      Map<String, ValueSchema> realProfiles) {}

  record SimulationSegment(
      Duration elapsedTime,
      Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      Map<String, List<Pair<Duration, SerializedValue>>> discreteProfiles,
      SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events
      // TODO activity info
  ) {}
}
