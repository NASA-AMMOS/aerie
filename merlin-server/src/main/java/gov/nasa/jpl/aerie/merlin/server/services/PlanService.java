package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;

import java.util.Map;

public interface PlanService {
  Plan getPlanById(String id) throws NoSuchPlanException;
  long getPlanRevisionById(String id) throws NoSuchPlanException;


  Map<String, Constraint> getConstraintsForPlan(String planId)
  throws NoSuchPlanException;
}
