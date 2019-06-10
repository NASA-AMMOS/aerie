package gov.nasa.jpl.ammos.mpsa.aerie.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.repositories.PlansRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: Test invalid plan id for activity instance put, patch, and delete methods
// TODO: Test invalid activity instance id for activity instance put, patch, and delete methods
// TODO: Test invalid request body type (e.g. not an object)

/**
 * Test that the REST endpoints, repository, and database all work together
 *
 * <p>Annotate class with DirtiesContext to force the server context to reload after each test,
 * providing each test with a fresh database. Time intensive. A better alternative is to mark each
 * test that dirties the context so that only those tests reload the context.
 * https://www.javarticles.com/2016/03/spring-dirtiescontext-annotation-example.html
 * Example: @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = PlanApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PlansControllerIntegrationTest {

  @Autowired private PlansRepository plansRepository;

  @Autowired private TestRestTemplate restTemplate;

  // TestRestTemplate doesn't support PATCH, work around this with the Apache HttpClient
  // see https://rtmccormick.com/2017/07/30/solved-testing-patch-spring-boot-testresttemplate/
  private RestTemplate patchRestTemplate;

  @BeforeEach
  public void setup() {
    this.patchRestTemplate = restTemplate.getRestTemplate();
    HttpClient httpClient = HttpClientBuilder.create().build();
    this.patchRestTemplate.setRequestFactory(
        new HttpComponentsClientHttpRequestFactory(httpClient));
  }

  @Test
  @DirtiesContext
  public void shouldCreateANewPlan() throws IOException {
    Plan plan = generateRandomPlan();

    ResponseEntity<String> response = restTemplate.postForEntity("/plans/", plan, String.class);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = response.getBody();
    Plan responsePlan = objectMapper.readValue(body, Plan.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responsePlan.getName()).isEqualTo(plan.getName());
  }

  @Test
  public void shouldReturnA400WhenPostPlanBodyIsNotSpecified() {
    ResponseEntity<String> response = restTemplate.postForEntity("/plans/", null, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldReturnA422WhenPostPlanIsInvalid() {
    Plan plan = generateRandomPlan();
    plan.setName(null);

    ResponseEntity<String> response = restTemplate.postForEntity("/plans/", plan, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @Test
  @DirtiesContext
  public void shouldRetrieveAPlanById() throws IOException {

    Plan plan = postRandomPlan();

    ResponseEntity<String> response =
        restTemplate.getForEntity("/plans/" + plan.getId(), String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = response.getBody();
    Plan responsePlan = objectMapper.readValue(body, Plan.class);

    assertThat(responsePlan.getId()).isEqualTo(plan.getId());
  }

  @Test
  public void shouldReturn400WhenGetPlanByIdIsMalformed() {
    ResponseEntity<String> response = restTemplate.getForEntity("/plans/123456", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldReturn404WhenGetPlanByIdIsMalformed() {
    String id = "2482d296-8665-11e9-bc42-526af7764f64";
    ResponseEntity<String> response = restTemplate.getForEntity("/plans/" + id, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DirtiesContext
  public void shouldRetrieveAPlanList() throws IOException {

    Plan plan1 = postRandomPlan();
    Plan plan2 = postRandomPlan();

    ResponseEntity<String> response = restTemplate.getForEntity("/plans", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = response.getBody();
    Plan[] responsePlans = objectMapper.readValue(body, Plan[].class);

    assertThat(responsePlans.length).isEqualTo(2);
    assertThat(responsePlans[0].getId()).isEqualTo(plan1.getId());
    assertThat(responsePlans[1].getId()).isEqualTo(plan2.getId());
  }

  @Test
  @DirtiesContext
  public void shouldReplaceAPlan() throws IOException {
    Plan originalPlan = postRandomPlan();
    Plan replacementPlan = generateRandomPlan();
    replacementPlan.setId(originalPlan.getId());

    HttpEntity<Plan> responseEntity = new HttpEntity<>(replacementPlan, new HttpHeaders());
    ResponseEntity<String> putResponse =
        restTemplate.exchange(
            "/plans/" + originalPlan.getId(), HttpMethod.PUT, responseEntity, String.class);

    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Get the replaced Plan and verify that it has actually been replaced
    ResponseEntity<String> getResponse =
        restTemplate.getForEntity("/plans/" + originalPlan.getId(), String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = getResponse.getBody();
    PlanDetail responsePlan = objectMapper.readValue(body, PlanDetail.class);

    assertThat(responsePlan.getId()).isEqualTo(originalPlan.getId());
    assertThat(responsePlan.getName()).isEqualTo(replacementPlan.getName());
    assertThat(responsePlan.getEndTimestamp()).isEqualTo(replacementPlan.getEndTimestamp());
    assertThat(responsePlan.getStartTimestamp()).isEqualTo(replacementPlan.getStartTimestamp());
    assertThat(responsePlan.getAdaptationId()).isEqualTo(replacementPlan.getAdaptationId());
  }

  @Test
  public void shouldReturn400WhenPutPlanIdIsMalformed() {
    HttpEntity<Plan> responseEntity = new HttpEntity<>(new Plan(), new HttpHeaders());
    ResponseEntity<String> putResponse =
        restTemplate.exchange("/plans/123456", HttpMethod.PUT, responseEntity, String.class);
    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldReturnA400WhenPutPlanBodyIsNotSpecified() {
    String id = "2482d296-8665-11e9-bc42-526af7764f64";
    HttpEntity<Plan> responseEntity = new HttpEntity<>(null, new HttpHeaders());
    ResponseEntity<String> putResponse =
        restTemplate.exchange("/plans/" + id, HttpMethod.PUT, responseEntity, String.class);
    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldReturnA404WhenPutPlanIdDoesntExist() {
    String id = "2482d296-8665-11e9-bc42-526af7764f64";
    HttpEntity<Plan> responseEntity = new HttpEntity<>(new Plan(), new HttpHeaders());
    ResponseEntity<String> putResponse =
        restTemplate.exchange("/plans/" + id, HttpMethod.PUT, responseEntity, String.class);
    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  public void shouldReturnA405WhenPutPlanIdIsNotSpecified() {
    HttpEntity<Plan> responseEntity = new HttpEntity<>(null, new HttpHeaders());
    ResponseEntity<String> putResponse =
        restTemplate.exchange("/plans/", HttpMethod.PUT, responseEntity, String.class);
    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
  }

  @Test
  @DirtiesContext
  public void shouldReturnA422WhenPutPlanIsIncomplete() throws IOException {
    PlanDetail plan = generateRandomPlanDetail();

    // Upload an initial plan.
    {
      ResponseEntity<String> response = restTemplate.postForEntity("/plans/", plan, String.class);

      ObjectMapper objectMapper = new ObjectMapper();
      PlanDetail responsePlan = objectMapper.readValue(response.getBody(), PlanDetail.class);

      plan.setId(responsePlan.getId());
    }

    // Remove the name from this plan.
    plan.setName(null);

    // Attempt to update the plan.
    HttpEntity<PlanDetail> responseEntity = new HttpEntity<>(plan, new HttpHeaders());
    ResponseEntity<String> putResponse =
        restTemplate.exchange("/plans/" + plan.getId(), HttpMethod.PUT, responseEntity, String.class);

    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @Test
  @DirtiesContext
  public void shouldUpdateAPlan() throws IOException {
    final Plan originalPlan = postRandomPlan();

    final Plan planPatch = new Plan();
    planPatch.setName(originalPlan.getName() + "-patched");

    final HttpEntity<Plan> responseEntity = new HttpEntity<>(planPatch, new HttpHeaders());
    ResponseEntity<String> patchResponse =
        restTemplate.exchange(
            "/plans/" + originalPlan.getId(), HttpMethod.PATCH, responseEntity, String.class);

    assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Get the patched Plan and verify that it has actually been replaced
    final ResponseEntity<String> getResponse =
        restTemplate.getForEntity("/plans/" + originalPlan.getId(), String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    final ObjectMapper objectMapper = new ObjectMapper();
    final String body = getResponse.getBody();
    final PlanDetail responsePlan = objectMapper.readValue(body, PlanDetail.class);

    assertThat(responsePlan.getId()).isEqualTo(originalPlan.getId());
    assertThat(responsePlan.getName()).isEqualTo(planPatch.getName());
    assertThat(responsePlan.getEndTimestamp()).isEqualTo(originalPlan.getEndTimestamp());
    assertThat(responsePlan.getStartTimestamp()).isEqualTo(originalPlan.getStartTimestamp());
    assertThat(responsePlan.getAdaptationId()).isEqualTo(originalPlan.getAdaptationId());
  }

  @Test
  public void shouldReturn400WhenPatchPlanIdIsMalformed() {
    HttpEntity<Plan> responseEntity = new HttpEntity<>(new Plan(), new HttpHeaders());
    ResponseEntity<String> patchResponse =
        restTemplate.exchange("/plans/123456", HttpMethod.PATCH, responseEntity, String.class);
    assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldReturnA400WhenPatchPlanBodyIsNotSpecified() {
    String id = "2482d296-8665-11e9-bc42-526af7764f64";
    HttpEntity<Plan> responseEntity = new HttpEntity<>(null, new HttpHeaders());
    ResponseEntity<String> patchResponse =
        restTemplate.exchange("/plans/" + id, HttpMethod.PATCH, responseEntity, String.class);
    assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void shouldReturnA404WhenPatchPlanIdDoesntExist() {
    String id = "2482d296-8665-11e9-bc42-526af7764f64";
    HttpEntity<Plan> responseEntity = new HttpEntity<>(new Plan(), new HttpHeaders());
    ResponseEntity<String> patchResponse =
        restTemplate.exchange("/plans/" + id, HttpMethod.PATCH, responseEntity, String.class);
    assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  public void shouldReturnA405WhenPatchPlanBodyIsUndefined() {
    HttpEntity<Plan> responseEntity = new HttpEntity<>(null, new HttpHeaders());
    ResponseEntity<String> patchResponse =
        restTemplate.exchange("/plans/", HttpMethod.PATCH, responseEntity, String.class);
    assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
  }

  @Test
  @DirtiesContext
  public void shouldDeleteAPlan() throws IOException {
    Plan plan = postRandomPlan();

    HttpEntity responseEntity = new HttpEntity<>(null, new HttpHeaders());
    ResponseEntity deleteResponse =
        restTemplate.exchange(
            "/plans/" + plan.getId(), HttpMethod.DELETE, responseEntity, String.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Verify that the plan has been deleted
    ResponseEntity<String> response =
        restTemplate.getForEntity("/plans/" + plan.getId(), String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  public void shouldReturnA404WhenDeletePlanIdDoesntExist() {
    String id = "2482d296-8665-11e9-bc42-526af7764f64";
    HttpEntity responseEntity = new HttpEntity<>(null, new HttpHeaders());
    ResponseEntity deleteResponse =
        restTemplate.exchange("/plans/" + id, HttpMethod.DELETE, responseEntity, String.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DirtiesContext
  public void shouldCreateAnActivityInstance() throws IOException {
    // Setup: create a plan
    Plan originalPlan = postRandomPlan();

    ResponseEntity<String> response =
        restTemplate.getForEntity("/plans/" + originalPlan.getId(), String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = response.getBody();
    PlanDetail responsePlan = objectMapper.readValue(body, PlanDetail.class);

    assertThat(responsePlan.getActivityInstances().size()).isEqualTo(0);

    // Setup: Add activity instances to the plan
    ActivityInstance activityInstance = generateRandomActivityInstance();
    ResponseEntity<String> addActivityInstancesResponse =
        restTemplate.postForEntity(
            "/plans/" + originalPlan.getId() + "/activity_instances",
            activityInstance,
            String.class);

    assertThat(addActivityInstancesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper addActivityInstancesObjectMapper = new ObjectMapper();
    String addActivityInstancesBody = addActivityInstancesResponse.getBody();
    ActivityInstance addActivityInstancesPlan =
        addActivityInstancesObjectMapper.readValue(
            addActivityInstancesBody, ActivityInstance.class);

    // Fetch the Plan Detail which will have Activity Instances
    ResponseEntity<String> modifiedPlanResponse =
        restTemplate.getForEntity("/plans/" + originalPlan.getId(), String.class);
    assertThat(modifiedPlanResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper modifiedPlanResponseObjectMapper = new ObjectMapper();
    String modifiedPlanResponseBody = modifiedPlanResponse.getBody();
    PlanDetail modifiedPlan =
        modifiedPlanResponseObjectMapper.readValue(modifiedPlanResponseBody, PlanDetail.class);

    // Verify
    assertThat(modifiedPlan.getActivityInstances().size()).isEqualTo(1);
    assertThat(modifiedPlan.getActivityInstances().get(0).getActivityId()).isNotNull();
    assertThat(modifiedPlan.getActivityInstances().get(0).getActivityId())
        .isEqualTo(addActivityInstancesPlan.getActivityId());
  }

  @Test
  @DirtiesContext
  public void shouldCreateActivityInstanceWithFreshId() throws IOException {
    // Setup: create a plan
    Plan originalPlan = postRandomPlan();

    ResponseEntity<String> response =
        restTemplate.getForEntity("/plans/" + originalPlan.getId(), String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = response.getBody();
    PlanDetail responsePlan = objectMapper.readValue(body, PlanDetail.class);

    assertThat(responsePlan.getActivityInstances().size()).isEqualTo(0);

    // Setup: Add activity instances to the plan
    UUID initialIgnoredId = UUID.randomUUID();
    ActivityInstance activityInstance = generateRandomActivityInstance();
    activityInstance.setActivityId(initialIgnoredId.toString());
    ResponseEntity<String> addActivityInstancesResponse =
        restTemplate.postForEntity(
            "/plans/" + originalPlan.getId() + "/activity_instances",
            activityInstance,
            String.class);

    ObjectMapper addActivityInstancesObjectMapper = new ObjectMapper();
    String addActivityInstancesBody = addActivityInstancesResponse.getBody();
    ActivityInstance addedActivityInstance =
        addActivityInstancesObjectMapper.readValue(
            addActivityInstancesBody, ActivityInstance.class);

    assertThat(addedActivityInstance.getActivityId()).isNotEqualTo(initialIgnoredId.toString());
  }

  @Test
  @DirtiesContext
  public void shouldGetAnActivityInstanceList() throws IOException {
    Plan plan = postRandomPlan();
    ActivityInstance activityInstance1 = postRandomActivityInstance(plan.getId());
    ActivityInstance activityInstance2 = postRandomActivityInstance(plan.getId());

    String url = "/plans/" + plan.getId() + "/activity_instances/";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = response.getBody();
    ActivityInstance[] activityInstanceList =
        objectMapper.readValue(body, ActivityInstance[].class);

    assertThat(activityInstanceList.length).isEqualTo(2);
    assertThat(activityInstanceList[0].getActivityId())
        .isEqualTo(activityInstance1.getActivityId());
    assertThat(activityInstanceList[1].getActivityId())
        .isEqualTo(activityInstance2.getActivityId());
  }

  @Test
  @DirtiesContext
  public void shouldGetAnActivityInstance() throws IOException {
    Plan originalPlan = postRandomPlan();
    ActivityInstance originalActivityInstance = postRandomActivityInstance(originalPlan.getId());

    String url =
        "/plans/"
            + originalPlan.getId()
            + "/activity_instances/"
            + originalActivityInstance.getActivityId();

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = response.getBody();
    ActivityInstance responseActivityInstance =
        objectMapper.readValue(body, ActivityInstance.class);

    assertThat(responseActivityInstance.getActivityId())
        .isEqualTo(originalActivityInstance.getActivityId());
    assertThat(responseActivityInstance.getActivityType())
        .isEqualTo(originalActivityInstance.getActivityType());
    assertThat(responseActivityInstance.getBackgroundColor())
        .isEqualTo(originalActivityInstance.getBackgroundColor());
    assertThat(responseActivityInstance.getDuration())
        .isEqualTo(originalActivityInstance.getDuration());
    assertThat(responseActivityInstance.getEnd()).isEqualTo(originalActivityInstance.getEnd());
    assertThat(responseActivityInstance.getEndTimestamp())
        .isEqualTo(originalActivityInstance.getEndTimestamp());
    assertThat(responseActivityInstance.getIntent())
        .isEqualTo(originalActivityInstance.getIntent());
    assertThat(responseActivityInstance.getName()).isEqualTo(originalActivityInstance.getName());
    assertThat(responseActivityInstance.getStart()).isEqualTo(originalActivityInstance.getStart());
    assertThat(responseActivityInstance.getStartTimestamp())
        .isEqualTo(originalActivityInstance.getStartTimestamp());
    assertThat(responseActivityInstance.getTextColor())
        .isEqualTo(originalActivityInstance.getTextColor());
    assertThat(responseActivityInstance.getY()).isEqualTo(originalActivityInstance.getY());
  }

  @Test
  @DirtiesContext
  public void shouldReplaceAnActivityInstance() throws IOException {
    Plan plan = postRandomPlan();
    ActivityInstance original = postRandomActivityInstance(plan.getId());
    ActivityInstance replacement = generateRandomActivityInstance();
    replacement.setActivityId(original.getActivityId());

    HttpEntity<ActivityInstance> putResponseEntity =
        new HttpEntity<>(replacement, new HttpHeaders());
    String putUrl = "/plans/" + plan.getId() + "/activity_instances/" + original.getActivityId();
    ResponseEntity<String> putResponse =
        restTemplate.exchange(putUrl, HttpMethod.PUT, putResponseEntity, String.class);

    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    String url = "/plans/" + plan.getId() + "/activity_instances/" + original.getActivityId();

    ResponseEntity<String> getResponseEntity = restTemplate.getForEntity(url, String.class);
    assertThat(getResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = getResponseEntity.getBody();
    ActivityInstance replaced = objectMapper.readValue(body, ActivityInstance.class);

    assertThat(replaced.getActivityId()).isEqualTo(original.getActivityId());
    assertThat(replaced.getActivityType()).isEqualTo(replacement.getActivityType());
    assertThat(replaced.getBackgroundColor()).isEqualTo(replacement.getBackgroundColor());
    assertThat(replaced.getDuration()).isEqualTo(replacement.getDuration());
    assertThat(replaced.getEnd()).isEqualTo(replacement.getEnd());
    assertThat(replaced.getEndTimestamp()).isEqualTo(replacement.getEndTimestamp());
    assertThat(replaced.getIntent()).isEqualTo(replacement.getIntent());
    assertThat(replaced.getName()).isEqualTo(replacement.getName());
    assertThat(replaced.getStart()).isEqualTo(replacement.getStart());
    assertThat(replaced.getStartTimestamp()).isEqualTo(replacement.getStartTimestamp());
    assertThat(replaced.getTextColor()).isEqualTo(replacement.getTextColor());
    assertThat(replaced.getY()).isEqualTo(replacement.getY());
  }

  @Test
  @DirtiesContext
  public void shouldReplaceAnActivityInstanceWithFreshId() throws IOException {
    Plan plan = postRandomPlan();
    ActivityInstance original = postRandomActivityInstance(plan.getId());

    ActivityInstance replacement = generateRandomActivityInstance();
    replacement.setActivityId(original.getActivityId() + "-patched");

    HttpEntity<ActivityInstance> putResponseEntity =
        new HttpEntity<>(replacement, new HttpHeaders());
    String putUrl = "/plans/" + plan.getId() + "/activity_instances/" + original.getActivityId();
    ResponseEntity<String> putResponse =
        restTemplate.exchange(putUrl, HttpMethod.PUT, putResponseEntity, String.class);

    assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    String url = "/plans/" + plan.getId() + "/activity_instances/" + original.getActivityId();
    ResponseEntity<String> getResponseEntity = restTemplate.getForEntity(url, String.class);
    assertThat(getResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = getResponseEntity.getBody();
    ActivityInstance replaced = objectMapper.readValue(body, ActivityInstance.class);

    assertThat(replaced.getActivityId()).isEqualTo(original.getActivityId());
    assertThat(replaced.getActivityType()).isEqualTo(replacement.getActivityType());
  }

  @Test
  @DirtiesContext
  public void shouldUpdateAnActivityInstance() throws IOException {
    Plan plan = postRandomPlan();
    ActivityInstance original = postRandomActivityInstance(plan.getId());
    Map<String, String> replacement = new HashMap<>();
    replacement.put("name", generateRandomString(8));

    HttpEntity<Map<String, String>> putResponseEntity =
            new HttpEntity<>(replacement, new HttpHeaders());
    String patchUrl = "/plans/" + plan.getId() + "/activity_instances/" + original.getActivityId();
    ResponseEntity<String> patchResponse =
            restTemplate.exchange(patchUrl, HttpMethod.PATCH, putResponseEntity, String.class);

    assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    String url = "/plans/" + plan.getId() + "/activity_instances/" + original.getActivityId();
    ResponseEntity<String> getResponseEntity = restTemplate.getForEntity(url, String.class);
    assertThat(getResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = getResponseEntity.getBody();
    ActivityInstance updated = objectMapper.readValue(body, ActivityInstance.class);

    assertThat(updated.getActivityId()).isEqualTo(original.getActivityId());
    assertThat(updated.getName()).isEqualTo(replacement.get("name"));
    assertThat(updated.getIntent()).isEqualTo(original.getIntent());
  }

  @Test
  @DirtiesContext
  public void shouldDeleteAnActivityInstance() throws IOException {
    Plan plan = postRandomPlan();
    ActivityInstance original = postRandomActivityInstance(plan.getId());

    String deleteUrl = "/plans/" + plan.getId() + "/activity_instances/" + original.getActivityId();

    HttpEntity deleteResponseEntity = new HttpEntity<>(null, new HttpHeaders());
    ResponseEntity deleteResponse =
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, deleteResponseEntity, String.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<String> response = restTemplate.getForEntity(deleteUrl, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  public Plan postRandomPlan() throws IOException {

    Plan plan = generateRandomPlan();

    ResponseEntity<String> response = restTemplate.postForEntity("/plans/", plan, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper objectMapper = new ObjectMapper();
    String body = response.getBody();
    return objectMapper.readValue(body, Plan.class);
  }

  public ActivityInstance postRandomActivityInstance(String planId) throws IOException {
    ActivityInstance activityInstance = generateRandomActivityInstance();
    ResponseEntity<String> addActivityInstancesResponse =
        restTemplate.postForEntity(
            "/plans/" + planId + "/activity_instances", activityInstance, String.class);

    assertThat(addActivityInstancesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ObjectMapper addActivityInstancesObjectMapper = new ObjectMapper();
    String addActivityInstancesBody = addActivityInstancesResponse.getBody();
    return addActivityInstancesObjectMapper.readValue(
        addActivityInstancesBody, ActivityInstance.class);
  }

  public Plan generateRandomPlan() {
    return Plan.fromDetail(generateRandomPlanDetail());
  }

  public PlanDetail generateRandomPlanDetail() {
    final PlanDetail plan = new PlanDetail();
    plan.setAdaptationId(generateRandomString(8));
    plan.setEndTimestamp(generateRandomDate());
    plan.setName(generateRandomString(8));
    plan.setStartTimestamp(generateRandomDate());
    return plan;
  }

  public ActivityInstance generateRandomActivityInstance() {
    ActivityInstance activityInstance = new ActivityInstance();
    activityInstance.setActivityType(generateRandomString(8));
    activityInstance.setBackgroundColor("#fff");
    activityInstance.setDuration(1.00);
    activityInstance.setEnd(1.00);
    activityInstance.setEndTimestamp(generateRandomDate());
    activityInstance.setIntent(generateRandomString(12));
    activityInstance.setName(generateRandomString(8));
    activityInstance.setStart(0.00);
    activityInstance.setStartTimestamp(generateRandomDate());
    activityInstance.setTextColor("#000");
    activityInstance.setY(2.15);
    return activityInstance;
  }

  public String generateRandomString(int length) {
    //        byte[] array = new byte[length]; // length is bounded by 7
    //        new Random().nextBytes(array);
    //        return new String(array, Charset.forName("UTF-8"));
    return RandomStringUtils.randomAlphabetic(length);
  }

  public String generateRandomDate() {
    LocalDate now = LocalDate.now();
    LocalDate randomDaysAgo = now.minusDays(new Random().nextInt());
    return randomDaysAgo.toString();
  }
}
