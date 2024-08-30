package gov.nasa.jpl.aerie.scheduler.solver;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ConflictSolverResult {
  private ConflictSatisfaction satisfaction;
  private Set<SchedulingActivity> activitiesCreated;
  private List<TaskNetTemplateData> decompositionsCreated;
  //private Deque<Conflict> newConflicts;

  public ConflictSolverResult(){
    this.satisfaction = ConflictSatisfaction.SAT;
    this.activitiesCreated = new HashSet<>();
    this.decompositionsCreated = new ArrayList<>();
    //this.newConflicts = new ArrayDeque<>();
  }

  public ConflictSolverResult(final ConflictSatisfaction conflictSatisfaction){
    this.satisfaction = conflictSatisfaction;
    this.activitiesCreated = new HashSet<>();
    this.decompositionsCreated = new ArrayList<>();
    //this.newConflicts = new ArrayDeque<>();
  }

  public ConflictSolverResult(
      final ConflictSatisfaction conflictSatisfaction,
      final List<SchedulingActivity> activitiesSatisfying,
      final List<TaskNetTemplateData> decompositionsSatisfying) {
    this.satisfaction = conflictSatisfaction;
    this.activitiesCreated = new HashSet<>(activitiesSatisfying);
    this.decompositionsCreated = new ArrayList<>(decompositionsSatisfying);
    //this.newConflicts = new ArrayDeque<>();
  }

  public ConflictSolverResult(
      final ConflictSatisfaction conflictSatisfaction,
      final List<SchedulingActivity> activitiesSatisfying,
      final ArrayList<TaskNetTemplateData> decompositionsSatisfying,
      final Collection<Conflict> conflicts) {
    this.satisfaction = conflictSatisfaction;
    this.activitiesCreated = new HashSet<>(activitiesSatisfying);
    this.decompositionsCreated = new ArrayList<>(decompositionsSatisfying);
    //this.newConflicts = new ArrayDeque<>();
  }

  public void mergeConflictSolverResult(ConflictSolverResult other){
    this.satisfaction = this.satisfaction.ordinal() <= other.satisfaction.ordinal() ? this.satisfaction : other.satisfaction;
    this.activitiesCreated.addAll(other.activitiesCreated());
    this.decompositionsCreated.addAll(other.decompositionsCreated());
    //this.newConflicts.addAll(other.getNewConflicts());
  }


  public void setSatisfaction(final ConflictSatisfaction satisfaction){
    this.satisfaction = satisfaction;
  }
  /*
  public void setNewConflicts(final Deque<Conflict> newConflicts) {
    this.newConflicts = newConflicts;
  }*/

  public ConflictSatisfaction satisfaction(){
    return this.satisfaction;
  }

  public Set<SchedulingActivity> activitiesCreated(){
    return this.activitiesCreated;
  }

  public List<TaskNetTemplateData> decompositionsCreated() {
    return decompositionsCreated;
  }

  /*
  public Collection<Conflict> getNewConflicts() {
    return newConflicts;
  }*/
}

