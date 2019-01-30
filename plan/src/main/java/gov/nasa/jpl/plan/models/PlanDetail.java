package gov.nasa.jpl.plan.models;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Document("plans")
public class PlanDetail extends Plan {
    private ArrayList<ActivityInstance> activityInstances = new ArrayList<ActivityInstance>();

    public PlanDetail() {
    }

    public PlanDetail(ObjectId _id, String adaptationId, String endTimestamp, String name, String startTimestamp,
            ArrayList<ActivityInstance> activityInstances) {
        super(_id, adaptationId, endTimestamp, name, startTimestamp);
        this.setActivityInstances(activityInstances);
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
            mergeObjects(activityInstanceToMerge, activityInstance);
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
            if (ai.getActivityId().equals(id.toString())) {
                return ai;
            }
        }
        return null;
    }

    private int getActivityInstanceIndex(UUID id) {
        int i = 0;
        for (ActivityInstance ai : this.getActivityInstances()) {
            if (ai.getActivityId().equals(id.toString())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * This takes two Objects and merges them into the target Object while ignoring
     * null property values.
     * 
     * TODO: Move this to a util package.
     * 
     * @see https://bit.ly/2Wujn6G
     * @param source
     * @param target
     */
    public static void mergeObjects(Object source, Object target) {
        String[] ignoreProperties = getNullPropertyNames(source);
        BeanUtils.copyProperties(source, target, ignoreProperties);
    }

    /**
     * This returns a list of non-null properties in a Java Object.
     * This exists because we need a list of proerties to ignore when using
     * the BeanUtils `copyProperties` for merging two Objects.
     * 
     * TODO: Move this to a util package.
     * 
     * @see https://stackoverflow.com/a/19739041
     * @param source
     * @return
     */
    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null)
                emptyNames.add(pd.getName());
        }

        String[] result = new String[emptyNames.size()];

        return emptyNames.toArray(result);
    }
}
