package gov.nasa.jpl.aerie.scheduler.simulation;

import com.google.common.collect.Maps;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

public class SimulationResultsConverter {

  /**
   * convert a simulation driver SimulationResult to a constraint evaluation engine SimulationResult
   *
   * @param driverResults the recorded results of a simulation run from the simulation driver
   * @param planDuration the duration of the plan
   * @return the same results rearranged to be suitable for use by the constraint evaluation engine
   */
  public static gov.nasa.jpl.aerie.constraints.model.SimulationResults convertToConstraintModelResults(
      SimulationResults driverResults, Duration planDuration){
    final var activities =  driverResults.simulatedActivities.entrySet().stream()
                                                             .map(e -> convertToConstraintModelActivityInstance(e.getKey().id(), e.getValue(), driverResults.startTime))
                                                             .collect(Collectors.toList());
    return new gov.nasa.jpl.aerie.constraints.model.SimulationResults(
        Interval.between(Duration.ZERO, planDuration),
        activities,
        Maps.transformValues(driverResults.realProfiles, $ -> LinearProfile.fromSimulatedProfile($.getRight())),
        Maps.transformValues(driverResults.discreteProfiles, $ -> DiscreteProfile.fromSimulatedProfile($.getRight()))
    );
  }

  /**
   * convert an activity entry output by the simulation driver to one suitable for the constraint evaluation engine
   *
   * @param id the name of the activity instance
   * @param driverActivity the completed activity instance details from a driver SimulationResult
   * @return an activity instance suitable for a constraint model SimulationResult
   */
  public static gov.nasa.jpl.aerie.constraints.model.ActivityInstance convertToConstraintModelActivityInstance(
      long id, SimulatedActivity driverActivity, final Instant startTime)
  {
    final var startT = Duration.of(startTime.until(driverActivity.start(), ChronoUnit.MICROS), MICROSECONDS);
    final var endT = startT.plus(driverActivity.duration());
    final var activityInterval = startT.isEqualTo(endT)
        ? Interval.between(startT, endT)
        : Interval.betweenClosedOpen(startT, endT);
    return new gov.nasa.jpl.aerie.constraints.model.ActivityInstance(
        id, driverActivity.type(), driverActivity.arguments(),
        activityInterval);
  }
}
