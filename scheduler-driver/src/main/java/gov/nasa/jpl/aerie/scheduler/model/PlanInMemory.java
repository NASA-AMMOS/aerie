package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.solver.Evaluation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * an in-memory solution to a planning problem including a schedule of activities
 *
 * may only be a partial solution to the whole planning problem, ie some
 * goals may be left unsatisfied
 */
public class PlanInMemory implements Plan {

  /**
   * the set of all evaluations posted to the plan
   *
   * note that different solvers may evaluate the same plan differently
   */
  protected Evaluation evaluation;

  /**
   * container of all activity instances in plan, indexed by name
   */
  private final HashMap<SchedulingActivityDirectiveId, SchedulingActivityDirective> actsById =
      new HashMap<>();

  /**
   * container of all activity instances in plan, indexed by type
   */
  private final HashMap<ActivityType, List<SchedulingActivityDirective>> actsByType =
      new HashMap<>();

  /**
   * container of all activity instances in plan, indexed by start time
   */
  private final TreeMap<Duration, List<SchedulingActivityDirective>> actsByTime = new TreeMap<>();

  /**
   * container of all activity instances in plan
   */
  private final HashSet<SchedulingActivityDirective> actsSet = new HashSet<>();

  /**
   * ctor creates a new empty solution plan
   *
   */
  public PlanInMemory() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(Collection<SchedulingActivityDirective> acts) {
    for (final var act : acts) {
      add(act);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(SchedulingActivityDirective act) {
    if (act == null) {
      throw new IllegalArgumentException("adding null activity to plan");
    }
    final var startT = act.startOffset();
    if (startT == null) {
      throw new IllegalArgumentException("adding activity with null start time to plan");
    }
    final var id = act.getId();
    assert id != null;
    if (actsById.containsKey(id)) {
      throw new IllegalArgumentException("adding activity with duplicate name=" + id + " to plan");
    }
    final var type = act.getType();
    assert type != null;

    actsById.put(id, act);
    // REVIEW: use a cleaner multimap? maybe guava
    actsByTime.computeIfAbsent(startT, k -> new LinkedList<>()).add(act);
    actsByType.computeIfAbsent(type, k -> new LinkedList<>()).add(act);
    actsSet.add(act);
  }

  @Override
  public void remove(Collection<SchedulingActivityDirective> acts) {
    for (var act : acts) {
      remove(act);
    }
  }

  @Override
  public void remove(SchedulingActivityDirective act) {
    // TODO: handle ownership. Constraint propagation ?
    actsById.remove(act.getId());
    var acts = actsByTime.get(act.startOffset());
    if (acts != null) acts.remove(act);
    acts = actsByType.get(act.getType());
    if (acts != null) acts.remove(act);
    actsSet.remove(act);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SchedulingActivityDirective> getActivitiesByTime() {
    // REVIEW: could probably do something tricky with streams to avoid new
    final var orderedActs = new LinkedList<SchedulingActivityDirective>();

    // NB: tree map ensures that values are in key order, but still need to flatten
    for (final var actsAtT : actsByTime.values()) {
      assert actsAtT != null;
      orderedActs.addAll(actsAtT);
    }

    return Collections.unmodifiableList(orderedActs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<ActivityType, List<SchedulingActivityDirective>> getActivitiesByType() {
    return Collections.unmodifiableMap(actsByType);
  }

  @Override
  public Map<SchedulingActivityDirectiveId, SchedulingActivityDirective> getActivitiesById() {
    return Collections.unmodifiableMap(actsById);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<SchedulingActivityDirective> getActivities() {
    return Collections.unmodifiableSet(actsSet);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<SchedulingActivityDirective> find(
      ActivityExpression template,
      SimulationResults simulationResults,
      EvaluationEnvironment evaluationEnvironment) {
    // REVIEW: could do something clever with returning streams to prevent wasted work
    // REVIEW: something more clever for time-based queries using time index
    LinkedList<SchedulingActivityDirective> matched = new LinkedList<>();
    for (final var actsAtTime : actsByTime.values()) {
      for (final var act : actsAtTime) {
        if (template.matches(act, simulationResults, evaluationEnvironment)) {
          matched.add(act);
        }
      }
    }
    return matched;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addEvaluation(Evaluation eval) {
    evaluation = eval;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Evaluation getEvaluation() {
    return evaluation;
  }
}
