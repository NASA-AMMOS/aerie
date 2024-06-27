package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplate;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.solver.Evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
   * container of all activity instances in plan, indexed by start time
   */
  private final TreeMap<Duration, List<SchedulingActivity>> actsByTime;
  private List<TaskNetTemplateData> decompositions;

  /**
   * ctor creates a new empty solution plan
   *
   */
  public PlanInMemory() {
    this.actsByTime = new TreeMap<>();
    this.decompositions = new ArrayList<>();
  }

  public PlanInMemory(final PlanInMemory other){
    if(other.evaluation != null) this.evaluation = other.evaluation.duplicate();
    this.actsByTime = new TreeMap<>();
    for(final var entry: other.actsByTime.entrySet()){
      this.actsByTime.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    this.decompositions = new ArrayList<>();
    Collections.copy(this.decompositions,other.decompositions);
  }

  @Override
  public PlanInMemory duplicate() {
    return new PlanInMemory(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(Collection<SchedulingActivity> acts) {
    for (final var act : acts) {
      add(act);
    }
  }

  public int size(){
    int size = 0;
    for(final var entry: this.actsByTime.entrySet()){
      size += entry.getValue().size();
    }
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(final SchedulingActivity act) {
    if (act == null) {
      throw new IllegalArgumentException(
          "adding null activity to plan");
    }
    final var startT = act.startOffset();
    if (startT == null) {
      throw new IllegalArgumentException(
          "adding activity with null start time to plan");
    }
    final var id = act.getId();
    assert id != null;
    actsByTime.computeIfAbsent(startT, k -> new LinkedList<>())
              .add(act);
  }

  @Override
  public void addTaskNetTemplateData(final TaskNetTemplateData tn){
    if (tn == null) {
      throw new IllegalArgumentException(
          "adding null tasknet to plan");
    }
    if (tn.subtasks() == null) {
      throw new IllegalArgumentException(
          "adding template with null list of substasks");
    }
    this.decompositions.add(tn);
    //TODO need to add code in scheduler to instantiate activities
  }

  @Override
  public void remove(Collection<SchedulingActivity> acts) {
    for (var act : acts) {
      remove(act);
    }
  }

  @Override
  public void remove(SchedulingActivity act) {
    var acts = actsByTime.get(act.startOffset());
    if (acts != null) acts.remove(act);
  }

  @Override
  public void removeTaskNetTemplate(final TaskNetTemplate tn){
    this.decompositions.remove(tn);
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public List<SchedulingActivity> getActivitiesByTime() {
    //REVIEW: could probably do something tricky with streams to avoid new
    final var orderedActs = new LinkedList<SchedulingActivity>();

    //NB: tree map ensures that values are in key order, but still need to flatten
    for (final var actsAtT : actsByTime.values()) {
      assert actsAtT != null;
      orderedActs.addAll(actsAtT);
    }

    return Collections.unmodifiableList(orderedActs);
  }

  public void replaceActivity(SchedulingActivity oldAct, SchedulingActivity newAct){
    this.remove(oldAct);
    this.add(newAct);
    if(evaluation != null) this.evaluation.updateGoalEvals(oldAct, newAct);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<ActivityType, List<SchedulingActivity>> getActivitiesByType() {
    final var map = new HashMap<ActivityType, List<SchedulingActivity>>();
    for(final var entry: this.actsByTime.entrySet()){
      for(final var activity : entry.getValue()){
        map.computeIfAbsent(activity.type(), t -> new ArrayList<>()).add(activity);
      }
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public Map<SchedulingActivityDirectiveId, SchedulingActivity> getActivitiesById() {
    final var map = new HashMap<SchedulingActivityDirectiveId, SchedulingActivity>();
    for(final var entry: this.actsByTime.entrySet()){
      for(final var activity : entry.getValue()){
        map.put(activity.id(), activity);
      }
    }
    return Collections.unmodifiableMap(map);
  }

@Override
  public Set<SchedulingActivityDirectiveId> getAnchorIds() {
    return getActivities().stream()
                  .map(SchedulingActivity::anchorId)
                  .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<SchedulingActivity> getActivities() {
    final var set = new HashSet<SchedulingActivity>();
    for(final var entry: this.actsByTime.entrySet()){
      set.addAll(entry.getValue());
    }
    return Collections.unmodifiableSet(set);
  }

  public List<TaskNetTemplateData> getDecompositions() {
    return decompositions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<SchedulingActivity> find(
      ActivityExpression template, SimulationResults simulationResults,
      EvaluationEnvironment evaluationEnvironment)
  {
    //REVIEW: could do something clever with returning streams to prevent wasted work
    //REVIEW: something more clever for time-based queries using time index
    LinkedList<SchedulingActivity> matched = new LinkedList<>();
    for (final var actsAtTime : actsByTime.values()) {
      for (final var act : actsAtTime) {
        if (template.matches(act, simulationResults, evaluationEnvironment, true, this)) {
          matched.add(act);
        }
      }
    }
    return matched;
  }

  /**
   * adds a new evaluation to the plan
   *
   * note that different solvers or metrics will have different evaluations
   * for the same plan
   *
   * @param eval IN the new evaluation to add to the plan
   */

  public void addEvaluation(Evaluation eval) {
    evaluation = eval;
  }

  /**
   * fetches evaluation posted to the plan
   *
   * @return evaluation posted to the plan
   */
  public Evaluation getEvaluation() {
    return evaluation;
  }

  @Override
  public Duration calculateAbsoluteStartOffsetAnchoredActivity(SchedulingActivity act){
    if(act == null)
      return null;
    if(act.anchorId() != null){
      SchedulingActivity parent = this.getActivitiesById().get(act.anchorId());
      if(!act.anchoredToStart() && parent.duration() == null)
        throw new IllegalArgumentException("Cannot calculate the absolute duration for an activity that is not anchored to the start while the parent doesn't have duration");
      return calculateAbsoluteStartOffsetAnchoredActivity(parent).plus(act.anchoredToStart() ? act.startOffset() : act.startOffset().plus(parent.duration()));
    }
    return act.startOffset();
  }
}
