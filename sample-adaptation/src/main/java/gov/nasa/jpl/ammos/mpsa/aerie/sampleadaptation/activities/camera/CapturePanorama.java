package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Parameter;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates.cameraPointing;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;

@ActivityType(name="CapturePanorama")
public class CapturePanorama implements Activity {

    @Parameter
    public int nFramesHorizontal = 2;

    @Parameter
    public int nFramesVertical = 2;

    @Parameter
    public int imageQuality = 90;

    @Parameter
    public Vector3D focusPoint; // Null allowed to use current pointing

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

        if (focusPoint != null && focusPoint.equals(Vector3D.ZERO)) {
            validationErrors.add("focusPoint must be a non-zero vector, or null to use current pointing");
        }

        return validationErrors;
    }

    @Override
    public void modelEffects() {
        // Determine the center of the panorama
        Vector3D centerPoint = (focusPoint != null) ? focusPoint.normalize() : cameraPointing.get();

        // Determine camera pointing elevations
        double frameHeight = 0.167; // radians (30 degrees)
        double frameWidth = 0.167; // radians (30 degrees)
        double minimumElevation = centerPoint.getDelta() - (frameHeight * (nFramesVertical-1)/2.0);
        double minimumAzimuth = centerPoint.getAlpha() - (frameWidth * (nFramesHorizontal-1)/2.0);

        for (int verticalIndex = 0; verticalIndex < nFramesVertical; verticalIndex++) {
            double elevation = minimumElevation + frameHeight*verticalIndex;
            for(int horizontalIndex = 0; horizontalIndex < nFramesHorizontal; horizontalIndex++) {
                double azimuth = minimumAzimuth + frameWidth*horizontalIndex;
                ctx.call(new PointCamera(new Vector3D(azimuth, elevation)));
                ctx.call(new CaptureImage(imageQuality));
            }
        }

        // Return camera to original pointing
        ctx.call(new PointCamera(centerPoint));
    }
}
