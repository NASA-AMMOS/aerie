package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public record UnwrappedProfileSet(
    Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles,
    Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles
){}
