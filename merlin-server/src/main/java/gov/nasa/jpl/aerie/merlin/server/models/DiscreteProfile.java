package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

public record DiscreteProfile(
    ValueSchema schema,
    List<Pair<Duration, Optional<SerializedValue>>> segments) {}
