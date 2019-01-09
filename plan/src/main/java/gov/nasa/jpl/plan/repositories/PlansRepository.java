package gov.nasa.jpl.plan.repositories;

import gov.nasa.jpl.plan.models.Plan;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlansRepository extends MongoRepository<Plan, String> {
    Plan findBy_id(ObjectId _id);
}