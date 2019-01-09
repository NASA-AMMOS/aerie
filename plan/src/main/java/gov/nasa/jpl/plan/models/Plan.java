package gov.nasa.jpl.plan.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;

@Document("plans")
public class Plan {
    @Id
    private ObjectId _id;

    private String name;
    private String end;
    private String start;
    private String adaptationId;
    private ArrayList<ActivityInstance> activityInstances =
            new ArrayList<ActivityInstance>();

    public Plan() {
    }

    public Plan(ObjectId _id, String name, String end, String start,
            String adaptationId,
            ArrayList<ActivityInstance> activityInstances) {
        this.set_id(_id);
        this.setName(name);
        this.setEnd(end);
        this.setStart(start);
        this.setAdaptationId(adaptationId);
        this.setActivityInstances(activityInstances);
    }

    // ObjectId needs to be converted to string
    public String get_id() {
        return _id.toHexString();
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getAdaptationId() {
        return adaptationId;
    }

    public void setAdaptationId(String adaptationId) {
        this.adaptationId = adaptationId;
    }

    public ArrayList<ActivityInstance> getActivityInstances() {
        return activityInstances;
    }

    public void setActivityInstances(ArrayList<ActivityInstance> activityInstances) {
        this.activityInstances = activityInstances;
    }

    public void addActivityInstance(ActivityInstance activityInstance) {
        this.activityInstances.add(activityInstance);
    }

    public void updateActivityInstance(UUID id, ActivityInstance activityInstanceToMerge) {
        ActivityInstance activityInstance = this.getActivityInstance(id);
        if (activityInstance != null) {
            this.mergeActivityInstances(activityInstance, activityInstanceToMerge);
            int index = this.getActivityInstanceIndex(id);
            if (index > -1) {
                this.activityInstances.set(index, activityInstance);
                return;
            }
        }
        throw new NoSuchElementException();
    }

    public void removeActivityInstance(UUID id) {
        int index = this.getActivityInstanceIndex(id);
        if (index > -1) {
            this.activityInstances.remove(index);
        }
        // TODO: Error handling?
    }

    public ActivityInstance getActivityInstance(UUID id) {
        for (ActivityInstance ai : this.getActivityInstances()) {
            if (ai.getId().equals(id.toString())) {
                return ai;
            }
        }
        return null;
    }

    /**
     * Merge two activity instances
     *
     * @param a Activity instance that will be overwritten
     * @param b Activity instance that will overwrite a
     * @return Activity instance a
     */
    public void mergeActivityInstances(ActivityInstance a, ActivityInstance b) {
        if (b.name != null) {
            a.setName(b.name);
        }

        if (b.listeners != null) {
            a.setListeners(b.listeners);
        }

        if (b.parameters != null) {
            a.setParameters(b.parameters);
        }
    }

    private int getActivityInstanceIndex(UUID id) {
        int i = 0;
        for (ActivityInstance ai : this.getActivityInstances()) {
            if (ai.getId().equals(id.toString())) {
                return i;
            }
            i++;
        }
        return -1;
    }

}

