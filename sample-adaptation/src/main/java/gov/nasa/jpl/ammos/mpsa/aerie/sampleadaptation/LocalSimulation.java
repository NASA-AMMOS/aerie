package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimpleSimulator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimulationResults;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public class LocalSimulation {

    public static void printValues(String id, SimulationResults results, String label) {
        List<SerializedParameter> values = results.timelines.get(id);
        System.out.println(label);
        for (int i = 1; i < values.size(); i++) {
            if (!(values.get(i-1).equals(values.get(i)))){
                System.out.println("Value: " + values.get(i) + " Time: " + results.timestamps.get(i));
            }
        }
    }

    public static void printConstraintWindows(String id, SimulationResults results, String label) {
        System.out.println(label);
        for (var i : results.constraintViolations){
            if (i.id.equals(id)){
                System.out.println(i.violationWindows);
            }
        }
    }

    public static void main(String[] args) {
        final var schedule = List.of(
                Pair.of(Duration.of(0, Duration.SECONDS), new SerializedActivity("RunInstrument",
                        Map.of("durationInSeconds", SerializedParameter.of(300),
                                "dataMode", SerializedParameter.of("LOW")))),
                Pair.of(Duration.of(600, Duration.SECONDS), new SerializedActivity("RunInstrument",
                        Map.of("durationInSeconds", SerializedParameter.of(600),
                                "dataMode", SerializedParameter.of("MED")))),
                Pair.of(Duration.of(720, Duration.SECONDS), new SerializedActivity("PreheatCamera",
                        Map.of("heatDurationInSeconds", SerializedParameter.of(1800)))),
                Pair.of(Duration.of(1620, Duration.SECONDS), new SerializedActivity("CapturePanorama",
                                Map.of("nFramesHorizontal", SerializedParameter.of(4),
                                        "nFramesVertical", SerializedParameter.of(2),
                                        "imageQuality", SerializedParameter.of(90)))),
                Pair.of(Duration.of(1900, Duration.SECONDS), new SerializedActivity("RunInstrument",
                        Map.of("durationInSeconds", SerializedParameter.of(3000),
                                "dataMode", SerializedParameter.of("MED")))),
                Pair.of(Duration.of(25200, Duration.SECONDS), new SerializedActivity("DownlinkData",
                        Map.of("downlinkAll", SerializedParameter.of(true),
                                "totalBits", SerializedParameter.of(0)))),
                Pair.of(Duration.of(30000, Duration.SECONDS), new SerializedActivity("RunInstrument",
                        Map.of("durationInSeconds", SerializedParameter.of(7500),
                                "dataMode", SerializedParameter.of("MED")))),
                Pair.of(Duration.of(40000, Duration.SECONDS), new SerializedActivity("CapturePanorama",
                        Map.of("nFramesHorizontal", SerializedParameter.of(8),
                                "nFramesVertical", SerializedParameter.of(2),
                                "imageQuality", SerializedParameter.of(80))))
        );
        final var adaptation = new SampleAdaptation();
        final var results = SimpleSimulator.simulateToCompletion(adaptation, schedule, Duration.of(100, Duration.MILLISECONDS));

        /*
        Minimum allowed battery capacity is Config.startBatteryCapacity_J*0.3
        Here are the step changes in the battery capacity (this can also be seen using the UI).
        From this we can see that we do have values that are below the minimum SoC.
         */
        System.out.println("Min SOC " + Config.startBatteryCapacity_J * 0.3);
        System.out.println("Low SOC range (" + Config.startBatteryCapacity_J * 0.5 + ", " + Config.startBatteryCapacity_J * 0.3 + "]");
        printValues("batteryCapacity", results, "\nBattery Capacity values: ");

        /*
        A constraint was created to check if the battery SoC decreases below 30%. (see Sample Mission States).
        While we can view the violations in the UI, we can also print them out here.
         */
        printConstraintWindows("minSOC", results, "\nConstraint windows for min SOC: ");

        /*
         A constraint was created to check if the battery SoC dips into a warning zone. (see Sample Mission States).
         */
        printConstraintWindows("lowSOC", results, "\nConstraint windows for low SOC: ");

        /*
        Here are the step changes in the instrument (this can also be seen using the UI).
        We can also view the constraint windows.
         */
        printValues("instrumentData", results, "\nInstrument Data values: ");
        printConstraintWindows("maxAllocatedInstrumentData", results,  "\nConstraint windows for max instrument data: ");


        /*
        Here are the step changes in the camera (this can also be seen using the UI).
        From this we can see that we do have values that are below the minimum SoC.
         */
        printValues("cameraData", results, "\nCamera Data values: ");
        printConstraintWindows("maxAllocatedCameraData", results,  "\nConstraint windows for max camera data: ");
    }
}
