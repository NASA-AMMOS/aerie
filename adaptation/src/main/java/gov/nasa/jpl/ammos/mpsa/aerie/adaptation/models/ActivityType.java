package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityTypeParameter;
import org.springframework.data.annotation.Id;

import java.util.List;

public class ActivityType extends gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType {

    public ActivityType(String id, String name, List<ActivityTypeParameter> parameters) {
        super(id, name, parameters);
    }

    @Id
    @Override
    public String getId() {
        return super.getId();
    }
}
