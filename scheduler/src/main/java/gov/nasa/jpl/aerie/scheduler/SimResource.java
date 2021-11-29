package gov.nasa.jpl.aerie.scheduler;

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
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Class mocking the behavior of an externally defined resource and implementing ExternalState interface
 *
 * @param <T> the type of the variable managed by the resource
 */
public class SimResource<T extends Comparable<T>> implements
    ExternalState<T>
{

  /** the identifier of this resource for use in properly querying the simulation results */
  private String name;

  /** reference to simulation results that contextualize queries to this resource */
  private SimulationResults simResults;

  /** the scheduler time stamp at the beginning of the horizon, used to contextualize offset-duration results */
  private Duration horizonStart;

  /** handles conversions between SerializedValue storage type and the desired in-core data type */
  ValueMapper<T> valueMapper;

  TreeMap<Window, T> values;

  public boolean isEmpty() {
    return simResults == null
           || (!simResults.realProfiles.containsKey(name) && !simResults.discreteProfiles.containsKey(name));
  }

  public void failIfEmpty() {
    if (isEmpty()) {
      throw new IllegalArgumentException("Trying to use uninitialized resource (have you simulated before?)");
    }
  }

  public void initFromSimRes(
      String name,
      ValueMapper<T> valueMapper,
      SimulationResults simResults,
      List<Pair<Duration, T>> fileValues,
      Duration planningHorizonStart)
  {
    this.name = name;
    this.valueMapper = valueMapper;
    this.simResults = simResults;
    this.horizonStart = planningHorizonStart;

    values = new TreeMap<>();
    Duration start = null;
    T val;
    T lastVal = null;
    int i = 0;
    for (Pair<Duration, T> entry : fileValues) {
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

  public T getValueAtTime(Duration t) {
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
      //TODO: better type handling from profiles (maybe type vars?)
      //queries on real profiles can only ever return doubles anyway (but what about SimResource<Float>?)
      @SuppressWarnings("unchecked")
      final var castVal = (T) dblVal;
      return castVal;
    } else if (this.simResults.discreteProfiles.containsKey(this.name)) {
      //TODO: improve the profile data structure to allow fast time-keyed query
      //TODO: improve Window to allow querying containment in interval directly
      //for now we look for the last matching profile segment, if any (last to get latest if any overlaps)
      final var profile = this.simResults.discreteProfiles.get(this.name);
      final Predicate<DiscreteProfilePiece> containsQueryTimeP = (piece)
          -> !Window.intersect(piece.window, Window.at(queryT)).isEmpty();
      final var matchPiece = Lists.reverse(profile.profilePieces).stream()
                                  .filter(containsQueryTimeP).findFirst();
      return matchPiece.map(piece -> valueMapper.deserializeValue(piece.value).getSuccessOrThrow()).orElse(null);
    } else {
      return null;
    }
  }

  public Windows whenValueBetween(T inf, T sup, Windows windows) {
    failIfEmpty();

    //special case doubles are the only aerie types that can be compared with inequality constraints
    if (inf instanceof Double) {
      final var gteConstraint = new GreaterThanOrEqual(new RealResource(this.name), new RealValue((Double) inf));
      final var lteConstraint = new LessThanOrEqual(new RealResource(this.name), new RealValue((Double) sup));
      final var constraint = new And(gteConstraint, lteConstraint);
      final var satisfied = constraint.evaluate(this.simResults);
      return convertToSchedulerWindows(satisfied, windows);
    } else {
      throw new UnsupportedOperationException("aerie inequality constraints only work with real-valued resources");
    }
  }

  public Windows whenValueBelow(T val, Windows windows) {
    failIfEmpty();

    //special case doubles are the only aerie types that can be compared with inequality constraints
    if (val instanceof Double) {
      final var constraint = new LessThan(new RealResource(this.name), new RealValue((Double) val));
      final var satisfied = constraint.evaluate(this.simResults);
      return convertToSchedulerWindows(satisfied, windows);
    } else {
      throw new UnsupportedOperationException("aerie inequality constraints only work with real-valued resources");
    }
  }

  public Windows whenValueAbove(T val, Windows windows) {
    failIfEmpty();

    //special case doubles are the only aerie types that can be compared with inequality constraints
    if (val instanceof Double) {
      final var constraint = new GreaterThan(new RealResource(this.name), new RealValue((Double) val));
      final var satisfied = constraint.evaluate(this.simResults);
      return convertToSchedulerWindows(satisfied, windows);
    } else {
      throw new UnsupportedOperationException("aerie inequality constraints only work with real-valued resources");
    }
  }

  public Windows whenValueEqual(T val, Windows windows) {
    failIfEmpty();

    Expression<Windows> constraint;
    if (val instanceof Double || val instanceof Float) {
      //aeire discrete double resources can be promoted to real resources even if discrete, so just do that for all doubles
      final var dblVal = ((Number) val).doubleValue();
      constraint = new Equal<>(new RealResource(this.name), new RealValue(dblVal));
    } else {
      //everything else is handled as a discrete resource
      //TODO: improve type multiplexing (type vars? or just unify type handling with aerie)
      if (val instanceof Boolean) {
        final var serVal = SerializedValue.of((Boolean) val);
        constraint = new Equal<>(new DiscreteResource(this.name), new DiscreteValue(serVal));
      } else if (val instanceof Long || val instanceof Integer) {
        final var serVal = SerializedValue.of(((Number) val).longValue());
        constraint = new Equal<>(new DiscreteResource(this.name), new DiscreteValue(serVal));
      } else if (val instanceof String) {
        final var serVal = SerializedValue.of((String) val);
        constraint = new Equal<>(new DiscreteResource(this.name), new DiscreteValue(serVal));
      } else {
        throw new UnsupportedOperationException("unrecognized type for aerie discrete resource equality constraint");
      }
    }
    final var satisfied = constraint.evaluate(this.simResults);
    return convertToSchedulerWindows(satisfied, windows);
  }

  @Override
  public Map<Window, T> getTimeline(Windows timeDomain) {
    return values;
  }

  @Override
  public Windows whenValueNotEqual(T val, Windows windows) {

    Expression<Windows> constraint;
    if (val instanceof Double || val instanceof Float) {
      //aeire discrete double resources can be promoted to real resources even if discrete, so just do that for all doubles
      final var dblVal = ((Number) val).doubleValue();
      constraint = new NotEqual<>(new RealResource(this.name), new RealValue(dblVal));
    } else {
      //everything else is handled as a discrete resource
      //TODO: improve type multiplexing (type vars? or just unify type handling with aerie)
      if (val instanceof Boolean) {
        final var serVal = SerializedValue.of((Boolean) val);
        constraint = new NotEqual<>(new DiscreteResource(this.name), new DiscreteValue(serVal));
      } else if (val instanceof Long || val instanceof Integer) {
        final var serVal = SerializedValue.of(((Number) val).longValue());
        constraint = new NotEqual<>(new DiscreteResource(this.name), new DiscreteValue(serVal));
      } else if (val instanceof String) {
        final var serVal = SerializedValue.of((String) val);
        constraint = new NotEqual<>(new DiscreteResource(this.name), new DiscreteValue(serVal));
      } else {
        throw new UnsupportedOperationException("unrecognized type for aerie discrete resource inequality constraint");
      }
    }
    final var satisfied = constraint.evaluate(this.simResults);
    return convertToSchedulerWindows(satisfied, windows);
  }

}
