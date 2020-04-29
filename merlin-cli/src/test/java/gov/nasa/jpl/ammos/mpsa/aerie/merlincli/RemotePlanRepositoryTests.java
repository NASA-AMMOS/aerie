package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.matchers.JsonMatcher;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.mocks.MockHttpHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.RemotePlanRepository;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanRepository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.HttpUtilities.createBasicHttpResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;


public class RemotePlanRepositoryTests {

    MockHttpHandler requestHandler = new MockHttpHandler();
    PlanRepository repository = new RemotePlanRepository(requestHandler);

    @Test
    public void testCreatePlan() throws InvalidJsonException, InvalidPlanException, IOException {
        String expected_id = "test_id_plan_creation_success";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_CREATED);
        response.addHeader("location", expected_id);
        requestHandler.setNextResponse(response);

        // Call repository method
        String id = repository.createPlan(json);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans");
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpPost.METHOD_NAME);
        assertThat(new String(
                ((HttpPost)request)
                        .getEntity()
                        .getContent()
                        .readAllBytes()
                )
        ).isEqualTo(json);

        // Verify output corresponds to that in the response
        assertThat(id).isEqualTo(expected_id);
    }

    @Test
    public void testCreatePlanInvalidJson() {
        String json = "not valid json";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_BAD_REQUEST);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.createPlan(json));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(InvalidJsonException.class);
    }

    @Test
    public void testCreatePlanInvalidPlan() {
        String json = "{\"info\": \"not a valid plan\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.createPlan(json));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(InvalidPlanException.class);
    }

    @Test
    public void testUpdatePlan() throws PlanNotFoundException, InvalidJsonException, InvalidPlanException, IOException {
        String planId = "test-update-plan-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.updatePlan(planId, json);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans/%s", planId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpPatch.METHOD_NAME);
        assertThat(new String(
                        ((HttpPatch)request)
                                .getEntity()
                                .getContent()
                                .readAllBytes()
                )
        ).isEqualTo(json);
    }

    @Test
    public void testUpdatePlanNotFound() {
        String planId = "test-update-plan-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.updatePlan(planId, json));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    public void testUpdatePlanInvalidJson() {
        String planId = "test-update-plan-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_BAD_REQUEST);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.updatePlan(planId, json));

        // Verify InvalidJsonException was thrown
        assertThat(thrown).isInstanceOf(InvalidJsonException.class);
    }

    @Test
    public void testUpdatePlanInvalidPlan() {
        String planId = "test-update-plan-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.updatePlan(planId, json));

        // Verify InvalidPlanException was thrown
        assertThat(thrown).isInstanceOf(InvalidPlanException.class);
    }

    @Test
    public void testDeletePlan() throws PlanNotFoundException {
        String planId = "test-delete-plan-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.deletePlan(planId);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans/%s", planId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpDelete.METHOD_NAME);
    }

    @Test
    public void testDeletePlanNotFound() {
        String planId = "test-delete-plan-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.deletePlan(planId));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    public void testDownloadPlan() throws PlanNotFoundException, IOException {
        String planId = "test-update-plan-id";
        String outname = ".download-plan-test.39fjf39j239j24223.tmp";
        String planBody = "{\"plan-test\": \"it is a test!\"}";

        // Ensure the file does not already exist
        Files.deleteIfExists(Path.of(outname));

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(planBody));
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.downloadPlan(planId, outname);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans/%s", planId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpGet.METHOD_NAME);

        // Verify plan was written to expected file
        String downloadedContents = new String(Files.readAllBytes(Path.of(outname)));
        assertThat(new JsonMatcher(downloadedContents).matches(planBody)).isTrue();

        // Cleanup
        Files.deleteIfExists(Path.of(outname));
    }

    @Test
    public void testDownloadPlanNotFound() {
        String planId = "test-update-plan-id";
        String outname = "never-gonna-be-used.txt";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.downloadPlan(planId, outname));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    public void testGetPlanList() throws IOException {
        String json = "[{\"name\":\"plan1\"},{\"name\":\"plan2\"}]";

        // Ser the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(json));
        requestHandler.setNextResponse(response);

        // Call repository method
        String planList = repository.getPlanList();

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans");
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpGet.METHOD_NAME);

        // Verify planList is the expected one
        assertThat(new JsonMatcher(planList).matches(json)).isTrue();
    }

    @Test
    public void testAppendActivityInstances() throws PlanNotFoundException, InvalidJsonException, InvalidPlanException, IOException {
        String planId = "test-append-activities";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.appendActivityInstances(planId, json);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans/%s", planId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpPost.METHOD_NAME);
        assertThat(new String(
                        ((HttpPost)request)
                                .getEntity()
                                .getContent()
                                .readAllBytes()
                )
        ).isEqualTo(json);
    }

    @Test
    public void testAppendActivityInstancesInvalidJson()  {
        String planId = "test-append-activities";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_BAD_REQUEST);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.appendActivityInstances(planId, json));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(InvalidJsonException.class);
    }

    @Test
    public void testAppendActivityInstancesInvalidPlan()  {
        String planId = "test-append-activities";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.appendActivityInstances(planId, json));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(InvalidPlanException.class);
    }

    @Test
    public void testAppendActivityInstancesNotFound()  {
        String planId = "test-append-activities";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.appendActivityInstances(planId, json));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    public void testGetActivityInstance() throws PlanNotFoundException, ActivityInstanceNotFoundException, IOException {
        String planId = "test-get-activity-instance";
        String activityId = "test-activity-id";
        String json = "{\"type\":\"test-activity\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(json));
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.getActivityInstance(planId, activityId);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans/%s/activity_instances/%s", planId, activityId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpGet.METHOD_NAME);
    }

    @Disabled("No way to distinguish PlanNotFound from ActivityInstanceNotFound yet")
    @Test
    public void testGetActivityInstancePlanNotFound() throws IOException {
        String planId = "test-get-activity-instance";
        String activityId = "test-activity-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
                requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.getActivityInstance(planId, activityId));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    public void testGetActivityInstanceActivityNotFound() throws IOException {
        String planId = "test-get-activity-instance";
        String activityId = "test-activity-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.getActivityInstance(planId, activityId));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(ActivityInstanceNotFoundException.class);
    }

    @Test
    public void testUpdateActivityInstance() throws PlanNotFoundException, ActivityInstanceNotFoundException, InvalidJsonException, InvalidActivityInstanceException, IOException {
        String planId = "test-update-activity-instance";
        String activityId = "test-activity-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NO_CONTENT);
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.updateActivityInstance(planId, activityId, json);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans/%s/activity_instances/%s", planId, activityId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpPatch.METHOD_NAME);
        assertThat(new String(
                        ((HttpPatch)request)
                                .getEntity()
                                .getContent()
                                .readAllBytes()
                )
        ).isEqualTo(json);
    }

    @Disabled("No way to distinguish PlanNotFound from ActivityInstanceNotFound yet")
    @Test
    public void testUpdateActivityInstancePlanNotFound()  {
        String planId = "test-update-activity-instance";
        String activityId = "test-activity-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.updateActivityInstance(planId, activityId, json));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    public void testUpdateActivityInstanceActivityNotFound()  {
        String planId = "test-update-activity-instance";
        String activityId = "test-activity-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.updateActivityInstance(planId, activityId, json));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(ActivityInstanceNotFoundException.class);
    }

    @Test
    public void testUpdateActivityInstanceInvalidJson()  {
        String planId = "test-update-activity-instance";
        String activityId = "test-activity-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_BAD_REQUEST);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.updateActivityInstance(planId, activityId, json));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(InvalidJsonException.class);
    }

    @Test
    public void testUpdateActivityInstanceInvalidActivityInstance()  {
        String planId = "test-update-activity-instance";
        String activityId = "test-activity-id";
        String json = "{\"key\":\"value\"}";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.updateActivityInstance(planId, activityId, json));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(InvalidActivityInstanceException.class);
    }

    @Test
    public void testDeleteActivityInstance() throws PlanNotFoundException, ActivityInstanceNotFoundException {
        String planId = "test-delete-plan-id";
        String activityId = "test-activity-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.deleteActivityInstance(planId, activityId);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans/%s/activity_instances/%s", planId, activityId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpDelete.METHOD_NAME);
    }

    @Disabled("No way to distinguish PlanNotFound from ActivityInstanceNotFound yet")
    @Test
    public void testDeleteActivityInstancePlanNotFound() {
        String planId = "test-delete-plan-id";
        String activityId = "test-activity-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.deleteActivityInstance(planId, activityId));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    public void testDeleteActivityInstanceActivityNotFound() {
        String planId = "test-delete-plan-id";
        String activityId = "test-activity-id";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.deleteActivityInstance(planId, activityId));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(ActivityInstanceNotFoundException.class);
    }

    @Test
    public void testRemoteSimulation() throws PlanNotFoundException, IOException {
        String planId = "test-simulate-plan-id";
        String json = "{\"name\": \"testremotesimulation\"}";
        String outname = "test23n5w4otjwsdno323.json";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_OK);
        response.setEntity(new StringEntity(json));
        requestHandler.setNextResponse(response);

        // Call repository method
        repository.getSimulationResults(planId, 3600000000L, outname);

        // Verify request contained expected information
        HttpUriRequest request = requestHandler.getLastRequest();

        String expectedURIPath = String.format("/plans/%s/results?sampling-period=3600000000", planId);
        assertThat(request.getURI().toString()).endsWith(expectedURIPath);
        assertThat(request.getMethod()).isEqualTo(HttpGet.METHOD_NAME);

        // Verify plan was written to expected file
        String downloadedContents = new String(Files.readAllBytes(Path.of(outname)));
        assertThat(new JsonMatcher(downloadedContents).matches(json)).isTrue();

        // Cleanup
        Files.deleteIfExists(Path.of(outname));
    }

    @Test
    public void testRemoteSimulationPlanNotFound() throws IOException {
        String planId = "test-simulate-plan-id";
        String outname = "test258474267492759y75353t3t.json";

        // Set the response for the handler
        HttpResponse response = createBasicHttpResponse(HttpStatus.SC_NOT_FOUND);
        requestHandler.setNextResponse(response);

        // Call repository method
        Throwable thrown = catchThrowable(() -> repository.getSimulationResults(planId, 3600000000L, outname));

        // Verify PlanNotFoundException was thrown
        assertThat(thrown).isInstanceOf(PlanNotFoundException.class);
    }
}
