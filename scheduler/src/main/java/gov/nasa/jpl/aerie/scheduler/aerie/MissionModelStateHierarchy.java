package gov.nasa.jpl.aerie.scheduler.aerie;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gov.nasa.jpl.aerie.scheduler.Duration;
import gov.nasa.jpl.aerie.scheduler.aerie.SimulateQuery;
import gov.nasa.jpl.aerie.scheduler.Time;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MissionModelStateHierarchy {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Duration.class, new DurationTypeAdapter())
            .create();

    public void updateFromSimulation(List<SimulateQuery.Result> simResults) throws RuntimeException {
        final Class<?> rootClass = this.getClass();
        for (final SimulateQuery.Result result : simResults) {
            final Time startTime = Time.fromString(result.start());
            final String[] path = result.name().substring(1).split("/");
            Class<?> currClass = rootClass;
            Object currObj = this;
            try {
                for (final String name : path) {
                    final Field field = currClass.getField(name);
                    currClass = field.getType();
                    Object childObj = field.get(currObj);
                    if (childObj == null) {
                        childObj = currClass.getConstructor().newInstance();
                        field.set(currObj, childObj);
                    }
                    currObj = childObj;
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(String.format("Error retrieving state %s", result.name()));
            }
            if (currObj instanceof AerieState) {
                final AerieState<?> aerieState = (AerieState<?>) currObj;
                final Type type = aerieState.getType();
                final Map<Time, Object> map = new TreeMap<>();
                for (final SimulateQuery.Value value : result.values()) {
                    final Time time = startTime.plus(Duration.ofMicroseconds((long) value.x()));
                    Object val = gson.fromJson(gson.toJson(value.y()), type);
                    map.put(time, val);
                }
                aerieState.updateFromSimulation(map);
            }
        }
    }
}
