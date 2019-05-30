package gov.nasa.jpl.ammos.mpsa.aerie.plan.controller;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.repositories.PlansRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstanceParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityTypeParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Validator;

import java.io.IOException;
import java.util.*;
import javax.validation.Valid;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * NOTE: ObjectId is used throughout to validate that a MongoDB formatted ID is passed
 */
@CrossOrigin
@RestController
@RequestMapping("/plans")
public class PlansController {

    @Value("${adaptation-url}")
    private String adaptationUri;

    //  @Autowired
    private PlansRepository repository;

    public PlansController(PlansRepository plansRepository) {
        this.repository = plansRepository;
    }

    @GetMapping("")
    public ResponseEntity<Object> getPlans() {
        return ResponseEntity.ok(repository.findAll().stream().map(plan -> Plan.fromDetail(plan)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getPlan(@PathVariable("id") ObjectId _id) {
        Optional<PlanDetail> plan = repository.findById(_id.toHexString());
        if (plan.isPresent()) {
            return ResponseEntity.ok(plan);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> replacePlan(
            @PathVariable("id") ObjectId _id, @Valid @RequestBody PlanDetail planDetail) {
        String id = _id.toHexString();
        if (repository.existsById(id)) {
            planDetail.setId(_id.toHexString());

            try {
                if (!Validator.validate(planDetail)) {
                    return ResponseEntity.unprocessableEntity().build();
                }
            } catch (IOException ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            repository.save(planDetail);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> updatePlan(
            @PathVariable("id") ObjectId _id,
            @Valid @RequestBody PlanDetail planDetail
    ) {
        String id = _id.toHexString();
        PlanDetail existing = repository.findPlanDetailById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        planDetail.setId(id);

        if (planDetail.getAdaptationId() != null) existing.setAdaptationId(planDetail.getAdaptationId());
        if (planDetail.getEndTimestamp() != null) existing.setEndTimestamp(planDetail.getEndTimestamp());
        if (planDetail.getName() != null) existing.setName(planDetail.getName());
        if (planDetail.getStartTimestamp() != null) existing.setStartTimestamp(planDetail.getStartTimestamp());
        if (planDetail.getActivityInstances() != null) existing.setActivityInstances(planDetail.getActivityInstances());

        try {
            if (!Validator.validate(existing)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        repository.save(existing);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/")
    public ResponseEntity<Object> createPlan(@Valid @RequestBody PlanDetail plan) {
        plan.setId(new ObjectId().toString());

        try {
            if (!Validator.validate(plan)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        repository.save(plan);
        return ResponseEntity.ok(plan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletePlan(@PathVariable("id") ObjectId _id) {
        String id = _id.toHexString();
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{planId}/activity_instances")
    public ResponseEntity<Object> createActivityInstance(
            @PathVariable("planId") ObjectId _id,
            @RequestParam(value = "adaptationId", required = false) String adaptationId,
            @RequestParam(value = "activityTypeId", required = false) String activityTypeId,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {
        ActivityInstance activityInstance = new ActivityInstance();

        // Get activity type from adaptation service
        if (adaptationId != null && activityTypeId != null) {
            RestTemplate restTemplate = new RestTemplate();
            String uri = String.format("%s/%s/activities", adaptationUri, adaptationId);

            ResponseEntity<HashMap<String, ActivityType>> response =
                    restTemplate.exchange(
                            uri,
                            HttpMethod.GET,
                            null,
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
        requestBodyActivityInstance.setActivityId(uuid.toString());

        PlanDetail planDetail = repository.findPlanDetailById(_id.toHexString());
        planDetail.addActivityInstance(activityInstance);
        planDetail.updateActivityInstance(uuid, requestBodyActivityInstance);

        try {
            if (!Validator.validate(planDetail)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        repository.save(planDetail);

        return ResponseEntity.ok(planDetail.getActivityInstance(uuid));
    }

    @GetMapping("/{planId}/activity_instances")
    public ResponseEntity<Object> getActivityInstances(@PathVariable("planId") ObjectId _id) {
        PlanDetail planDetail = repository.findPlanDetailById(_id.toHexString());
        if (planDetail != null) {
            return ResponseEntity.ok(planDetail.getActivityInstances());
        } else {
            return ResponseEntity.ok(new ArrayList<ActivityInstance>());
        }
    }

    @GetMapping("/{planId}/activity_instances/{id}")
    public ResponseEntity<Object> getActivityInstance(
            @PathVariable("planId") ObjectId _id, @PathVariable("id") UUID id) {
        PlanDetail planDetail = repository.findPlanDetailById(_id.toHexString());

        if (planDetail != null) {
            for (ActivityInstance ai : planDetail.getActivityInstances()) {
                if (ai.getActivityId().equals(id.toString())) {
                    return ResponseEntity.ok(ai);
                }
            }
        }

        return ResponseEntity.notFound().build();
    }


    /**
     * Replace an activity instance with a new one, keeping the original ID
     * <p>
     * NOTE: activityId is not required within the request body, it will be
     * added automatically using the ID passed in the request URL.
     *
     * @param planId                      ID of the plan
     * @param activityInstanceId          ID of the activity instance
     * @param requestBodyActivityInstance The replacement activity instance
     * @return An empty response body
     */
    @PutMapping("/{planId}/activity_instances/{id}")
    public ResponseEntity<Object> replaceActivityInstance(
            @PathVariable("planId") ObjectId planId,
            @PathVariable("id") UUID activityInstanceId,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {

        PlanDetail planDetail = repository.findPlanDetailById(planId.toHexString());
        if (planDetail == null) {
            return ResponseEntity.notFound().build();
        }

        ActivityInstance activityInstance = planDetail.getActivityInstance(activityInstanceId);
        if (activityInstance == null) {
            return ResponseEntity.notFound().build();
        }

        // Ensure that there is always an activityId, even if the user hasn't passed one
        requestBodyActivityInstance.setActivityId(activityInstance.getActivityId());

        try {
            if (!Validator.validate(requestBodyActivityInstance)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        planDetail.replaceActivityInstance(activityInstanceId, requestBodyActivityInstance);
        repository.save(planDetail);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{planId}/activity_instances/{id}")
    public ResponseEntity<Object> updateActivityInstance(
            @PathVariable("planId") ObjectId _id,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {
        PlanDetail planDetail = repository.findPlanDetailById(_id.toHexString());
        if (planDetail == null) {
          return ResponseEntity.notFound().build();
        }

        ActivityInstance activityInstance = planDetail.getActivityInstance(id);
        if (activityInstance == null) {
          return ResponseEntity.notFound().build();
        }

        planDetail.updateActivityInstance(id, requestBodyActivityInstance);

        try {
            if (!Validator.validate(planDetail)) {
                return ResponseEntity.unprocessableEntity().build();
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        repository.save(planDetail);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{planId}/activity_instances/{id}")
    public ResponseEntity<Object> deleteActivityInstance(
            @PathVariable("planId") ObjectId _id, @PathVariable("id") UUID id) {
        PlanDetail planDetail = repository.findPlanDetailById(_id.toHexString());

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
