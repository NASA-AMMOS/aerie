package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityEffectTrait implements EffectTrait<List<ActivityEffect>> {

    @Override
    public List<ActivityEffect> empty() {
        return Collections.emptyList();
    }

    @Override
    public List<ActivityEffect> sequentially(List<ActivityEffect> prefix, List<ActivityEffect> suffix) {
        final var sequenced = new ArrayList<>(prefix);
        sequenced.addAll(suffix);

        return sequenced;
    }

    @Override
    public List<ActivityEffect> concurrently(List<ActivityEffect> left, List<ActivityEffect> right) {
        for (var l : left) {
            for (var r : right) {
                if (l.getActivityID().equals(r.getActivityID())) {
                    throw new RuntimeException("Multiple simultaneous events on same activity instance : " + l.getActivityID());
                }
            }
        }

        return sequentially(left, right);
    }
}
