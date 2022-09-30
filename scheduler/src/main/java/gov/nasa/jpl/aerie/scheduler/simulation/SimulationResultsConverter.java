package gov.nasa.jpl.aerie.scheduler.simulation;

import com.google.common.collect.Maps;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
        Maps.transformValues(driverResults.realProfiles, SimulationResultsConverter::convertToConstraintModelLinearProfile),
        Maps.transformValues(driverResults.discreteProfiles, SimulationResultsConverter::convertToConstraintModelDiscreteProfile)
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

  /**
   * convert a linear profile output from the simulation driver to one suitable for the constraint evaluation engine
   *
   * @param driverProfile the as-simulated real profile from a driver SimulationResult
   * @return a real profile suitable for a constraint model SimulationResult, starting from the zero duration
   */
  public static LinearProfile convertToConstraintModelLinearProfile(
      Pair<ValueSchema, List<Pair<Duration, RealDynamics>>> driverProfile)
  {
    final var pieces = new ArrayList<LinearProfilePiece>(driverProfile.getRight().size());
    var elapsed = Duration.ZERO;
    for (final var piece : driverProfile.getRight()) {
      final var extent = piece.getLeft();
      final var value = piece.getRight();
      pieces.add(new LinearProfilePiece(Interval.betweenClosedOpen(elapsed, elapsed.plus(extent)), value.initial, value.rate));
      elapsed = elapsed.plus(extent);
    }
    return new LinearProfile(pieces);
  }

  /**
   * convert a discrete profile output from the simulation driver to one suitable for the constraint evaluation engine
   *
   * @param driverProfile the as-simulated discrete profile from a driver SimulationResult
   * @return a discrete profile suitable for a constraint model SimulationResult, starting from the zero duration
   */
  public static DiscreteProfile convertToConstraintModelDiscreteProfile(
      Pair<ValueSchema, List<Pair<Duration, SerializedValue>>> driverProfile)
  {
    final var pieces = new ArrayList<DiscreteProfilePiece>(driverProfile.getRight().size());
    var elapsed = Duration.ZERO;
    for (final var piece : driverProfile.getRight()) {
      final var extent = piece.getLeft();
      final var value = piece.getRight();
      pieces.add(new DiscreteProfilePiece(Interval.betweenClosedOpen(elapsed, elapsed.plus(extent)), value));
      elapsed = elapsed.plus(extent);
    }
    return new DiscreteProfile(pieces);
  }
}
