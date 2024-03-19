package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.StartOffsetReducer;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SchedulePlanGrounder {
  public static Optional<List<ActivityInstance>> groundSchedule(
      final List<SchedulingActivityDirective> schedulingActivityDirectiveList,
      final Duration planDuration){
    final var grounded = new HashMap<ActivityDirectiveId, ActivityInstance>();
    final var idMap = schedulingActivityDirectiveList.stream().map(a -> Pair.of(new ActivityDirectiveId(a.getId().id()), a)).collect(Collectors.toMap(
        Pair::getLeft, Pair::getRight));
    final var converted = schedulingActivityDirectiveList.stream().map(a ->
                                                                           Pair.of(
                                                                               new ActivityDirectiveId(a.id().id()),
                                                                               new ActivityDirective(
                                                                                a.startOffset(),
                                                                                a.type().getName(),
                                                                                a.arguments(),
                                                                                (a.anchorId() == null) ? null : new ActivityDirectiveId(a.anchorId().id()),
                                                                                a.anchoredToStart()
                                                                           ))).collect(Collectors.toMap(Pair::getLeft,
                                                                                                        Pair::getRight));
    final var converter = new StartOffsetReducer(planDuration, converted);
    var computed = converter.compute();
    computed = StartOffsetReducer.filterOutNegativeStartOffset(computed);

  for(final var entry: computed.entrySet()){
        Duration offset = Duration.ZERO;
        final var idActivity = entry.getKey();
        if(idActivity != null){
          if(grounded.get(idActivity) == null){
            return Optional.empty();
          } else {
            final var alreadyGroundedAct = grounded.get(idActivity);
            offset = alreadyGroundedAct.interval.end;
          }
        }
        for(final var activityAndDependents : entry.getValue()) {
          final var dependentId = activityAndDependents.getKey();
          final var dependentOriginalActivity = idMap.get(dependentId);
          final var startTime = offset.plus(activityAndDependents.getRight());
          //happens only in tests
          if(dependentOriginalActivity.duration() == null){
            return Optional.empty();
          }
          grounded.put(dependentId, new ActivityInstance(
              dependentId.id(),
              dependentOriginalActivity.type().getName(),
              dependentOriginalActivity.arguments(),
              Interval.between(startTime, startTime.plus(dependentOriginalActivity.duration()))));
        }
    }
    return Optional.of(grounded.values().stream().toList());
  }
}
