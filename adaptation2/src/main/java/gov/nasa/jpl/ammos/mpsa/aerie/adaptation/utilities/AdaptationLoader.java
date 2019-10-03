package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationAccessException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.InvalidAdaptationJARException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk.AdaptationUtils;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ParameterSchema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AdaptationLoader {
    public static Map<String, ActivityType> loadActivities(final Path path) throws InvalidAdaptationJARException {
        if (path == null) {
            System.err.println("Adaptation load requested, but no path specified");
            return null;
        }

        final MerlinAdaptation userAdaptation;
        try {
            userAdaptation = Optional
                    .ofNullable(AdaptationUtils.loadAdaptation(path))
                    .orElseThrow(() -> new InvalidAdaptationJARException(path));
        } catch (final IOException e) {
            throw new AdaptationAccessException(path, e);
        }

        final ActivityMapper activityMapper = userAdaptation.getActivityMapper();
        final Map<String, ParameterSchema> activitySchemas = activityMapper.getActivitySchemas();

        return activitySchemas
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getKey(),
                        p -> new ActivityType(p.getKey(), p.getValue())));
    }
}
