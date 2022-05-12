package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record UnfinishedActivity(
  String type,
  Map<String, SerializedValue> arguments,
  Instant start,
  ActivityInstanceId parentId,
  List<ActivityInstanceId> childIds,
  Optional<ActivityInstanceId> directiveId
) { }
