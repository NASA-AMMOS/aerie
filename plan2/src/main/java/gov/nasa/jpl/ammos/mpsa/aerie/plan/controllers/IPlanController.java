package gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public interface IPlanController {
  Stream<Pair<String, Plan>> getPlans();
  Plan getPlanById(String id) throws NoSuchPlanException;
  String addPlan(NewPlan plan) throws ValidationException;
  void removePlan(String id) throws NoSuchPlanException;
  void updatePlan(String id, Plan patch) throws ValidationException, NoSuchPlanException;
  void replacePlan(String id, NewPlan plan) throws ValidationException, NoSuchPlanException;

  class ValidationException extends Exception {
    private final List<String> errors;

    public ValidationException(final String message, final List<String> errors) {
      super(message + ": " + errors.toString());
      this.errors = Collections.unmodifiableList(errors);
    }

    public List<String> getValidationErrors() {
      return this.errors;
    }
  }

  class NoSuchPlanException extends Exception {
    private final String id;

    public NoSuchPlanException(final String id) {
      super("No plan exists with id `" + id + "`");
      this.id = id;
    }

    public String getInvalidPlanId() {
      return this.id;
    }
  }
}
