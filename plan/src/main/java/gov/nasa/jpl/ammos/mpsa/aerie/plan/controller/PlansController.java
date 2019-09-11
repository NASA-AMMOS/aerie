package gov.nasa.jpl.ammos.mpsa.aerie.plan.controller;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.PlanValidator;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.PlanValidatorInterface;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.repositories.PlansRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.services.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstanceParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityTypeParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Validator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/plans")
public class PlansController {
    // Controller requestMapping
    private final String controllerLocation = this.getClass().getAnnotation(RequestMapping.class).value()[0];

    private final AdaptationService adaptationService;
    private final PlansRepository repository;
    private final PlanValidatorInterface planValidator;

    public PlansController(
        AdaptationService adaptationService,
        PlanValidatorInterface planValidator,
        PlansRepository plansRepository
    ) {
        this.adaptationService = adaptationService;
        this.planValidator = planValidator;
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

            // If no activity list was specified, make an empty list
            List<ActivityInstance> activityInstanceList = planDetail.getActivityInstances();
            if ( activityInstanceList == null ) {
                planDetail.setActivityInstances(new ArrayList<>());
            }

            // Generate IDs for the activity instances
            for (ActivityInstance act : planDetail.getActivityInstances()) {
                act.setActivityId(UUID.randomUUID().toString());
            }

            try {
                final List<String> validationErrors = Validator.findValidationFailures(planDetail);
                if (!validationErrors.isEmpty()) {
                    return ResponseEntity.unprocessableEntity().body(validationErrors);
                }
            } catch (IOException ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Plan semantic validation (against the adaptation)
            try {
                planValidator.validateActivitiesForPlan(planDetail);
            } catch (PlanValidator.ValidationException e) {
                return ResponseEntity.unprocessableEntity().body(e.getMessage());
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
        if (planDetail.getActivityInstances() != null) {
            for (ActivityInstance act : planDetail.getActivityInstances()) {
                act.setActivityId(UUID.randomUUID().toString());
            }
            existing.setActivityInstances(planDetail.getActivityInstances());
        }

        try {
            final List<String> validationErrors = Validator.findValidationFailures(existing);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.unprocessableEntity().body(validationErrors);
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Plan semantic validation (against the adaptation)
        try {
            planValidator.validateActivitiesForPlan(planDetail);
        } catch (PlanValidator.ValidationException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }

        repository.save(existing);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("")
    public ResponseEntity<Object> createPlan(@Valid @RequestBody PlanDetail plan) {
        plan.setId(UUID.randomUUID().toString());

        // If no activity list was specified, make an empty list
        List<ActivityInstance> activityInstanceList = plan.getActivityInstances();
        if ( activityInstanceList == null ) {
            plan.setActivityInstances(new ArrayList<>());
        }

        // Generate IDs for the activity instances
        for (ActivityInstance act : plan.getActivityInstances()) {
            act.setActivityId(UUID.randomUUID().toString());
        }

        // Plan syntax validation
        try {
            final List<String> validationErrors = Validator.findValidationFailures(plan);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.unprocessableEntity().body(validationErrors);
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Plan semantic validation (against the adaptation)
        try {
            planValidator.validateActivitiesForPlan(plan);
        } catch (PlanValidator.ValidationException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }

        URI location = null;
        try {
            location = new URI(String.format("%s/%s", controllerLocation, plan.getId()));
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        repository.save(plan);
        return ResponseEntity.created(location).body(plan);
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

    /**
     * Add a list of activity instances to a plan
     */
    @PostMapping("/{planId}/activity_instances")
    public ResponseEntity<Object> appendActivityInstances(
            @PathVariable("planId") UUID planId,
            @Valid @RequestBody List<ActivityInstance> requestActivityInstanceList) {

        // If the request is valid, we will return a list of IDs for the created instances
        List<String> createdIDs = new ArrayList<>();

        if (!repository.existsById(planId.toString())) {
            return ResponseEntity.notFound().build();
        }
        PlanDetail planDetail = repository.findPlanDetailById(planId.toString());
        String adaptationId = planDetail.getAdaptationId();

        // Get activity type from adaptation service
        if (adaptationId != null) {
            Map<String, ActivityType> activityTypes = adaptationService.getActivityTypes(adaptationId);

            for (ActivityInstance activityInstance : requestActivityInstanceList) {
                String activityType = activityInstance.getActivityType();
                if (activityTypes.containsKey(activityType)) {
                    ActivityType at = activityTypes.get(activityType);
                    List<ActivityInstanceParameter> requestParameters = activityInstance.getParameters();

                    // Build a list and map containing the instance parameters
                    // We use the map to replace default values, but the list is needed to
                    // assign them to the activity instance we are creating
                    List<ActivityInstanceParameter> parameterList = new ArrayList<>();
                    Map<String, ActivityInstanceParameter> actualParameters = new HashMap<>();
                    for (ActivityTypeParameter parameter : at.getParameters()) {
                        String parameterName = parameter.getName();

                        ActivityInstanceParameter instanceParameter = new ActivityInstanceParameter();
                        instanceParameter.setName(parameterName);
                        instanceParameter.setType(parameter.getType());
                        instanceParameter.setValue(parameter.getDefaultValue());

                        parameterList.add(instanceParameter);
                        actualParameters.put(parameterName, instanceParameter);
                    }

                    for (ActivityInstanceParameter requestParameter : requestParameters ) {

                        // If the parameter isn't in the list of type params, explode
                        if (!actualParameters.containsKey(requestParameter.getName())) {
                            return ResponseEntity.unprocessableEntity().build();
                        }

                        // Replace the default value with this parameter's value
                        actualParameters.get(requestParameter.getName()).setValue(requestParameter.getValue());
                    }

                    activityInstance.setParameters(parameterList);
                    String instanceID = UUID.randomUUID().toString();
                    activityInstance.setActivityId(instanceID);
                    createdIDs.add(instanceID);
                }
            }
        }

        // TODO: We should add the ability to add a list of activity instances and use that instead of a loop here
        for (ActivityInstance activityInstance : requestActivityInstanceList) {
            planDetail.addActivityInstance(activityInstance);
        }

        // Validate the new activities before adding them to the plan, by validating the entire plan
        try {
            final List<String> validationErrors = Validator.findValidationFailures(planDetail);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.unprocessableEntity().body(validationErrors);
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Plan semantic validation (against the adaptation)
        try {
            planValidator.validateActivitiesForPlan(planDetail);
        } catch (PlanValidator.ValidationException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }

        URI location = null;
        try {
            location = new URI(String.format("%s/%s/activity_instances", controllerLocation, planDetail.getId()));
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        repository.save(planDetail);
        return ResponseEntity.created(location).body(createdIDs);
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
            final List<String> validationErrors = Validator.findValidationFailures(requestBodyActivityInstance);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.unprocessableEntity().body(validationErrors);
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Plan semantic validation (against the adaptation)
        try {
            planValidator.validateActivitiesForPlan(planDetail);
        } catch (PlanValidator.ValidationException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
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
            final List<String> validationErrors = Validator.findValidationFailures(planDetail);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.unprocessableEntity().body(validationErrors);
            }
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Plan semantic validation (against the adaptation)
        try {
            planValidator.validateActivitiesForPlan(planDetail);
        } catch (PlanValidator.ValidationException e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
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
