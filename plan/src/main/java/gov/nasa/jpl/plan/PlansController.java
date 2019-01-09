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

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<Object> getPlans() {
        return ResponseEntity.ok(repository.findAll());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Object> getPlan(@PathVariable("id") ObjectId id) {
        return ResponseEntity.ok(repository.findBy_id(id));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Object> replacePlan(@PathVariable("id") ObjectId id,
            @Valid @RequestBody Plan plan) {
        plan.set_id(id);
        repository.save(plan);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<Object> updatePlan(@PathVariable("id") ObjectId id,
            @Valid @RequestBody Plan plan) {
        if (id.toHexString().equals(plan.get_id())) {
            repository.save(plan);
            return ResponseEntity.noContent().build();
        } else {
            // The id in the URL is different than the one in the plan
            return ResponseEntity.badRequest().build();
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<Object> createPlan(@Valid @RequestBody Plan plan) {
        plan.set_id(ObjectId.get());
        repository.save(plan);
        return ResponseEntity.ok(plan);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Object> deletePlan(@PathVariable ObjectId id) {
        repository.delete(repository.findBy_id(id));
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{planId}/activity_instances", method =
            RequestMethod.POST)
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
        activityInstance.setId(uuid.toString());

        Plan plan = repository.findBy_id(planId);
        plan.addActivityInstance(activityInstance);
        plan.updateActivityInstance(uuid, requestBodyActivityInstance);
        repository.save(plan);

        return ResponseEntity.ok(activityInstance);
    }

    @RequestMapping(value = "/{planId}/activity_instances", method =
            RequestMethod.GET)
    public ResponseEntity<Object> getActivityInstances(@PathVariable("planId") ObjectId planId) {
        Plan plan = repository.findBy_id(planId);
        if (plan != null) {
            return ResponseEntity.ok(plan.getActivityInstances());
        } else {
            return ResponseEntity.ok(new ArrayList<ActivityInstance>());
        }
    }

    @RequestMapping(value = "/{planId}/activity_instances/{id}", method =
            RequestMethod.GET)
    public ResponseEntity<Object> getActivityInstance(@PathVariable("planId") ObjectId planId,
            @PathVariable("id") UUID id) {
        Plan plan = repository.findBy_id(planId);
        if (plan != null) {
            for (ActivityInstance ai : plan.getActivityInstances()) {
                if (ai.getId().equals(id.toString())) {
                    return ResponseEntity.ok(ai);
                }
            }
        }

        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/{planId}/activity_instances/{id}", method =
            RequestMethod.PUT)
    public ResponseEntity<Object> replaceActivityInstance(@PathVariable(
            "planId") ObjectId planId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {

        return this.updateActivityInstance(planId, id, requestBodyActivityInstance);
    }

    @RequestMapping(value = "/{planId}/activity_instances/{id}", method =
            RequestMethod.PATCH)
    public ResponseEntity<Object> updateActivityInstance(@PathVariable(
            "planId") ObjectId planId,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ActivityInstance requestBodyActivityInstance) {

        Plan plan = repository.findBy_id(planId);
        if (plan != null) {
            ActivityInstance activityInstance = plan.getActivityInstance(id);
            if (activityInstance != null) {
                plan.updateActivityInstance(id, requestBodyActivityInstance);
                repository.save(plan);
                return ResponseEntity.noContent().build();
            }

        }

        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/{planId}/activity_instances/{id}", method =
            RequestMethod.DELETE)
    public ResponseEntity<Object> updateActivityInstance(@PathVariable(
            "planId") ObjectId planId,
            @PathVariable("id") UUID id) {

        Plan plan = repository.findBy_id(planId);
        if (plan != null) {
            try {
                plan.removeActivityInstance(id);
                repository.save(plan);
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