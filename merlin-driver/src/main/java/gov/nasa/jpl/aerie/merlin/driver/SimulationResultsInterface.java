package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
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
import java.util.Set;

public interface SimulationResultsInterface {

  default String makeString() {
    return
        "SimulationResults "
        + "{ startTime=" + this.getStartTime()
        + ", realProfiles=" + this.getRealProfiles()
        + ", discreteProfiles=" + this.getDiscreteProfiles()
        + ", simulatedActivities=" + this.getSimulatedActivities()
        + ", unfinishedActivities=" + this.getUnfinishedActivities()
        + " }";
  }

  Instant getStartTime();

  Duration getDuration();

  Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> getRealProfiles();

  Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> getDiscreteProfiles();

  Map<SimulatedActivityId, SimulatedActivity> getSimulatedActivities();

  Set<SimulatedActivityId> getRemovedActivities();

  Map<SimulatedActivityId, UnfinishedActivity> getUnfinishedActivities();

  List<Triple<Integer, String, ValueSchema>> getTopics();

  Map<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> getEvents();
}
