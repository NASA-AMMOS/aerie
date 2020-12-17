package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

public final class Fixtures {
    public static final URI fooAdaptationUri;
    static {
        try {
            fooAdaptationUri = Fixtures.class
                .getResource("/gov/nasa/jpl/ammos/mpsa/aerie/foo-adaptation-0.6.0-SNAPSHOT.jar")
                .toURI();
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    public final MockAdaptationRepository adaptationRepository;

    public final String EXISTENT_ADAPTATION_ID;
    public static final String EXISTENT_ACTIVITY_TYPE_ID = "foo";
    public static final String NONEXISTENT_ADAPTATION_ID = "nonexistent adaptation";
    public static final String NONEXISTENT_ACTIVITY_TYPE_ID = "nonexistent activity type";

    public final Map<String, ActivityType> ACTIVITY_TYPES = Map.of(
        "foo", new ActivityType("foo", Map.of(
            "x", ValueSchema.INT,
            "y", ValueSchema.STRING
        ), Map.of(
            "x", SerializedValue.of(0),
            "y", SerializedValue.of("test")
        ))
    );

    public Fixtures() {
        this.adaptationRepository = new MockAdaptationRepository();

        final AdaptationJar adaptationJar = new AdaptationJar();
        adaptationJar.name = "foo-adaptation-0.6.0-SNAPSHOT";
        adaptationJar.version = "0.0.1";
        adaptationJar.mission = "Motherland";
        adaptationJar.owner = "Deris";
        adaptationJar.path = Path.of(fooAdaptationUri);

        this.EXISTENT_ADAPTATION_ID = adaptationRepository.createAdaptation(adaptationJar);
    }

    public static AdaptationJar createValidAdaptationJar(final String mission) {
        final AdaptationJar adaptation = new AdaptationJar();
        adaptation.name = "foo-adaptation-0.6.0-SNAPSHOT";
        adaptation.version = "0.0.1";
        adaptation.mission = mission;
        adaptation.owner = "Arthur";
        adaptation.path = Path.of(fooAdaptationUri);
        return adaptation;
    }
}
