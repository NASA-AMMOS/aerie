package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class QueryUtilityMethods {

    public static <T extends Number> List<Window> stateThreshold(Set<Map.Entry<Duration, Double>> entrySet, Instant endTime,
                                                                 Predicate<Double> lambda){

        final var iter = entrySet.iterator();
        List<Window> windows = new ArrayList<>();

        while (iter.hasNext()){
            Instant start = null;
            while (iter.hasNext()){
                final var point = iter.next();
                if (lambda.test(point.getValue())) {
                    start = SimulationInstant.ORIGIN.plus(point.getKey());
                    break;
                }
            }
            if (start == null) break;

            Instant end = null;
            while (iter.hasNext()){
                final var point = iter.next();
                if (!lambda.test(point.getValue())){
                    end = SimulationInstant.ORIGIN.plus(point.getKey());
                    break;
                }
            }
            if (end == null) end = endTime;

            windows.add(Window.between(start, end));
        }
        return windows;
    }
}