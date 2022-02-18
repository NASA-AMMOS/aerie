package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Collection;

/**
 * an in-memory solution to a planning problem including a schedule of activities
 *
 * may only be a partial solution to the whole planning problem, ie some
 * goals may be left unsatisfied
 */
public class PlanInMemory implements Plan {

  /**
   * ctor creates a new empty solution plan
   *
   */
  public PlanInMemory() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(Collection<ActivityInstance> acts) {
    for (final var act : acts) {
      add(act);
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void add(ActivityInstance act) {
    if (act == null) {
      throw new IllegalArgumentException(
          "adding null activity to plan");
    }
    final var startT = act.getStartTime();
    if (startT == null) {
      throw new IllegalArgumentException(
          "adding activity with null start time to plan");
    }
    final var id = act.getId();
    assert id != null;
    if (actsById.containsKey(id)) {
      throw new IllegalArgumentException(
          "adding activity with duplicate name=" + id + " to plan");
    }
    final var type = act.getType().getName();
    assert type != null;

    actsById.put(id, act);
    //REVIEW: use a cleaner multimap? maybe guava
    actsByTime.computeIfAbsent(startT, k -> new java.util.LinkedList<>())
              .add(act);
    actsByType.computeIfAbsent(type, k -> new java.util.LinkedList<>())
              .add(act);
    actsSet.add(act);
  }

  @Override
  public void remove(Collection<ActivityInstance> acts) {
    for (var act : acts) {
      remove(act);
    }
  }

  @Override
  public void remove(ActivityInstance act) {
    //TODO: handle ownership. Constraint propagation ?
    actsById.remove(act.getId());
    var acts = actsByTime.get(act.getStartTime());
    if (acts != null) acts.remove(act);
    acts = actsByType.get(act.getType().getName());
    if (acts != null) acts.remove(act);
    actsSet.remove(act);
  }

  @Override
  public void removeAllWindows() {
    var acts = actsByType.get("Window");
    for (var act : acts) {
      remove(act);
    }

  }


  /**
   * {@inheritDoc}
   */
  @Override
  public java.util.List<ActivityInstance> getActivitiesByTime() {
    //REVIEW: could probably do something tricky with streams to avoid new
    final var orderedActs = new java.util.LinkedList<ActivityInstance>();

    //NB: tree map ensures that values are in key order, but still need to flatten
    for (final var actsAtT : actsByTime.values()) {
      assert actsAtT != null;
      orderedActs.addAll(actsAtT);
    }

    return java.util.Collections.unmodifiableList(orderedActs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public java.util.Map<String, java.util.List<ActivityInstance>> getActivitiesByType() {
    return java.util.Collections.unmodifiableMap(actsByType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public java.util.Set<ActivityInstance> getActivities() {
    return java.util.Collections.unmodifiableSet(actsSet);
  }

  /**
   * container of all activity instances in plan, indexed by name
   */
  private final java.util.HashMap<SchedulingActivityInstanceId, ActivityInstance> actsById
      = new java.util.HashMap<>();

  /**
   * container of all activity instances in plan, indexed by type
   */
  private final java.util.HashMap<String, java.util.List<ActivityInstance>> actsByType
      = new java.util.HashMap<>();

  /**
   * container of all activity instances in plan, indexed by start time
   */
  private final java.util.TreeMap<Duration, java.util.List<ActivityInstance>> actsByTime
      = new java.util.TreeMap<>();

  /**
   * container of all activity instances in plan
   */
  private final java.util.HashSet<ActivityInstance> actsSet
      = new java.util.HashSet<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<ActivityInstance> find(
      ActivityExpression template)
  {
    //REVIEW: could do something clever with returning streams to prevent wasted work
    //REVIEW: something more clever for time-based queries using time index
    java.util.LinkedList<ActivityInstance> matched = new java.util.LinkedList<>();
    for (final var actsAtTime : actsByTime.values()) {
      for (final var act : actsAtTime) {
        if (template.matches(act)) {
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
    evals.add(eval);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Evaluation> getEvaluations() {
    return java.util.Collections.unmodifiableCollection(evals);
  }

  /**
   * the set of all evaluations posted to the plan
   *
   * note that different solvers may evaluate the same plan differently
   */
  protected final java.util.List<Evaluation> evals = new java.util.LinkedList<>();


}
