package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;

@ActivityType(name="CapturePanorama", generateMapper=true)
public class CapturePanorama implements Activity {

    @Parameter
    public int nFramesHorizontal = 2;

    @Parameter
    public int nFramesVertical = 2;

    @Parameter
    public int imageQuality = 90;

    @Override
    public List<String> validateParameters() {
        List<String> validationErrors = new ArrayList<>();

        if (nFramesHorizontal < 1 || nFramesHorizontal > 4) {
            validationErrors.add("nFramesHorizontal must be between 1 and 4");
        }

        if (nFramesVertical < 1 || nFramesVertical > 4) {
            validationErrors.add("nFramesVertical must be between 1 and 4");
        }

        if (imageQuality < 0 || imageQuality > 100) {
            validationErrors.add("imageQuality must be between 0 and 100");
        }

        return validationErrors;
    }

    @Override
    public void modelEffects() {
        for (int verticalIndex = 0; verticalIndex < nFramesVertical; verticalIndex++) {
            for(int horizontalIndex = 0; horizontalIndex < nFramesHorizontal; horizontalIndex++) {
                ctx.call(new CaptureImage(imageQuality));
            }
        }
    }
}
