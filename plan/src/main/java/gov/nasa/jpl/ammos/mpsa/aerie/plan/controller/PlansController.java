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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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

    @GetMapping("/{planId}")
    public ResponseEntity<Object> getPlan(@PathVariable("planId") UUID planId) {
        Optional<PlanDetail> plan = repository.findById(planId.toString());
        if (plan.isPresent()) {
            return ResponseEntity.ok(plan);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{planId}")
    public ResponseEntity<Object> replacePlan(
            @PathVariable("planId") UUID planId, @Valid @RequestBody PlanDetail planDetail) {
        if (repository.existsById(planId.toString())) {
            planDetail.setId(planId.toString());

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

    @PatchMapping("/{planId}")
    public ResponseEntity<Object> updatePlan(
            @PathVariable("planId") UUID planId,
            @Valid @RequestBody PlanDetail planDetail
    ) {
        PlanDetail existing = repository.findPlanDetailById(planId.toString());
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        planDetail.setId(planId.toString());

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
        plan.setId(UUID.randomUUID().toString());

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

    @DeleteMapping("/{planId}")
    public ResponseEntity<Object> deletePlan(@PathVariable("planId") UUID planId) {
        if (repository.existsById(planId.toString())) {
            repository.deleteById(planId.toString());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{planId}/activity_instances")
    public ResponseEntity<Object> createActivityInstance(
            @PathVariable("planId") UUID planId,
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

        PlanDetail planDetail = repository.findPlanDetailById(planId.toString());
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
    public ResponseEntity<Object> getActivityInstances(@PathVariable("planId") UUID planId) {
        PlanDetail planDetail = repository.findPlanDetailById(planId.toString());
        if (planDetail != null) {
            return ResponseEntity.ok(planDetail.getActivityInstances());
        } else {
            return ResponseEntity.ok(new ArrayList<ActivityInstance>());
        }
    }

    @GetMapping("/{planId}/activity_instances/{activityId}")
    public ResponseEntity<Object> getActivityInstance(
            @PathVariable("planId") UUID planId, @PathVariable("activityId") UUID activityId) {
        PlanDetail planDetail = repository.findPlanDetailById(planId.toString());

        if (planDetail != null) {
            for (ActivityInstance ai : planDetail.getActivityInstances()) {
                if (ai.getActivityId().equals(activityId.toString())) {
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
    @PutMapping("/{planId}/activity_instances/{activityId}")
    public ResponseEntity<Object> replaceActivityInstance(
            @PathVariable("planId") UUID planId,
            @PathVariable("activityId") UUID activityInstanceId,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {

        PlanDetail planDetail = repository.findPlanDetailById(planId.toString());
        if (planDetail == null) {
            return ResponseEntity.notFound().build();
        }

        ActivityInstance activityInstance = planDetail.getActivityInstance(activityInstanceId);
        if (activityInstance == null) {
            return ResponseEntity.notFound().build();
        }

        // Ensure that there is always an activityId, and ignore a user-provided id.
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

    @PatchMapping("/{planId}/activity_instances/{activityId}")
    public ResponseEntity<Object> updateActivityInstance(
            @PathVariable("planId") UUID planId,
            @PathVariable("activityId") UUID activityId,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {
        PlanDetail planDetail = repository.findPlanDetailById(planId.toString());
        if (planDetail == null) {
          return ResponseEntity.notFound().build();
        }

        ActivityInstance activityInstance = planDetail.getActivityInstance(activityId);
        if (activityInstance == null) {
          return ResponseEntity.notFound().build();
        }

        // Ensure that there is always an activityId, and ignore a user-provided id.
        requestBodyActivityInstance.setActivityId(activityInstance.getActivityId());
        planDetail.updateActivityInstance(activityId, requestBodyActivityInstance);

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

    @DeleteMapping("/{planId}/activity_instances/{activityId}")
    public ResponseEntity<Object> deleteActivityInstance(
            @PathVariable("planId") UUID planId, @PathVariable("activityId") UUID activityId) {
        PlanDetail planDetail = repository.findPlanDetailById(planId.toString());

        if (planDetail != null) {
            try {
                planDetail.removeActivityInstance(activityId);
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
