package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates.cameraDataBits;

@ActivityType(name="CaptureImage", generateMapper=true)
public class CaptureImage implements Activity {

    @Parameter
    public int imageQuality = 95;

    // Explicitly include no-arg constructor for Merlin activity mapper
    public CaptureImage() {}

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
        double imageSizeBits = (1 + imageQuality*0.02)*1000000;
        double imageDuration = imageQuality < 100 ? 2 + imageQuality * 0.1 : 30;
        cameraDataBits.add(imageSizeBits);

        ctx.delay(Duration.of((long)imageDuration, TimeUnit.SECONDS));
    }
}
