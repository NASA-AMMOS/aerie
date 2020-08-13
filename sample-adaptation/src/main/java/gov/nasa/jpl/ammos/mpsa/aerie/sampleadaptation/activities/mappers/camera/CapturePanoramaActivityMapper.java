package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.mappers.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.contrib.typemappers.Vector3DValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ActivitiesMapped;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.IntegerValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.NullableValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;
import java.lang.Integer;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera.CapturePanorama;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

@ActivitiesMapped(CapturePanorama.class)
public final class CapturePanoramaActivityMapper implements ActivityMapper {
    private static final String ACTIVITY_TYPE = "CapturePanorama";

    private static final ValueMapper<Integer> mapper_nFramesHorizontal = new NullableValueMapper<>(new IntegerValueMapper());

    private static final ValueMapper<Integer> mapper_nFramesVertical = new NullableValueMapper<>(new IntegerValueMapper());

    private static final ValueMapper<Integer> mapper_imageQuality = new NullableValueMapper<>(new IntegerValueMapper());

    private static final ValueMapper<Vector3D> mapper_focusPoint = new NullableValueMapper<>(new Vector3DValueMapper());

    @Override
    public Map<String, Map<String, ValueSchema>> getActivitySchemas() {
        final var parameters = new HashMap<String, ValueSchema>();
        parameters.put("nFramesHorizontal", this.mapper_nFramesHorizontal.getValueSchema());
        parameters.put("nFramesVertical", this.mapper_nFramesVertical.getValueSchema());
        parameters.put("imageQuality", this.mapper_imageQuality.getValueSchema());
        parameters.put("focusPoint", this.mapper_focusPoint.getValueSchema());
        return Map.of(ACTIVITY_TYPE, parameters);
    }

    @Override
    public Optional<Activity> deserializeActivity(final SerializedActivity serializedActivity) {
        if (!serializedActivity.getTypeName().equals(ACTIVITY_TYPE)) {
            return Optional.empty();
        }

        final var activity = new CapturePanorama();
        for (final var entry : serializedActivity.getParameters().entrySet()) {
            switch (entry.getKey()) {
                case "nFramesHorizontal":
                    activity.nFramesHorizontal = this.mapper_nFramesHorizontal.deserializeValue(entry.getValue()).getSuccessOrThrow();
                    break;
                case "nFramesVertical":
                    activity.nFramesVertical = this.mapper_nFramesVertical.deserializeValue(entry.getValue()).getSuccessOrThrow();
                    break;
                case "imageQuality":
                    activity.imageQuality = this.mapper_imageQuality.deserializeValue(entry.getValue()).getSuccessOrThrow();
                    break;
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
        if (!(abstractActivity instanceof CapturePanorama)) return Optional.empty();
        final CapturePanorama activity = (CapturePanorama)abstractActivity;

        final var parameters = new HashMap<String, SerializedValue>();
        parameters.put("nFramesHorizontal", this.mapper_nFramesHorizontal.serializeValue(activity.nFramesHorizontal));
        parameters.put("nFramesVertical", this.mapper_nFramesVertical.serializeValue(activity.nFramesVertical));
        parameters.put("imageQuality", this.mapper_imageQuality.serializeValue(activity.imageQuality));
        parameters.put("focusPoint", this.mapper_focusPoint.serializeValue(activity.focusPoint));

        return Optional.of(new SerializedActivity(ACTIVITY_TYPE, parameters));
    }
}
