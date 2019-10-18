package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.InvalidAdaptationJARException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
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
    public void testGetAdaptation() throws NoSuchAdaptationException {
        // GIVEN
        final NewAdaptation newAdaptation = Fixtures.createValidNewAdaptation("new-adaptation");
        final String id = this.adaptationRepository.createAdaptation(newAdaptation);

        // WHEN
        final Adaptation adaptation = this.adaptationRepository.getAdaptation(id);

        // THEN
        assertThat(adaptation.name).isEqualTo(newAdaptation.name);
    }

    @Test
    public void testRetrieveAllAdaptations() {
        // GIVEN
        final String id1 = this.adaptationRepository.createAdaptation(Fixtures.createValidNewAdaptation("test1"));
        final String id2 = this.adaptationRepository.createAdaptation(Fixtures.createValidNewAdaptation("test2"));
        final String id3 = this.adaptationRepository.createAdaptation(Fixtures.createValidNewAdaptation("test3"));

        // WHEN
        final Map<String, Adaptation> adaptations = this.adaptationRepository
                .getAllAdaptations()
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        // THEN
        assertThat(adaptations.size()).isEqualTo(3);
        assertThat(adaptations.get(id1)).isNotNull();
        assertThat(adaptations.get(id2)).isNotNull();
        assertThat(adaptations.get(id3)).isNotNull();
    }

    @Test
    public void testGetActivityTypeParameters() throws NoSuchAdaptationException, NoSuchActivityTypeException, InvalidAdaptationJARException {
        // GIVEN
        final NewAdaptation adaptation = Fixtures.createValidNewAdaptation("test");
        adaptation.path = Fixtures.banananation;
        final String adaptationId = this.adaptationRepository.createAdaptation(adaptation);
        final String activityId = Fixtures.EXISTENT_ACTIVITY_TYPE_ID;

        // WHEN
        final Map<String, ParameterSchema> parameters = this.adaptationRepository
                .getActivityTypeParameters(adaptationId, activityId);

        // THEN
        assertThat(parameters).isNotEmpty();
    }

    @Test
    public void testCanDeleteAllAdaptations() throws NoSuchAdaptationException {
        // GIVEN
        final String id1 = this.adaptationRepository.createAdaptation(Fixtures.createValidNewAdaptation("test1"));
        final String id2 = this.adaptationRepository.createAdaptation(Fixtures.createValidNewAdaptation("test2"));
        final String id3 = this.adaptationRepository.createAdaptation(Fixtures.createValidNewAdaptation("test3"));

        // WHEN
        this.adaptationRepository.deleteAdaptation(id1);
        this.adaptationRepository.deleteAdaptation(id2);
        this.adaptationRepository.deleteAdaptation(id3);

        // THEN
        assertThat(this.adaptationRepository.getAllAdaptations()).isEmpty();
    }
}
