package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.json.Uncurry.untuple;

public record ProfileSet(
    Map<String, Pair<ValueSchema, List<ProfileSegment<Optional<RealDynamics>>>>> realProfiles,
    Map<String, Pair<ValueSchema, List<ProfileSegment<Optional<SerializedValue>>>>> discreteProfiles
) {
  public static ProfileSet ofNullable(
      final Map<String, Pair<ValueSchema, List<ProfileSegment<Optional<RealDynamics>>>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<ProfileSegment<Optional<SerializedValue>>>>> discreteProfiles
  ) {
    return new ProfileSet(
        realProfiles,
        discreteProfiles
    );
  }
  public static ProfileSet of(
      final Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles
  ) {
    return new ProfileSet(
        wrapInOptional(realProfiles),
        wrapInOptional(discreteProfiles)
    );
  }

  public static <T> Map<String, Pair<ValueSchema, List<ProfileSegment<Optional<T>>>>> wrapInOptional(
      final Map<String, Pair<ValueSchema, List<ProfileSegment<T>>>> profileMap
  ) {
    return profileMap
      .entrySet().stream()
      .map($ -> Pair.of(
          $.getKey(),
          Pair.of(
              $.getValue().getLeft(),
              $.getValue().getRight()
               .stream()
               .map(untuple(segment -> new ProfileSegment<>(segment.extent(), Optional.of(segment.dynamics()))))
               .toList()
          )
      ))
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }

  public static <T> Map<String, Pair<ValueSchema, List<ProfileSegment<T>>>> unwrapOptional(
      final Map<String, Pair<ValueSchema, List<ProfileSegment<Optional<T>>>>> profileMap
  ) throws NoSuchElementException {
    return profileMap
        .entrySet().stream()
        .map($ -> Pair.of(
            $.getKey(),
            Pair.of(
                $.getValue().getLeft(),
                $.getValue().getRight()
                 .stream()
                 .map(segment -> new ProfileSegment<>(segment.extent(), segment.dynamics().get()))
                 .toList()
            )
        ))
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }
}
