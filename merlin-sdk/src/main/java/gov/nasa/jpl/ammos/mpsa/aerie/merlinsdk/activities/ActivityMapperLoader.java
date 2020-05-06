package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.JsonUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ActivityMapperLoader {

    public static final String PATH_TO_MAPPERS_IN_JAR = "META-INF/merlin/activityMappers.json";

    /**
     * Reads in the activity type to mapper json file produced by the annotation
     * processor to produce an ActivityMapper for the set of included activities
     * @param jsonStream  Input stream for activity mapper json
     * @param classLoader A ClassLoader to load the activity type and mapper
     *                    classes specified by the jsonStream
     * @return an ActivityMapper for all activities in the input file
     */
    public static ActivityMapper loadActivityMapper(InputStream jsonStream, ClassLoader classLoader) throws ActivityMapperLoadException, ClassNotFoundException {
        // Parse the JSON
        Map<String, String> activityMappings;
        try {
            activityMappings = JsonUtilities.parseStringStringMap(jsonStream);
        } catch (IOException e) {
            throw new ActivityMapperLoadException(e);
        }

        // Create the mapping for activity types to their mappers
        Map<String, ActivityMapper> activityMappers = new HashMap<>();
        for (var entry : activityMappings.entrySet()) {
            String activityTypeName = entry.getKey();
            Class<?> mapperClass = classLoader.loadClass(entry.getValue());

            ActivityMapper activityTypeMapper;
            try {
                activityTypeMapper = (ActivityMapper) mapperClass.getConstructor().newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
                throw new ActivityMapperLoadException(e);
            }

            activityMappers.put(activityTypeName, activityTypeMapper);
        }

        // Return the composite mapper
        return new CompositeActivityMapper(activityMappers);
    }

    public static ActivityMapper loadActivityMapper(Class<? extends MerlinAdaptation> adaptationClass) throws ActivityMapperLoadException {
        try {
            //TODO: Ensure we are getting all mapper files, as there may be more than one
            //      especially if we load multiple JAR files
            InputStream mapperStream = adaptationClass.getResourceAsStream("/" + PATH_TO_MAPPERS_IN_JAR);
            return loadActivityMapper(mapperStream, adaptationClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ActivityMapperLoadException(e);
        }
    }

    public static class ActivityMapperLoadException extends Exception {
        public ActivityMapperLoadException(Throwable e) {
            super(e.getClass().getCanonicalName() + " - " + e.getMessage(), e);
        }
    }
}
