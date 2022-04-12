package gov.nasa.jpl.aerie.scheduler.simulation;

import com.google.common.collect.Lists;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.And;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteValue;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThan;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.LessThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.NotEqual;
import gov.nasa.jpl.aerie.constraints.tree.RealResource;
import gov.nasa.jpl.aerie.constraints.tree.RealValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.ExternalState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Class mocking the behavior of an externally defined resource and implementing ExternalState interface
 */
public class SimResource implements
    ExternalState
{

  private final SimulationFacade facade;
  /** the identifier of this resource for use in properly querying the simulation results */
  private String name;

  /** reference to simulation results that contextualize queries to this resource */
  private SimulationResults simResults;

  TreeMap<Window, SerializedValue> values;

  public SimResource(SimulationFacade facade){
    this.facade = facade;
  }

  public boolean isEmpty() {
    return simResults == null
           || (!simResults.realProfiles.containsKey(name) && !simResults.discreteProfiles.containsKey(name));
  }

  public void failIfEmpty() {
    if (isEmpty()) {
      throw new IllegalArgumentException("Trying to use uninitialized resource (have you simulated before?)");
    }
  }

  final static Supplier<RuntimeException> exceptionType =
      () -> new UnsupportedOperationException("aerie inequality constraints only work with real-valued resources");

  public void initFromSimRes(
      String name,
      SimulationResults simResults,
      List<Pair<Duration, SerializedValue>> fileValues,
      Duration planningHorizonStart)
  {
    this.name = name;
    this.simResults = simResults;

    values = new TreeMap<>();
    Duration start = null;
    SerializedValue val;
    SerializedValue lastVal = null;
    int i = 0;
    for (Pair<Duration, SerializedValue> entry : fileValues) {
      i++;
      val = entry.getValue();

      var time = planningHorizonStart.plus(entry.getKey());
      if (start == null) {
        start = time;
        lastVal = val;
      }
      if (!val.equals(lastVal) || i == fileValues.size()) {
        values.put(Window.betweenClosedOpen(start, time), lastVal);
        start = time;
      }
      lastVal = val;
    }
  }

  /** convert constraint engine windows into scheduler windows, within specified bounding windows */
  private Windows convertToSchedulerWindows(Windows inWindows, Windows queryBounds) {
    final var outWindows = new Windows();
    for (final var inWin : inWindows) {
      final var startT = inWin.start;
      final var endT = inWin.end;
      outWindows.add(Window.betweenClosedOpen(startT, endT));
    }
    outWindows.intersectWith(queryBounds);
    return outWindows;
  }

  public SerializedValue getValueAtTime(Duration t) {
    facade.updateResourcesIfNecessary(t);
    failIfEmpty();

    final var queryT = t;

    //TODO: unify necessary generic profile operations in Profile interface to avoid special casing
    if (this.simResults.realProfiles.containsKey(this.name)) {
      //TODO: improve the profile data structure to allow fast time-keyed query
      //TODO: improve Window to allow querying containment in interval directly
      //for now we look for the last matching profile segment, if any (last to get latest if any overlaps)
      final var profile = this.simResults.realProfiles.get(this.name);
      final Predicate<LinearProfilePiece> containsQueryTimeP = (piece)
          -> !Window.intersect(piece.window, Window.at(queryT)).isEmpty();
      final var matchPiece = Lists.reverse(profile.profilePieces).stream()
                                  .filter(containsQueryTimeP).findFirst();
      final var dblVal = matchPiece.map(piece -> piece.valueAt(queryT)).orElse(null);
      return SerializedValue.of(dblVal);
    } else if (this.simResults.discreteProfiles.containsKey(this.name)) {
      //TODO: improve the profile data structure to allow fast time-keyed query
      //TODO: improve Window to allow querying containment in interval directly
      //for now we look for the last matching profile segment, if any (last to get latest if any overlaps)
      final var profile = this.simResults.discreteProfiles.get(this.name);
      final Predicate<DiscreteProfilePiece> containsQueryTimeP = (piece)
          -> !Window.intersect(piece.window, Window.at(queryT)).isEmpty();
      final var matchPiece = Lists.reverse(profile.profilePieces).stream()
                                  .filter(containsQueryTimeP).findFirst();
      return matchPiece.map(piece -> piece.value).orElse(null);
    } else {
      return null;
    }
  }

  public Windows whenValueBetween(SerializedValue inf, SerializedValue sup, Windows windows) {
    if(!windows.isEmpty()){
      facade.updateResourcesIfNecessary(windows.maxTimePoint().get());
    }

    //special case doubles are the only aerie types that can be compared with inequality constraints
      final var gteConstraint = new GreaterThanOrEqual(new RealResource(this.name), new RealValue(inf.asReal().orElseThrow(exceptionType)));
      final var lteConstraint = new LessThanOrEqual(new RealResource(this.name), new RealValue(sup.asReal().orElseThrow(exceptionType)));
      final var constraint = new And(gteConstraint, lteConstraint);
      final var satisfied = constraint.evaluate(this.simResults);
      return convertToSchedulerWindows(satisfied, windows);

  }

  public Windows whenValueBelow(SerializedValue val, Windows windows) {
    if(!windows.isEmpty()){
      facade.updateResourcesIfNecessary(windows.maxTimePoint().get());
    }
    failIfEmpty();
    //special case doubles are the only aerie types that can be compared with inequality constraints
      final var constraint = new LessThan(new RealResource(this.name), new RealValue(val.asReal().orElseThrow(exceptionType)));
      final var satisfied = constraint.evaluate(this.simResults);
      return convertToSchedulerWindows(satisfied, windows);
  }

  public Windows whenValueAbove(SerializedValue val, Windows windows) {
    if(!windows.isEmpty()){
      facade.updateResourcesIfNecessary(windows.maxTimePoint().get());
    }
    failIfEmpty();

    //special case doubles are the only aerie types that can be compared with inequality constraints
    final var constraint = new GreaterThan(new RealResource(this.name), new RealValue(val.asReal().orElseThrow(exceptionType)));
    final var satisfied = constraint.evaluate(this.simResults);
    return convertToSchedulerWindows(satisfied, windows);
  }

  public Windows whenValueEqual(SerializedValue val, Windows windows) {
    if(!windows.isEmpty()){
      facade.updateResourcesIfNecessary(windows.maxTimePoint().get());
    }
    failIfEmpty();
    var asReal = val.asReal();
    Expression<Windows> constraint;
    if (asReal.isPresent()) {
      //aeire discrete double resources can be promoted to real resources even if discrete, so just do that for all doubles
      final var dblVal = asReal.get();
      constraint = new Equal<>(new RealResource(this.name), new RealValue(dblVal));
    } else {
      //everything else is handled as a discrete resource
        constraint = new Equal<>(new DiscreteResource(this.name), new DiscreteValue(val));
    }
    final var satisfied = constraint.evaluate(this.simResults);
    return convertToSchedulerWindows(satisfied, windows);
  }

  @Override
  public Map<Window, SerializedValue> getTimeline(Windows timeDomain) {
    if(!timeDomain.isEmpty()){
      facade.updateResourcesIfNecessary(timeDomain.maxTimePoint().get());
    }
    return values;
  }

  @Override
  public Windows whenValueNotEqual(SerializedValue val, Windows windows) {
    if(!windows.isEmpty()){
      facade.updateResourcesIfNecessary(windows.maxTimePoint().get());
    }
    failIfEmpty();
    Expression<Windows> constraint;
    var asReal = val.asReal();
    if (asReal.isPresent()) {
      //aeire discrete double resources can be promoted to real resources even if discrete, so just do that for all doubles
      constraint = new NotEqual<>(new RealResource(this.name), new RealValue(asReal.get()));
    } else {
      //everything else is handled as a discrete resource
        constraint = new NotEqual<>(new DiscreteResource(this.name), new DiscreteValue(val));
    }
    final var satisfied = constraint.evaluate(this.simResults);
    return convertToSchedulerWindows(satisfied, windows);
  }

}
