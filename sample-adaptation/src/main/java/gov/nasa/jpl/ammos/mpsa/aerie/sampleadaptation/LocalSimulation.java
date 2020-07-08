package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimpleSimulator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public class LocalSimulation {

    public static void main(String[] args) {
        final var schedule = List.of(
                Pair.of(Duration.of(0, TimeUnit.SECONDS), new SerializedActivity("RunInstrument",
                        Map.of("durationInSeconds", SerializedParameter.of(300),
                                "dataMode", SerializedParameter.of("LOW")))),
                Pair.of(Duration.of(600, TimeUnit.SECONDS), new SerializedActivity("RunInstrument",
                        Map.of("durationInSeconds", SerializedParameter.of(600),
                                "dataMode", SerializedParameter.of("MED")))),
                Pair.of(Duration.of(720, TimeUnit.SECONDS), new SerializedActivity("PreheatCamera",
                        Map.of("heatDurationInSeconds", SerializedParameter.of(1800)))),
                Pair.of(Duration.of(1620, TimeUnit.SECONDS), new SerializedActivity("CapturePanorama",
                                Map.of("nFramesHorizontal", SerializedParameter.of(4),
                                        "nFramesVertical", SerializedParameter.of(2),
                                        "imageQuality", SerializedParameter.of(90)))),
                Pair.of(Duration.of(25200, TimeUnit.SECONDS), new SerializedActivity("DownlinkData",
                        Map.of("downlinkAll", SerializedParameter.of(true),
                                "totalAmount", SerializedParameter.of(0))))
        );
        final var adaptation = new SampleAdaptation();
        final var results = SimpleSimulator.simulateToCompletion(adaptation, schedule, Duration.of(100, TimeUnit.MILLISECONDS));
    }
}
