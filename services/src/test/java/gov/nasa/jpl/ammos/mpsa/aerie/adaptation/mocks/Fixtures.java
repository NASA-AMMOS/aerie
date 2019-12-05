package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;

import java.nio.file.Path;
import java.util.Map;

public final class Fixtures {
    public static final Path resourcesRoot = Path.of("src/test/resources");
    public static final Path banananation = resourcesRoot.resolve("gov/nasa/jpl/ammos/mpsa/aerie/banananation-1.0-SNAPSHOT.jar");
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
        )),
        "ParameterTest", new ActivityType("ParameterTest", Map.of(
            "a", ParameterSchema.REAL,
            "b", ParameterSchema.REAL,
            "c", ParameterSchema.INT,
            "d", ParameterSchema.INT,
            "e", ParameterSchema.INT,
            "f", ParameterSchema.INT,
            "g", ParameterSchema.STRING,
            "h", ParameterSchema.STRING
        ), Map.of(
            "a", SerializedParameter.of(3.141),
            "b", SerializedParameter.of(1.618f),
            "c", SerializedParameter.of(16),
            "d", SerializedParameter.of(32),
            "e", SerializedParameter.of(64),
            "f", SerializedParameter.of(128),
            "g", SerializedParameter.of("g"),
            "h", SerializedParameter.of("h")
        ))
    );

    public Fixtures() {
        this.adaptationRepository = new MockAdaptationRepository();

        final NewAdaptation newAdaptation = new NewAdaptation();
        newAdaptation.name = "adaptation1";
        newAdaptation.version = "3";
        newAdaptation.mission = "Motherland";
        newAdaptation.owner = "Deris";
        newAdaptation.path = banananation;

        this.EXISTENT_ADAPTATION_ID = adaptationRepository.createAdaptation(newAdaptation);
    }

    public static NewAdaptation createValidNewAdaptation(final String name) {
        final NewAdaptation adaptation = new NewAdaptation();
        adaptation.name = name;
        adaptation.version = "1.0";
        adaptation.mission = "Merlin";
        adaptation.owner = "Arthur";
        adaptation.path = banananation;
        return adaptation;
    }
}
