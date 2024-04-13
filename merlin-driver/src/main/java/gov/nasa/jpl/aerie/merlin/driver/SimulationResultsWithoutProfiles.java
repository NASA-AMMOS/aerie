package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public record SimulationResultsWithoutProfiles(
    Instant startTime,
    Duration duration,
    Map<SimulatedActivityId, SimulatedActivity> simulatedActivities,
    Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities,
    List<Triple<Integer, String, ValueSchema>> topics,
    SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events
) {
}
