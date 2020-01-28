package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JsonUtilities;
import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.AdaptationParsingException;
import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.DirectoryNotFoundException;
import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.PlanParsingException;
import gov.nasa.jpl.ammos.mpsa.apgen.model.Plan;
import gov.nasa.jpl.ammos.mpsa.apgen.parser.AdaptationParser;
import gov.nasa.jpl.ammos.mpsa.apgen.parser.ApfParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A proxy for Merlin commands to be dispatched to a running Aerie instance.
 *
 * Commands received by an AerieCommandReceiver will be dispatched
 * to an instance of Aerie on which Merlin has been deployed.
 */
class AerieCommandReceiver implements MerlinCommandReceiver {
    private final PlanRepository planRepository;
    private final AdaptationRepository adaptationRepository;

    public AerieCommandReceiver(final PlanRepository planRepository, final AdaptationRepository adaptationRepository) {
        this.planRepository = planRepository;
        this.adaptationRepository = adaptationRepository;
    }

    @Override
    public void createPlan(String path) {
        String planJson;
        try {
            planJson = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        String id;
        try {
            id = this.planRepository.createPlan(planJson);
        } catch (PlanRepository.InvalidPlanException | PlanRepository.InvalidJsonException e) {
            System.err.println(e);
            return;
        }

        System.out.println(String.format("CREATED: Plan successfully created at: %s.", id));
    }

    @Override
    public void updatePlanFromFile(String planId, String path) {
        String planUpdateJson;
        try {
            planUpdateJson = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        updatePlan(planId, planUpdateJson);
    }

    @Override
    public void updatePlanFromTokens(String planId, String[] tokens) {
        PlanDetail plan;
        try {
            plan = PlanDetail.fromTokens(tokens);
        } catch (InvalidTokenException e) {
            System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return;
        }

        String planUpdateJson = JsonUtilities.convertPlanToJson(plan);
        updatePlan(planId, planUpdateJson);
    }

    private void updatePlan(String planId, String planUpdateJson) {
        try {
            this.planRepository.updatePlan(planId, planUpdateJson);
        } catch (PlanRepository.InvalidPlanException | PlanRepository.PlanNotFoundException | PlanRepository.InvalidJsonException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Plan successfully updated.");
    }

    @Override
    public void deletePlan(String planId) {
        try {
            this.planRepository.deletePlan(planId);
        } catch (PlanRepository.PlanNotFoundException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Plan successfully deleted.");
    }

    @Override
    public void downloadPlan(String planId, String outName) {
        if (Files.exists(Path.of(outName))) {
            System.err.println(String.format("File %s already exists.", outName));
            return;
        }

        try {
            this.planRepository.downloadPlan(planId, outName);
        } catch (PlanRepository.PlanNotFoundException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Plan successfully downloaded.");
    }

    @Override
    public void appendActivityInstances(String planId, String path) {
        String instanceListJson;
        try {
            instanceListJson = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        try {
            this.planRepository.appendActivityInstances(planId, instanceListJson);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.InvalidJsonException | PlanRepository.InvalidPlanException e) {
            System.err.println(e);
            return;
        }

        System.out.println("CREATED: Activities successfully created.");
    }

    @Override
    public void displayActivityInstance(String planId, String activityId) {
        String activityInstanceJson;
        try {
            activityInstanceJson = this.planRepository.getActivityInstance(planId, activityId);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.ActivityInstanceNotFoundException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Activity retrieval successful.");
        System.out.println(activityInstanceJson);
    }

    @Override
    public void updateActivityInstance(String planId, String activityId, String[] tokens) {
        ActivityInstance activityInstance;
        try {
            activityInstance = ActivityInstance.fromTokens(tokens);
        } catch (InvalidTokenException e) {
            System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return;
        }

        String activityUpdateJson = JsonUtilities.convertActivityInstanceToJson(activityInstance);

        try {
            this.planRepository.updateActivityInstance(planId, activityId, activityUpdateJson);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.ActivityInstanceNotFoundException | PlanRepository.InvalidJsonException | PlanRepository.InvalidActivityInstanceException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Activity successfully updated.");
    }

    @Override
    public void deleteActivityInstance(String planId, String activityId) {
        try {
            this.planRepository.deleteActivityInstance(planId, activityId);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.ActivityInstanceNotFoundException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Activity successfully deleted.");
    }

    @Override
    public void listPlans() {
        String planListJson = this.planRepository.getPlanList();

        System.out.println("SUCCESS: Plan list retrieval successful.");
        System.out.println(planListJson);
    }

    @Override
    public void createAdaptation(String path, Adaptation adaptation) {
        File jarFile = new File(path);
        if (!jarFile.exists()) {
            System.err.println(String.format("File not found: %s", path));
            return;
        }

        String id;
        try {
            id = this.adaptationRepository.createAdaptation(adaptation, jarFile);
        } catch (AdaptationRepository.InvalidAdaptationException e) {
            System.err.println(e);
            return;
        }

        System.out.println(String.format("CREATED: Adaptation successfully created at: %s.", id));
    }

    @Override
    public void deleteAdaptation(String adaptationId) {
        try {
            this.adaptationRepository.deleteAdaptation(adaptationId);
        } catch (AdaptationRepository.AdaptationNotFoundException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Adaptation successfully deleted.");
    }

    @Override
    public void displayAdaptation(String adaptationId) {
        Adaptation adaptation;
        try {
            adaptation = this.adaptationRepository.getAdaptation(adaptationId);
        } catch (AdaptationRepository.AdaptationNotFoundException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Adaptation retrieval successful.");
        System.out.println(JsonUtilities.convertAdaptationToJson(adaptation));
    }

    @Override
    public void listAdaptations() {
        String adaptationListJson = this.adaptationRepository.getAdaptationList();

        System.out.println("SUCCESS: Adaptation list retrieval successful.");
        System.out.println(adaptationListJson);
    }

    @Override
    public void listActivityTypes(String adaptationId) {
        String activityTypeListJson;
        try {
            activityTypeListJson = this.adaptationRepository.getActivityTypes(adaptationId);
        } catch (AdaptationRepository.AdaptationNotFoundException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Activity type list retrieval successful.");
        System.out.println(activityTypeListJson);
    }

    @Override
    public void displayActivityType(String adaptationId, String activityType) {
        String activityTypeJson;
        try {
            activityTypeJson = this.adaptationRepository.getActivityType(adaptationId, activityType);
        } catch (AdaptationRepository.AdaptationNotFoundException | AdaptationRepository.ActivityTypeNotDefinedException e) {
            System.err.println(e);
            return;
        }

        System.out.println("SUCCESS: Activity type retrieval successful.");
        System.out.println(activityTypeJson);
    }

    @Override
    public void convertApfFile(String input, String output, String dir, String[] tokens) {

        /* Parse adaptation and plan file */
        Plan plan;
        try {
            gov.nasa.jpl.ammos.mpsa.apgen.model.Adaptation adaptation = AdaptationParser.parseDirectory(Path.of(dir));
            plan = ApfParser.parseFile(Path.of(input), adaptation);
        } catch (AdaptationParsingException | PlanParsingException e) {
            System.err.println(e.getMessage());
            return;
        } catch (DirectoryNotFoundException e) {
            System.err.println(String.format("Adaptation directory not found: %s", e.getMessage()));
            return;
        }

        /* Parse tokens for plan metadata */
        String adaptationId = null;
        String startTimestamp = null;
        String name = null;
        for (final String token : tokens) {
            final String[] pieces = token.split("=", 2);
            if (pieces.length != 2) { continue; }  // should really error at the user on this case

            switch (pieces[0]) {
                case "adaptationId": adaptationId = pieces[1]; break;
                case "startTimestamp": startTimestamp = pieces[1]; break;
                case "name": name = pieces[1]; break;
            }
        }

        /* Build the plan JSON and write it to the specified output file */
        if (JsonUtilities.writePlanToJSON(plan, Path.of(output), adaptationId, startTimestamp, name)) {
            System.out.println(String.format("SUCCESS: Plan file written to %s", output));
        }
    }
}
