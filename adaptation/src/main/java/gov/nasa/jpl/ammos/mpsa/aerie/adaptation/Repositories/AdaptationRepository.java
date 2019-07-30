package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.Repositories;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationProjection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AdaptationRepository extends MongoRepository<Adaptation, String> {
    List<AdaptationProjection> findAllProjectedBy();
}
