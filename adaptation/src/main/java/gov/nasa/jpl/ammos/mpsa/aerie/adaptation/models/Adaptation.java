package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("adaptations")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Adaptation extends gov.nasa.jpl.ammos.mpsa.aerie.schemas.Adaptation {

    private List<ActivityType> activityTypes;

    public Adaptation() {
        super();
    }

    public Adaptation(String id, String name, String version, String owner, String mission, String location) {
        super(id, location, name, mission, owner, version);
        this.activityTypes = null;
    }

    public Adaptation(String id, String name, String version, String owner, String mission, String location, List<ActivityType> activityTypes) {
        super(id, location, name, mission, owner, version);
        this.activityTypes = activityTypes;
    }

    public boolean equals(Adaptation adaptation) {
        return super.getName().equals(adaptation.getName()) &&
                super.getVersion().equals(adaptation.getVersion());
    }

    @Id
    @Override
    public String getId() {
        return super.getId();
    }

    @JsonIgnore
    @Override
    public String getLocation() {
        return super.getLocation();
    }

    public List<ActivityType> getActivityTypes() {
        return this.activityTypes;
    }

    public void setActivityTypes(List<ActivityType> activityTypes) {
        this.activityTypes = activityTypes;
    }
}