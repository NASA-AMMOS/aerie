package gov.nasa.jpl.plan;

import gov.nasa.jpl.plan.models.*;
import gov.nasa.jpl.plan.repositories.PlansRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/plans")
public class PlansController {
    @Value("${adaptation-url}")
    private String adaptationUri;

    @Autowired
    private PlansRepository repository;

    //@RequestMapping(value = "", method = RequestMethod.GET)
    @GetMapping("")
    public ResponseEntity<Object> getPlans() {
        return ResponseEntity.ok(repository.findAll());
    }

    //@RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @GetMapping("/{id}")
    public ResponseEntity<Object> getPlanDetail(@PathVariable("id") ObjectId id) {
        return ResponseEntity.ok(repository.findPlanDetailBy_id(id));
    }

    //@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @PutMapping("/{id}")
    public ResponseEntity<Object> replacePlan(@PathVariable("id") ObjectId id,
            @Valid @RequestBody PlanDetail planDetail) {
        planDetail.set_id(id);
        repository.save(planDetail);
        return ResponseEntity.noContent().build();
    }

    //@RequestMapping(value = "/{id}", method = RequestMethod.PATCH)
    @PatchMapping("/{id}")
    public ResponseEntity<Object> updatePlan(@PathVariable("id") ObjectId id,
            @Valid @RequestBody PlanDetail planDetail) {
        repository.save(planDetail);
        return ResponseEntity.noContent().build();

    }

    //@RequestMapping(value = "", method = RequestMethod.POST)
    @PutMapping("")
    public ResponseEntity<Object> createPlan(@Valid @RequestBody Plan plan) {
        plan.set_id(ObjectId.get());
        repository.save(plan);
        return ResponseEntity.ok(plan);
    }

    //@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deletePlan(@PathVariable ObjectId id) {
        repository.delete(repository.findPlanBy_id(id));
        return ResponseEntity.noContent().build();
    }

    //@RequestMapping(value = "/{planId}/activity_instances", method =RequestMethod.POST)
    @PostMapping("/{planId}/activity_instances")
    public ResponseEntity<Object> createActivityInstance(@PathVariable(
            "planId") ObjectId planId,
            @RequestParam(value = "adaptationId", required = false) String adaptationId,
            @RequestParam(value = "activityTypeId", required = false) String activityTypeId,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {

        ActivityInstance activityInstance = new ActivityInstance();

        // Get activity type from adaptation service
        if (adaptationId != null && activityTypeId != null) {
            RestTemplate restTemplate = new RestTemplate();
            String uri = String.format("%s/%s/activities", adaptationUri,
                    adaptationId);

            ResponseEntity<HashMap<String, ActivityType>> response =
                    restTemplate.exchange(uri, HttpMethod.GET, null,
                            new ParameterizedTypeReference<HashMap<String,
                                    ActivityType>>() {
                            });

            HashMap<String, ActivityType> activityTypes =
                    response.getBody();

            if (activityTypes.containsKey(activityTypeId)) {
                ActivityType at = activityTypes.get(activityTypeId);

                activityInstance.setName(activityTypeId);
                activityInstance.setActivityType(at.getActivityClass());

                ArrayList<String> listeners = new ArrayList<>();
                for (String listener : at.getListeners()) {
                    listeners.add(listener);
                }
                activityInstance.setListeners(listeners);

                ArrayList<Parameter> parameters = new ArrayList<>();
                for (ActivityTypeParameter parameter : at.getParameters()) {
                    parameters.add(new Parameter(parameter.getType(),
                            parameter.getName()));
                }
                activityInstance.setParameters(parameters);
            }
        }

        UUID uuid = UUID.randomUUID();
        activityInstance.setActivityId(uuid.toString());

        PlanDetail planDetail = repository.findPlanDetailBy_id(planId);
        planDetail.addActivityInstance(activityInstance);
        planDetail.updateActivityInstance(uuid, requestBodyActivityInstance);
        repository.save(planDetail);

        return ResponseEntity.ok(activityInstance);
    }

    //@RequestMapping(value = "/{planId}/activity_instances", method = RequestMethod.GET)
    @GetMapping("/{planId}/activity_instances")
    public ResponseEntity<Object> getActivityInstances(@PathVariable("planId") ObjectId planId) {
        PlanDetail planDetail = repository.findPlanDetailBy_id(planId);
        if (planDetail != null) {
            return ResponseEntity.ok(planDetail.getActivityInstances());
        } else {
            return ResponseEntity.ok(new ArrayList<ActivityInstance>());
        }
    }

    //@RequestMapping(value = "/{planId}/activity_instances/{id}", method = RequestMethod.GET)
    @GetMapping("/{planId}/activity_instances/{id}")
    public ResponseEntity<Object> getActivityInstance(@PathVariable("planId") ObjectId planId,
            @PathVariable("id") UUID id) {
        PlanDetail planDetail = repository.findPlanDetailBy_id(planId);
        if (planDetail != null) {
            for (ActivityInstance ai : planDetail.getActivityInstances()) {
                if (ai.getActivityId().equals(id.toString())) {
                    return ResponseEntity.ok(ai);
                }
            }
        }

        return ResponseEntity.notFound().build();
    }

    //@RequestMapping(value = "/{planId}/activity_instances/{id}", method = RequestMethod.PUT)
    @PutMapping("/{planId}/activity_instances/{id}")
    public ResponseEntity<Object> replaceActivityInstance(@PathVariable(
            "planId") ObjectId planId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {

        return this.updateActivityInstance(planId, id, requestBodyActivityInstance);
    }

    //@RequestMapping(value = "/{planId}/activity_instances/{id}", method = RequestMethod.PATCH)
    @PatchMapping("/{planId}/activity_instances/{id}")
    public ResponseEntity<Object> updateActivityInstance(@PathVariable(
            "planId") ObjectId planId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {

        PlanDetail planDetail = repository.findPlanDetailBy_id(planId);
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

    //@RequestMapping(value = "/{planId}/activity_instances/{id}", method =RequestMethod.DELETE)
    @DeleteMapping("/{planId}/activity_instances/{id}")
    public ResponseEntity<Object> updateActivityInstance(@PathVariable(
            "planId") ObjectId planId,
            @PathVariable("id") UUID id) {

        PlanDetail planDetail = repository.findPlanDetailBy_id(planId);
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