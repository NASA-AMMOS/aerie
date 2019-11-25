package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;

import java.nio.file.Path;

public final class Fixtures {
    public static final Path resourcesRoot = Path.of("src/test/resources");
    public static final Path banananation = resourcesRoot.resolve("gov/nasa/jpl/ammos/mpsa/aerie/banananation-1.0-SNAPSHOT.jar");
    public final MockAdaptationRepository adaptationRepository;

    public final String EXISTENT_ADAPTATION_ID;
    public static final String EXISTENT_ACTIVITY_TYPE_ID = "PeelBanana";
    public static final String NONEXISTENT_ADAPTATION_ID = "nonexistent adaptation";
    public static final String NONEXISTENT_ACTIVITY_TYPE_ID = "nonexistent activity type";

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
