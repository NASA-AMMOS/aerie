package gov.nasa.jpl.aerie.scheduler.solver;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ConflictSolverResult {
  private ConflictSatisfaction satisfaction;
  private Set<SchedulingActivity> activitiesCreated;
  private Set<TaskNetTemplateData> decompositionsCreated;

  public ConflictSolverResult(){
    this.satisfaction = ConflictSatisfaction.NOT_SAT;
    this.activitiesCreated = new HashSet<>();
    this.decompositionsCreated = new HashSet<>();
  }

  public ConflictSolverResult(final ConflictSatisfaction conflictSatisfaction){
    this.satisfaction = conflictSatisfaction;
    this.activitiesCreated = new HashSet<>();
    this.decompositionsCreated = new HashSet<>();
  }

  public ConflictSolverResult(
      final ConflictSatisfaction conflictSatisfaction,
      final List<SchedulingActivity> activitiesSatisfying,
      final List<TaskNetTemplateData> decompositionsSatisfying) {
    this.satisfaction = conflictSatisfaction;
    this.activitiesCreated = new HashSet<>(activitiesSatisfying);
    this.decompositionsCreated = new HashSet<>(decompositionsSatisfying);
  }


  public void setSatisfaction(final ConflictSatisfaction satisfaction){
    this.satisfaction = satisfaction;
  }

  public ConflictSatisfaction satisfaction(){
    return this.satisfaction;
  }

  public Set<SchedulingActivity> activitiesCreated(){
    return this.activitiesCreated;
  }

  public Set<TaskNetTemplateData> decompositionsCreated() {
    return decompositionsCreated;
  }
}

