package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.matchers.JsonMatcher;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.mocks.MockHttpHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.*;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.AdaptationRepository.*;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JsonUtilities;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.HttpUtilities.createBasicHttpResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

public class RemoteAdaptationRepositoryTests {

    final MockHttpHandler requestHandler = new MockHttpHandler();
    final AdaptationRepository repository = new RemoteAdaptationRepository(requestHandler);

    @Test
    public void testCreateAdaptation() throws InvalidAdaptationException {
        String expected_id = "test_id_adaptation_creation_success";
        Adaptation testAdaptation = new Adaptation("test-name", "test-version", "test-owner", "test-mission");

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_CREATED);
        response.addHeader("location", expected_id);
        requestHandler.setNextResponse(response);

        // Call repository method
        String id = repository.createAdaptation(testAdaptation, new File(""));

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/adaptations");
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpPost.METHOD_NAME);

        // TODO: Verify the body of the request contained the necessary information
        HttpEntity requestBody = ((HttpPost)request).getEntity();

        // Verify output corresponds to that in the response
        assertThat(id).isEqualTo(expected_id);
    }

    @Test
    public void testCreateAdaptationInvalidAdaptation() {
        Adaptation testAdaptation = new Adaptation("test-name", "test-version", "test-owner", "test-mission");

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.createAdaptation(testAdaptation, new File("")));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(InvalidAdaptationException.class);
    }

    @Test
    public void testDeleteAdaptation() throws AdaptationNotFoundException {
        String adaptationId = "test-adaptation-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.deleteAdaptation(adaptationId);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/adaptations/%s", adaptationId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpDelete.METHOD_NAME);
    }

    @Test
    public void testDeleteAdaptationNotFound() {
        String adaptationId = "test-adaptation-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.deleteAdaptation(adaptationId));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(AdaptationNotFoundException.class);
    }

    @Test
    public void testGetAdaptation() throws AdaptationNotFoundException, IOException {
        String adaptationId = "test-adaptation-id";
        Adaptation expectedAdaptation = new Adaptation("test-get-name", "test-get-version", "test-get-owner", "test-get-mission");

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(JsonUtilities.convertAdaptationToJson(expectedAdaptation)));
        requestHandler.setNextResponse(response);

        // Call repository method
        Adaptation adaptation = repository.getAdaptation(adaptationId);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/adaptations/%s", adaptationId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpGet.METHOD_NAME);

        assertThat(adaptation).isEqualTo(expectedAdaptation);
    }

    @Test
    public void testGetAdaptationNotFound() {
        String adaptationId = "test-adaptation-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.getAdaptation(adaptationId));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(AdaptationNotFoundException.class);
    }

    @Test
    public void testGetAdaptationList() throws IOException {
        String expectedAdaptationListJson = "[{},{}]";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(expectedAdaptationListJson));
        requestHandler.setNextResponse(response);

        // Call repository method
        String adaptationListJson = repository.getAdaptationList();

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/adaptations");
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpGet.METHOD_NAME);

        assertThat(adaptationListJson).isEqualTo(expectedAdaptationListJson);
    }

    @Test
    public void testGetActivityTypes() throws AdaptationNotFoundException, IOException {
        String adaptationId = "test-activity-types-id";
        String expectedActivityListJson = "[{\"type\":\"test-type-1\"},{\"type\":\"test-type-2\"}]";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(expectedActivityListJson));
        requestHandler.setNextResponse(response);

        // Call repository method
        String activityListJson = repository.getActivityTypes(adaptationId);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/adaptations/%s/activities", adaptationId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpGet.METHOD_NAME);

        assertThat(new JsonMatcher(activityListJson).matches(expectedActivityListJson)).isTrue();
    }

    @Test
    public void testGetActivityTypesAdaptationNotFound() {
        String adaptationId = "test-activity-types-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.getActivityTypes(adaptationId));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(AdaptationNotFoundException.class);
    }

    @Test
    public void testGetActivityType() throws AdaptationNotFoundException, ActivityTypeNotDefinedException, IOException {
        String adaptationId = "test-activity-type-id";
        String activityName = "test-activity-type-name";
        String expectedActivityTypeJson = "{\"type\":\"test-activity-type\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(expectedActivityTypeJson));
        requestHandler.setNextResponse(response);

        // Call repository method
        String activityTypeJson = repository.getActivityType(adaptationId, activityName);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/adaptations/%s/activities/%s", adaptationId, activityName);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpGet.METHOD_NAME);

        assertThat(new JsonMatcher(activityTypeJson).matches(expectedActivityTypeJson)).isTrue();
    }

    @Disabled("No way to distinguish AdaptationNotFound from ActivityTypeNotDefined yet")
    @Test
    public void testGetActivityTypeAdaptationNotFound() {
        String adaptationId = "test-activity-type-id";
        String activityName = "test-activity-type-name";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.getActivityType(adaptationId, activityName));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(AdaptationNotFoundException.class);
    }

    @Test
    public void testGetActivityTypeActivityNotDefined() {
        String adaptationId = "test-activity-type-id";
        String activityName = "test-activity-type-name";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.getActivityType(adaptationId, activityName));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(ActivityTypeNotDefinedException.class);
    }
}
