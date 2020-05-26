package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.delay;

public class CaptureImage implements Activity {

    @Parameter
    public int imageQuality = 95;

    public CaptureImage(int imageQuality) {
        this.imageQuality = imageQuality;
    }

    @Override
    public List<String> validateParameters() {
        List<String> validationErrors = new ArrayList<>();
        if (imageQuality < 0 || imageQuality > 100) {
            validationErrors.add("imageQuality must be between 0 and 100");
        }

        return validationErrors;
    }

    @Override
    public void modelEffects() {
        var states = SampleMissionStates.getModel();
        double imageSizeMbits = 1 + imageQuality*0.02;
        double imageDuration = imageQuality < 100 ? 2 + imageQuality * 0.1 : 30;
        double dataGenerationRate = imageSizeMbits / imageDuration;

        states.cameraData.turnOn(dataGenerationRate);

        delay(Duration.of((long)imageDuration, TimeUnit.SECONDS));

        states.cameraData.turnOff();
    }
}
