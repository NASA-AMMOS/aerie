package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks.Fixtures;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchActivityTypeException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk.MissingAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public final class AdaptationControllerTest {

    private Fixtures fixtures;
    private IAdaptationController controller;

    @BeforeEach
    public void initialize() {
        fixtures = new Fixtures();
        controller = new AdaptationController(fixtures.adaptationRepository);
    }

    @Test
    public void shouldGetAllAdaptations() {
        // GIVEN
        final List<Pair<String, Adaptation>> expectedAdaptations = fixtures.adaptationRepository
                .getAllAdaptations()
                .collect(Collectors.toUnmodifiableList());

        // WHEN
        final List<Pair<String, Adaptation>> adaptations = controller.getAdaptations().collect(Collectors.toUnmodifiableList());

        // THEN
        assertThat(adaptations).isEqualTo(expectedAdaptations);
    }

    @Test
    public void shouldGetAdaptationById() throws NoSuchAdaptationException {
        // GIVEN
        final String adaptationId = fixtures.EXISTENT_ADAPTATION_ID;
        final Adaptation expectedAdaptation = fixtures.adaptationRepository.getAdaptation(adaptationId);

        // WHEN
        final Adaptation adaptation = controller.getAdaptationById(adaptationId);

        // THEN
        assertThat(adaptation).isEqualTo(expectedAdaptation);
    }

    @Test
    public void shouldGetNonexistentAdaptationById() {
        // GIVEN
        final String adaptationId = Fixtures.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.getAdaptationById(adaptationId));

        // THEN
        assertThat(thrown).isInstanceOf(NoSuchAdaptationException.class);

        final String invalidAdaptationId = ((NoSuchAdaptationException)thrown).getInvalidAdaptationId();
        assertThat(invalidAdaptationId).isEqualTo(adaptationId);
    }

    @Test
    public void shouldAddAdaptation() throws ValidationException, NoSuchAdaptationException {
        // GIVEN
        final NewAdaptation adaptation = Fixtures.createValidNewAdaptation("test");

        // WHEN
        final String adaptationId = controller.addAdaptation(adaptation);

        // THEN
        assertThat(fixtures.adaptationRepository.getAdaptation(adaptationId)).isNotNull();
    }

    @Test
    public void shouldNotAddAdaptationWithNoName() {
        // GIVEN
        final NewAdaptation adaptation = Fixtures.createValidNewAdaptation("test-no-name");
        adaptation.name = null;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.addAdaptation(adaptation));

        // THEN
        assertThat(thrown).isInstanceOf(ValidationException.class);

        final List<String> validationErrors = ((ValidationException)thrown).getValidationErrors();
        assertThat(validationErrors).size().isEqualTo(1);
        // TODO: Can we check that the error caused by not having a name?
    }

    @Test
    public void shouldNotAddAdaptationWithNoVersion() {
        // GIVEN
        final NewAdaptation adaptation = Fixtures.createValidNewAdaptation("test-no-version");
        adaptation.version = null;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.addAdaptation(adaptation));

        // THEN
        assertThat(thrown).isInstanceOf(ValidationException.class);

        final List<String> validationErrors = ((ValidationException)thrown).getValidationErrors();
        assertThat(validationErrors).size().isEqualTo(1);
        // TODO: Can we check that the error caused by not having a version?
    }

    @Test
    public void shouldNotAddAdaptationWithNoFile() {
        // GIVEN
        final NewAdaptation adaptation = Fixtures.createValidNewAdaptation("no-file");
        adaptation.path = null;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.addAdaptation(adaptation));

        // THEN
        assertThat(thrown).isInstanceOf(ValidationException.class);

        final List<String> validationErrors = ((ValidationException)thrown).getValidationErrors();
        assertThat(validationErrors).size().isEqualTo(1);
        // TODO: Can we check that the error caused by not having an adaptation?
    }

    @Test
    public void shouldRemoveAdaptation() throws NoSuchAdaptationException {
        // GIVEN
        final String id = fixtures.EXISTENT_ADAPTATION_ID;

        // WHEN
        controller.removeAdaptation(id);

        // THEN
        final Throwable thrown = catchThrowable(() -> controller.getAdaptationById(id));
        assertThat(thrown).isInstanceOf(NoSuchAdaptationException.class);

        final String invalidAdaptationId = ((NoSuchAdaptationException)thrown).getInvalidAdaptationId();
        assertThat(invalidAdaptationId).isEqualTo(id);
    }

    @Test
    public void shouldNotRemoveNonexistentAdaptation() {
        // GIVEN
        final String id = Fixtures.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.removeAdaptation(id));

        // THEN
        assertThat(thrown).isInstanceOf(NoSuchAdaptationException.class);

        final String invalidAdaptationId = ((NoSuchAdaptationException)thrown).getInvalidAdaptationId();
        assertThat(invalidAdaptationId).isEqualTo(id);
    }

    @Test
    public void shouldGetActivityTypeList() throws NoSuchAdaptationException, MissingAdaptationException {
        // GIVEN
        final String adaptationId = fixtures.EXISTENT_ADAPTATION_ID;
        final Map<String, ActivityType> expectedTypes = fixtures.ACTIVITY_TYPES;

        // WHEN
        final Map<String, ActivityType> typeList = controller.getActivityTypes(adaptationId);

        // THEN
        assertThat(typeList).isEqualTo(expectedTypes);
    }

    @Test
    public void shouldNotGetActivityTypeListForNonexistentAdaptation() {
        // GIVEN
        final String adaptationId = Fixtures.NONEXISTENT_ADAPTATION_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.getActivityTypes(adaptationId));

        // THEN
        assertThat(thrown).isInstanceOf(NoSuchAdaptationException.class);

        final String invalidAdaptationId = ((NoSuchAdaptationException)thrown).getInvalidAdaptationId();
        assertThat(invalidAdaptationId).isEqualTo(adaptationId);
    }

    @Test
    public void shouldGetActivityType() throws NoSuchAdaptationException, NoSuchActivityTypeException, MissingAdaptationException {
        // GIVEN
        final String adaptationId = fixtures.EXISTENT_ADAPTATION_ID;
        final String activityId = Fixtures.EXISTENT_ACTIVITY_TYPE_ID;
        final ActivityType expectedType = fixtures.ACTIVITY_TYPES.get(activityId);

        // WHEN
        final ActivityType type = controller.getActivityType(adaptationId, activityId);

        // THEN
        assertThat(type).isEqualTo(expectedType);
    }

    @Test
    public void shouldNotGetActivityTypeForNonexistentAdaptation() {
        // GIVEN
        final String adaptationId = Fixtures.NONEXISTENT_ADAPTATION_ID;
        final String activityId = Fixtures.EXISTENT_ACTIVITY_TYPE_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.getActivityType(adaptationId, activityId));

        // THEN
        assertThat(thrown).isInstanceOf(NoSuchAdaptationException.class);

        final String invalidAdaptationId = ((NoSuchAdaptationException)thrown).getInvalidAdaptationId();
        assertThat(invalidAdaptationId).isEqualTo(adaptationId);
    }

    @Test
    public void shouldNotGetActivityTypeForNonexistentActivityType() {
        // GIVEN
        final String adaptationId = fixtures.EXISTENT_ADAPTATION_ID;
        final String activityId = Fixtures.NONEXISTENT_ACTIVITY_TYPE_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.getActivityType(adaptationId, activityId));

        // THEN
        assertThat(thrown).isInstanceOf(NoSuchActivityTypeException.class);

        final String exceptionAdaptationId = ((NoSuchActivityTypeException)thrown).getAdaptationId();
        assertThat(exceptionAdaptationId).isEqualTo(adaptationId);

        final String invalidActivityId = ((NoSuchActivityTypeException)thrown).getInvalidActivityTypeId();
        assertThat(invalidActivityId).isEqualTo(activityId);
    }

    @Test
    public void shouldGetActivityTypeParameters() throws NoSuchAdaptationException, NoSuchActivityTypeException, MissingAdaptationException {
        // GIVEN
        final String adaptationId = fixtures.EXISTENT_ADAPTATION_ID;
        final String activityId = Fixtures.EXISTENT_ACTIVITY_TYPE_ID;
        final Map<String, ParameterSchema> expectedParameters = fixtures.ACTIVITY_TYPES.get(activityId).parameters;

        // WHEN
        final Map<String, ParameterSchema> parameters = controller.getActivityTypeParameters(adaptationId, activityId);

        // THEN
        assertThat(parameters).isEqualTo(expectedParameters);
    }

    @Test
    public void shouldNotGetActivityTypeParametersForNonexistentAdaptation() {
        // GIVEN
        final String adaptationId = Fixtures.NONEXISTENT_ADAPTATION_ID;
        final String activityId = Fixtures.EXISTENT_ACTIVITY_TYPE_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.getActivityTypeParameters(adaptationId, activityId));

        // THEN
        assertThat(thrown).isInstanceOf(NoSuchAdaptationException.class);

        final String invalidAdaptationId = ((NoSuchAdaptationException)thrown).getInvalidAdaptationId();
        assertThat(invalidAdaptationId).isEqualTo(adaptationId);
    }

    @Test
    public void shouldNotGetActivityTypeParametersForNonexistentActivityType() {
        // GIVEN
        final String adaptationId = fixtures.EXISTENT_ADAPTATION_ID;
        final String activityId = Fixtures.NONEXISTENT_ACTIVITY_TYPE_ID;

        // WHEN
        final Throwable thrown = catchThrowable(() -> controller.getActivityTypeParameters(adaptationId, activityId));

        // THEN
        assertThat(thrown).isInstanceOf(NoSuchActivityTypeException.class);

        final String exceptionAdaptationId = ((NoSuchActivityTypeException)thrown).getAdaptationId();
        assertThat(exceptionAdaptationId).isEqualTo(adaptationId);

        final String invalidActivityId = ((NoSuchActivityTypeException)thrown).getInvalidActivityTypeId();
        assertThat(invalidActivityId).isEqualTo(activityId);
    }
}
