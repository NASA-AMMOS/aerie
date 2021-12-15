package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import java.util.Map;

public interface PlanService {
  Plan getPlan(String planId) throws NoSuchPlanException;
  long getPlanRevision(String planId) throws NoSuchPlanException;
  RevisionData getPlanRevisionData(String planId) throws NoSuchPlanException;

  Map<String, Constraint> getConstraintsForPlan(String planId) throws NoSuchPlanException;

  long addExternalDataset(String id, Timestamp datasetStart, ProfileSet profileSet) throws NoSuchPlanException;
}
