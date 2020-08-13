package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates.batteryCapacity;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates.cameraPointing;

@ActivityType(name="PointCamera")
public class PointCamera implements Activity {

    @Parameter
    public Vector3D focusPoint;

    public List<String> validateParameters() {
        List<String> validationErrors = new ArrayList<>();

        if (focusPoint == null || focusPoint.equals(Vector3D.ZERO)) {
            return List.of("focusPoint must be a non-zero vector");
        }

        return validationErrors;
    }

    public void modelEffects() {
        Vector3D currentPointing = cameraPointing.get();
        Vector3D targetPointing = focusPoint.normalize();

        double deltaAngle = Vector3D.angle(currentPointing, targetPointing);
        double rotationDurationInSeconds = deltaAngle / Config.cameraRotationRate;
        double powerUsed = Config.cameraRotationPower * rotationDurationInSeconds;

        ctx.delay(Duration.of((long)rotationDurationInSeconds, Duration.SECONDS));

        batteryCapacity.add(-powerUsed);
        cameraPointing.set(targetPointing);
    }
}
