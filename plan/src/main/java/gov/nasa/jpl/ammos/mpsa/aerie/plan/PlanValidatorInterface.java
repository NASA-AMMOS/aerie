package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.PlanDetail;

public interface PlanValidatorInterface {
    void validateActivitiesForPlan(PlanDetail plan) throws ValidationException;

    class ValidationException extends Exception {
        private String reason;

        public ValidationException(String reason) {
            super(reason);
            this.reason = reason;
        }
    }
}
