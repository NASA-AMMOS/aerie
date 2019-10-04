package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk.AdaptationUtils;
import gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk.MissingAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ParameterSchema;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AdaptationLoader {
    public static Map<String, ActivityType> loadActivities(final Path path) throws MissingAdaptationException {
        final MerlinAdaptation adaptation = AdaptationUtils.loadAdaptation(path);

        final Map<String, ParameterSchema> activitySchemas = Optional
            .of(adaptation)
            .map(MerlinAdaptation::getActivityMapper)
            .map(ActivityMapper::getActivitySchemas)
            .orElseGet(HashMap::new);

        return activitySchemas
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                p -> p.getKey(),
                p -> new ActivityType(p.getKey(), p.getValue())));
    }
}
