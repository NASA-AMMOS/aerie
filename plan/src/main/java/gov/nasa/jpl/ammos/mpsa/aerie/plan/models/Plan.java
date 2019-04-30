package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Document("plans")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Plan extends gov.nasa.jpl.ammos.mpsa.aerie.schemas.Plan {
    @Id()
    public String id;

    public Plan() {
        super();
    }

    public Plan(String adaptationId, String endTimestamp, String id, String name, String startTimestamp) {
        super(adaptationId, endTimestamp, id, name, startTimestamp);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
}
