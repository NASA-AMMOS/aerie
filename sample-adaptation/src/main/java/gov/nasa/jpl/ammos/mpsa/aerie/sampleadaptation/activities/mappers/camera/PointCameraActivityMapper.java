package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.mappers.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.contrib.typemappers.Vector3DValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ActivitiesMapped;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.NullableValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera.PointCamera;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

@ActivitiesMapped(PointCamera.class)
public final class PointCameraActivityMapper implements ActivityMapper {
    private static final String ACTIVITY_TYPE = "PointCamera";

    private static final ValueMapper<Vector3D> mapper_focusPoint = new NullableValueMapper<>(new Vector3DValueMapper());

    @Override
    public Map<String, Map<String, ValueSchema>> getActivitySchemas() {
        final var parameters = new HashMap<String, ValueSchema>();
        parameters.put("focusPoint", this.mapper_focusPoint.getValueSchema());
        return Map.of(ACTIVITY_TYPE, parameters);
    }

    @Override
    public Optional<Activity> deserializeActivity(final SerializedActivity serializedActivity) {
        if (!serializedActivity.getTypeName().equals(ACTIVITY_TYPE)) {
            return Optional.empty();
        }

        final var activity = new PointCamera();
        for (final var entry : serializedActivity.getParameters().entrySet()) {
            switch (entry.getKey()) {
                case "focusPoint":
                    activity.focusPoint = this.mapper_focusPoint.deserializeValue(entry.getValue()).getSuccessOrThrow();
                    break;
                default:
                    throw new RuntimeException("Unknown key `" + entry.getKey() + "`");
            }
        }

        return Optional.of(activity);
    }

    @Override
    public Optional<SerializedActivity> serializeActivity(final Activity abstractActivity) {
        if (!(abstractActivity instanceof PointCamera)) return Optional.empty();
        final PointCamera activity = (PointCamera) abstractActivity;

        final var parameters = new HashMap<String, SerializedValue>();
        parameters.put("focusPoint", this.mapper_focusPoint.serializeValue(activity.focusPoint));

        return Optional.of(new SerializedActivity(ACTIVITY_TYPE, parameters));
    }
}
