package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*package-local*/ record SimulatedActivityRecord(
    String type,
    Map<String, SerializedValue> arguments,
    Instant start,
    Duration duration,
    Optional<Long> parentId,
    List<Long> childIds,
    Optional<Long> directiveId,
    SerializedValue computedAttributes
) {}
