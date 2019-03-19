package gov.nasa.jpl.plan.repositories;

import gov.nasa.jpl.aerie.schemas.Plan;
import gov.nasa.jpl.plan.models.PlanDetail;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlansRepository extends MongoRepository<Plan, String> {
    Plan findPlanByid(String id);
    PlanDetail findPlanDetailByid(String id);
}
