package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.Optional;

public record ActivityAttributesRecord(
    Optional<ActivityInstanceId> directiveId,
    Map<String, SerializedValue> arguments
) {}
