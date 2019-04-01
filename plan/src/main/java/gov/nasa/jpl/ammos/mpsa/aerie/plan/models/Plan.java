package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("plans")
public class Plan extends gov.nasa.jpl.ammos.mpsa.aerie.schemas.Plan {
  @Id()
  private ObjectId _id;

  public Plan() {
  }

  public Plan(String adaptationId, String endTimestamp, String id, String name,
      String startTimestamp) {
    super(adaptationId, endTimestamp, id, name, startTimestamp);
  }

  public String get_id() {
    return _id.toHexString();
  }

  public void set_id(ObjectId _id) {
    this._id = _id;
  }
}
