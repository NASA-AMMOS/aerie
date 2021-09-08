package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Note:
//   If the tests fail, please try to update the adaptation jar file.
//   See the `jarFixturePath` field below for the expected (CLASSPATH-relative) location.
public abstract class AdaptationRepositoryContractTest {
    private final Path jarFixturePath;
    {
        try {
            jarFixturePath = Path.of(
                this.getClass()
                    .getResource("/gov/nasa/jpl/ammos/mpsa/aerie/foo-adaptation-0.6.0-SNAPSHOT.jar")
                    .toURI());
        } catch (final URISyntaxException ex) {
          throw new Error("Unable to find adaptation fixture", ex);
        }
    }

    protected AdaptationRepository adaptationRepository = null;

    protected abstract void resetRepository();

    @BeforeEach
    public void resetRepositoryBeforeEachTest() {
        this.resetRepository();
    }

    @Test
    public void testGetAdaptation() throws AdaptationRepository.NoSuchAdaptationException {
        // GIVEN
        final AdaptationJar newAdaptation = createValidAdaptationJar("new-adaptation");
        final String id = this.adaptationRepository.createAdaptation(newAdaptation);

        // WHEN
        final AdaptationJar adaptation = this.adaptationRepository.getAdaptation(id);

        // THEN
        assertThat(adaptation.mission).isEqualTo(newAdaptation.mission);
    }

    @Test
    public void testRetrieveAllAdaptations() {
        // GIVEN
        final String id1 = this.adaptationRepository.createAdaptation(createValidAdaptationJar("test1"));
        final String id2 = this.adaptationRepository.createAdaptation(createValidAdaptationJar("test2"));
        final String id3 = this.adaptationRepository.createAdaptation(createValidAdaptationJar("test3"));

        // WHEN
        final Map<String, AdaptationJar> adaptations = this.adaptationRepository
                .getAllAdaptations();

        // THEN
        assertThat(adaptations.size()).isEqualTo(3);
        assertThat(adaptations.get(id1)).isNotNull();
        assertThat(adaptations.get(id2)).isNotNull();
        assertThat(adaptations.get(id3)).isNotNull();
    }

    @Test
    public void testCanDeleteAllAdaptations() throws AdaptationRepository.NoSuchAdaptationException {
        // GIVEN
        final String id1 = this.adaptationRepository.createAdaptation(createValidAdaptationJar("test1"));
        final String id2 = this.adaptationRepository.createAdaptation(createValidAdaptationJar("test2"));
        final String id3 = this.adaptationRepository.createAdaptation(createValidAdaptationJar("test3"));

        // WHEN
        this.adaptationRepository.deleteAdaptation(id1);
        this.adaptationRepository.deleteAdaptation(id2);
        this.adaptationRepository.deleteAdaptation(id3);

        // THEN
        assertThat(this.adaptationRepository.getAllAdaptations()).isEmpty();
    }

    protected AdaptationJar createValidAdaptationJar(final String mission) {
        final AdaptationJar adaptation = new AdaptationJar();
        adaptation.name = "foo-adaptation-0.6.0-SNAPSHOT";
        adaptation.version = "0.0.1";
        adaptation.mission = mission;
        adaptation.owner = "Arthur";
        adaptation.path = this.jarFixturePath;
        return adaptation;
    }
}
