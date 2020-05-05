package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.GeneralSearch;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GeneralSearchTest {

    @Test
    public void bisectBy(){

        List<Window> windowsA = new ArrayList<>();
        List<Window> windowsB = new ArrayList<>();

        Instant t0 = SimulationInstant.ORIGIN;
        Instant t1 = SimulationInstant.ORIGIN;
        Instant t2 = t1.plus(10, TimeUnit.SECONDS);
        Duration duration = Duration.of(20,TimeUnit.SECONDS);

        // [0,10] , [20,30] , [40, 50] , ... [200,210]
        for (int i = 0; i < 10; i++){
            Window window = Window.between(t1,t2);
            windowsA.add(window);

            t1 = t1.plus(duration);
            t2 = t2.plus(duration);
        }

        {
            final var pivot = t1;
            final var indexBeforePivot = GeneralSearch.bisectBy(windowsA, window -> window.end.isBefore(pivot));
            assertEquals(indexBeforePivot, 10);
        }

        {
            final var pivot = t0.plus(45, TimeUnit.SECONDS);
            final var indexBeforePivot = GeneralSearch.bisectBy(windowsA, window -> window.end.isBefore(pivot));
            assertEquals(indexBeforePivot, 2);
        }

        {
            final var pivot = t0.plus(91, TimeUnit.SECONDS);
            final var indexBeforePivot = GeneralSearch.bisectBy(windowsA, window -> window.end.isBefore(pivot));
            assertEquals(indexBeforePivot, 5);
        }

        {
            final var pivot = t0.plus(90, TimeUnit.SECONDS);
            final var indexBeforePivot = GeneralSearch.bisectBy(windowsA, window -> window.end.isBefore(pivot));
            assertEquals(indexBeforePivot, 4);
        }
    }
}
