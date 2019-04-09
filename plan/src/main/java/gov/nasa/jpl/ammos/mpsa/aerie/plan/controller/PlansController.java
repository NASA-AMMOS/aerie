package gov.nasa.jpl.ammos.mpsa.aerie.plan.controller;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.repositories.PlansRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstanceParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityTypeParameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@CrossOrigin
@RestController
@RequestMapping("/plans")
public class PlansController {

  @Value("${adaptation-url}")
  private String adaptationUri;

  @Autowired
  private PlansRepository repository;

  @GetMapping("")
  public ResponseEntity<Object> getPlans() {
    return ResponseEntity.ok(repository.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> getPlan(@PathVariable("id") ObjectId _id) {
    Plan plan = repository.findPlanBy_id(_id);
    return ResponseEntity.ok(plan);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Object> replacePlan(@PathVariable("id") String id, @Valid @RequestBody PlanDetail planDetail) {
    planDetail.setId(id);
    repository.save(planDetail);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Object> updatePlan(@PathVariable("id") String id, @Valid @RequestBody PlanDetail planDetail) {
    repository.save(planDetail);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/")
  public ResponseEntity<Object> createPlan(@Valid @RequestBody Plan plan) {
    plan.set_id(ObjectId.get());
    repository.save(plan);
    return ResponseEntity.ok(plan);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Object> deletePlan(@PathVariable("id") ObjectId _id) {
    repository.delete(repository.findPlanBy_id(_id));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{planId}/activity_instances")
  public ResponseEntity<Object> createActivityInstance(@PathVariable("planId") ObjectId _id,
      @RequestParam(value = "adaptationId", required = false) String adaptationId,
      @RequestParam(value = "activityTypeId", required = false) String activityTypeId,
      @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {
    ActivityInstance activityInstance = new ActivityInstance();

    // Get activity type from adaptation service
    if (adaptationId != null && activityTypeId != null) {
      RestTemplate restTemplate = new RestTemplate();
      String uri = String.format("%s/%s/activities", adaptationUri, adaptationId);

      ResponseEntity<HashMap<String, ActivityType>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
          new ParameterizedTypeReference<HashMap<String, ActivityType>>() {
          });

      HashMap<String, ActivityType> activityTypes = response.getBody();

      if (activityTypes.containsKey(activityTypeId)) {
        ActivityType at = activityTypes.get(activityTypeId);

        activityInstance.setName(activityTypeId);
        activityInstance.setActivityType(at.getActivityClass());

        ArrayList<String> listeners = new ArrayList<>(at.getListeners());
        activityInstance.setListeners(listeners);

        // TODO This is not doing what needs to be done. We need to get the value from
        // the request body
        ArrayList<ActivityInstanceParameter> parameters = new ArrayList<>();
        for (ActivityTypeParameter parameter : at.getParameters()) {
          String defaultValue = "";
          String name = parameter.getName();
          List<String> range = new ArrayList<String>();
          String type = parameter.getType();
          String value = "FIX ME";
          parameters.add(new ActivityInstanceParameter(defaultValue, type, range, name, value));
        }
        activityInstance.setParameters(parameters);
      }
    }

    UUID uuid = UUID.randomUUID();
    activityInstance.setActivityId(uuid.toString());

    PlanDetail planDetail = repository.findPlanDetailBy_id(_id);
    planDetail.addActivityInstance(activityInstance);
    planDetail.updateActivityInstance(uuid, requestBodyActivityInstance);
    repository.save(planDetail);

    return ResponseEntity.ok(activityInstance);
  }

  @GetMapping("/{planId}/activity_instances")
  public ResponseEntity<Object> getActivityInstances(@PathVariable("planId") ObjectId _id) {
    PlanDetail planDetail = repository.findPlanDetailBy_id(_id);
    if (planDetail != null) {
      return ResponseEntity.ok(planDetail.getActivityInstances());
    } else {
      return ResponseEntity.ok(new ArrayList<ActivityInstance>());
    }
  }

  @GetMapping("/{planId}/activity_instances/{id}")
  public ResponseEntity<Object> getActivityInstance(@PathVariable("planId") ObjectId _id, @PathVariable("id") UUID id) {
    PlanDetail planDetail = repository.findPlanDetailBy_id(_id);

    if (planDetail != null) {
      for (ActivityInstance ai : planDetail.getActivityInstances()) {
        if (ai.getActivityId().equals(id.toString())) {
          return ResponseEntity.ok(ai);
        }
      }
    }

    return ResponseEntity.notFound().build();
  }

  @PutMapping("/{planId}/activity_instances/{id}")
  public ResponseEntity<Object> replaceActivityInstance(@PathVariable("planId") ObjectId _id,
      @PathVariable("id") UUID id, @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {
    return this.updateActivityInstance(_id, id, requestBodyActivityInstance);
  }

  @PatchMapping("/{planId}/activity_instances/{id}")
  public ResponseEntity<Object> updateActivityInstance(@PathVariable("planId") ObjectId _id,
      @PathVariable("id") UUID id, @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {
    PlanDetail planDetail = repository.findPlanDetailBy_id(_id);

    if (planDetail != null) {
      ActivityInstance activityInstance = planDetail.getActivityInstance(id);
      if (activityInstance != null) {
        planDetail.updateActivityInstance(id, requestBodyActivityInstance);
        repository.save(planDetail);
        return ResponseEntity.noContent().build();
      }

    }

    return ResponseEntity.notFound().build();
  }

  @DeleteMapping("/{planId}/activity_instances/{id}")
  public ResponseEntity<Object> updateActivityInstance(@PathVariable("planId") ObjectId _id,
      @PathVariable("id") UUID id) {
    PlanDetail planDetail = repository.findPlanDetailBy_id(_id);

    if (planDetail != null) {
      try {
        planDetail.removeActivityInstance(id);
        repository.save(planDetail);
        return ResponseEntity.noContent().build();
      } catch (NoSuchElementException e) {
        return ResponseEntity.notFound().build();
      } catch (Exception e) {
        return ResponseEntity.status(500).build();
      }

    }

    return ResponseEntity.notFound().build();
  }
}
