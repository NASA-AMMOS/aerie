package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.ValidationException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface PlanService {
  Stream<Pair<String, Plan>> getPlans();
  Plan getPlanById(String id) throws NoSuchPlanException;
  String addPlan(NewPlan plan) throws ValidationException;
  void removePlan(String id) throws NoSuchPlanException;
  void updatePlan(String id, Plan patch) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException;
  void replacePlan(String id, NewPlan plan) throws ValidationException, NoSuchPlanException;
  ActivityInstance getActivityInstanceById(String planId, String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException;
  List<String> addActivityInstancesToPlan(String planId, List<ActivityInstance> activityInstances) throws ValidationException, NoSuchPlanException;
  void removeActivityInstanceById(String planId, String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException;
  void updateActivityInstance(String planId, String activityInstanceId, ActivityInstance patch) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException;
  void replaceActivityInstance(String planId, String activityInstanceId, ActivityInstance activityInstance) throws NoSuchPlanException, ValidationException, NoSuchActivityInstanceException;

  Pair<SimulationResults, Map<String, List<Violation>>> getSimulationResultsForPlan(String planId)
  throws NoSuchPlanException;
}
