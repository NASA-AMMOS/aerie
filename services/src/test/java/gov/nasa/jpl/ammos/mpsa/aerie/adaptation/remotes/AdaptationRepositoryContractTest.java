package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AdaptationRepositoryContractTest {
    protected AdaptationRepository adaptationRepository = null;

    protected abstract void resetRepository();

    @BeforeEach
    public void resetRepositoryBeforeEachTest() {
        this.resetRepository();
    }

    @Test
    public void testGetAdaptation() throws AdaptationRepository.NoSuchAdaptationException {
        // GIVEN
        final AdaptationJar newAdaptation = Fixtures.createValidAdaptationJar("new-adaptation");
        final String id = this.adaptationRepository.createAdaptation(newAdaptation);

        // WHEN
        final AdaptationJar adaptation = this.adaptationRepository.getAdaptation(id);

        // THEN
        assertThat(adaptation.name).isEqualTo(newAdaptation.name);
    }

    @Test
    public void testRetrieveAllAdaptations() {
        // GIVEN
        final String id1 = this.adaptationRepository.createAdaptation(Fixtures.createValidAdaptationJar("test1"));
        final String id2 = this.adaptationRepository.createAdaptation(Fixtures.createValidAdaptationJar("test2"));
        final String id3 = this.adaptationRepository.createAdaptation(Fixtures.createValidAdaptationJar("test3"));

        // WHEN
        final Map<String, AdaptationJar> adaptations = this.adaptationRepository
                .getAllAdaptations()
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        // THEN
        assertThat(adaptations.size()).isEqualTo(3);
        assertThat(adaptations.get(id1)).isNotNull();
        assertThat(adaptations.get(id2)).isNotNull();
        assertThat(adaptations.get(id3)).isNotNull();
    }

    @Test
    public void testCanDeleteAllAdaptations() throws AdaptationRepository.NoSuchAdaptationException {
        // GIVEN
        final String id1 = this.adaptationRepository.createAdaptation(Fixtures.createValidAdaptationJar("test1"));
        final String id2 = this.adaptationRepository.createAdaptation(Fixtures.createValidAdaptationJar("test2"));
        final String id3 = this.adaptationRepository.createAdaptation(Fixtures.createValidAdaptationJar("test3"));

        // WHEN
        this.adaptationRepository.deleteAdaptation(id1);
        this.adaptationRepository.deleteAdaptation(id2);
        this.adaptationRepository.deleteAdaptation(id3);

        // THEN
        assertThat(this.adaptationRepository.getAllAdaptations()).isEmpty();
    }
}
