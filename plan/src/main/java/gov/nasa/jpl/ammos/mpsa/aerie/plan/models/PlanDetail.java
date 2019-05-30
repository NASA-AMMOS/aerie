package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("plans")
public class PlanDetail extends gov.nasa.jpl.ammos.mpsa.aerie.schemas.PlanDetail {
    public PlanDetail() {
        super();
    }

    public PlanDetail(
            String adaptationId,
            String endTimestamp,
            String id,
            String name,
            String startTimestamp,
            ArrayList<ActivityInstance> activityInstances
    ) {
        super(adaptationId, endTimestamp, id, name, startTimestamp, activityInstances);
    }

    @Id
    @Override
    public String getId() {
        return super.getId();
    }

    /**
     * This takes two Objects and merges them into the target Object while ignoring null property
     * values.
     * <p>
     * TODO: Move this to a util package.
     *
     * @see "https://bit.ly/2Wujn6G"
     */
    public static void mergeObjects(Object source, Object target) {
        String[] ignoreProperties = getNullPropertyNames(source);
        BeanUtils.copyProperties(source, target, ignoreProperties);
    }

    /**
     * This returns a list of non-null properties in a Java Object. This exists because we need a list
     * of proerties to ignore when using the BeanUtils `copyProperties` for merging two Objects.
     * <p>
     * TODO: Move this to a util package.
     *
     * @see "https://stackoverflow.com/a/19739041"
     */
    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }

        String[] result = new String[emptyNames.size()];

        return emptyNames.toArray(result);
    }

    public void addActivityInstance(ActivityInstance activityInstance) {
        getActivityInstances().add(activityInstance);
    }

    public void replaceActivityInstance(UUID id, ActivityInstance replacement) {
        int index = this.getActivityInstanceIndex(id);

        if (index == -1) {
            throw new NoSuchElementException();
        }

        getActivityInstances().set(index, replacement);
    }

    public void updateActivityInstance(UUID id, ActivityInstance activityInstanceToMerge) {
        int index = this.getActivityInstanceIndex(id);

        if (index != -1) {
            ActivityInstance activityInstance = getActivityInstances().get(index);
            mergeObjects(activityInstanceToMerge, activityInstance);

            getActivityInstances().set(index, activityInstance);
            return;
        }

        throw new NoSuchElementException();
    }

    public void removeActivityInstance(UUID id) {
        int index = this.getActivityInstanceIndex(id);
        if (index > -1) {
            getActivityInstances().remove(index);
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
}
