package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.CheckpointSimulationDriver;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class ResourceAwareSpreadCheckpointPolicy implements BiFunction<Duration, Duration, Boolean>{
  final BiFunction<Duration, Duration, Boolean> function;

  public ResourceAwareSpreadCheckpointPolicy(
      final int resourceCapacity,
      final Duration planningHorizonStart,
      final Duration planningHorizonEnd,
      final Duration subHorizonStart,
      final Duration subHorizonEnd,
      final double discount,
      final boolean endForSure){
    final List<Duration> desiredCheckpoints = new ArrayList<>();
    if(resourceCapacity > 0){
      final var period = planningHorizonEnd.minus(planningHorizonStart).dividedBy((int) (resourceCapacity * discount));
      //for a given planning horizon, we try always hitting the same checkpoint times to increase the probability of
      //already having it saved in the cache
      for (Duration cur = planningHorizonStart.plus(period);
           cur.longerThan(subHorizonStart) && cur.shorterThan(subHorizonEnd);
           cur = cur.plus(period)) {
        desiredCheckpoints.add(cur);
      }
      if (endForSure && !desiredCheckpoints.contains(subHorizonEnd)) desiredCheckpoints.add(subHorizonEnd);
    }
    this.function = CheckpointSimulationDriver.desiredCheckpoints(desiredCheckpoints);
  }

  @Override
  public Boolean apply(final Duration duration, final Duration duration2) {
    return function.apply(duration, duration2);
  }
}
