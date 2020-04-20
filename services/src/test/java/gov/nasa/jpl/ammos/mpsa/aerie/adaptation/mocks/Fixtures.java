package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

public final class Fixtures {
    public static final URI bananationUri;
    static {
        try {
            bananationUri = Fixtures.class.getResource("/gov/nasa/jpl/ammos/mpsa/aerie/banananation.jar").toURI();
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    public final MockAdaptationRepository adaptationRepository;

    public final String EXISTENT_ADAPTATION_ID;
    public static final String EXISTENT_ACTIVITY_TYPE_ID = "PeelBanana";
    public static final String NONEXISTENT_ADAPTATION_ID = "nonexistent adaptation";
    public static final String NONEXISTENT_ACTIVITY_TYPE_ID = "nonexistent activity type";

    public final Map<String, ActivityType> ACTIVITY_TYPES = Map.of(
        "BiteBanana", new ActivityType("BiteBanana", Map.of(
            "biteSize", ParameterSchema.REAL
        ), Map.of(
            "biteSize", SerializedParameter.of(1.0)
        )),
        "PeelBanana", new ActivityType("PeelBanana", Map.of(
            "peelDirection", ParameterSchema.STRING
        ), Map.of(
            "peelDirection", SerializedParameter.of("fromStem")
        ))
    );

    public Fixtures() {
        this.adaptationRepository = new MockAdaptationRepository();

        final AdaptationJar adaptationJar = new AdaptationJar();
        adaptationJar.name = "Banananation";
        adaptationJar.version = "0.0.1";
        adaptationJar.mission = "Motherland";
        adaptationJar.owner = "Deris";
        adaptationJar.path = Path.of(bananationUri);

        this.EXISTENT_ADAPTATION_ID = adaptationRepository.createAdaptation(adaptationJar);
    }

    public static AdaptationJar createValidAdaptationJar(final String mission) {
        final AdaptationJar adaptation = new AdaptationJar();
        adaptation.name = "Banananation";
        adaptation.version = "0.0.1";
        adaptation.mission = mission;
        adaptation.owner = "Arthur";
        adaptation.path = Path.of(bananationUri);
        return adaptation;
    }
}
