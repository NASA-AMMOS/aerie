package gov.nasa.jpl.aerie.services.cli;

import gov.nasa.jpl.aerie.services.cli.exceptions.InvalidTokenException;
import gov.nasa.jpl.aerie.services.cli.models.ActivityInstance;
import gov.nasa.jpl.aerie.services.cli.models.Adaptation;
import gov.nasa.jpl.aerie.services.cli.models.AdaptationRepository;
import gov.nasa.jpl.aerie.services.cli.models.PlanDetail;
import gov.nasa.jpl.aerie.services.cli.models.PlanRepository;
import gov.nasa.jpl.aerie.services.cli.utils.JsonUtilities;
import gov.nasa.jpl.aerie.apgen.exceptions.AdaptationParsingException;
import gov.nasa.jpl.aerie.apgen.exceptions.DirectoryNotFoundException;
import gov.nasa.jpl.aerie.apgen.exceptions.PlanParsingException;
import gov.nasa.jpl.aerie.apgen.model.Plan;
import gov.nasa.jpl.aerie.apgen.parser.AdaptationParser;
import gov.nasa.jpl.aerie.apgen.parser.ApfParser;

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
            System.out.println(String.format("Plan creation failed: %s", e.getMessage()));
            return;
        }

        String id;
        try {
            id = this.planRepository.createPlan(planJson);
        } catch (PlanRepository.InvalidPlanException | PlanRepository.InvalidJsonException e) {
            System.out.println(String.format("Plan creation failed: %s", e.getMessage()));
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
            System.out.println(String.format("Plan update failed: %s", e.getMessage()));
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
            System.out.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return;
        }

        String planUpdateJson = JsonUtilities.convertPlanToJson(plan);
        updatePlan(planId, planUpdateJson);
    }

    private void updatePlan(String planId, String planUpdateJson) {
        try {
            this.planRepository.updatePlan(planId, planUpdateJson);
        } catch (PlanRepository.InvalidPlanException | PlanRepository.PlanNotFoundException | PlanRepository.InvalidJsonException e) {
            System.out.println(String.format("Plan update failed: %s", e.getMessage()));
            return;
        }

        System.out.println("SUCCESS: Plan successfully updated.");
    }

    @Override
    public void deletePlan(String planId) {
        try {
            this.planRepository.deletePlan(planId);
        } catch (PlanRepository.PlanNotFoundException e) {
            System.out.println(String.format("Plan delete failed: %s", e.getMessage()));
            return;
        }

        System.out.println("SUCCESS: Plan successfully deleted.");
    }

    @Override
    public void downloadPlan(String planId, String outName) {
        if (Files.exists(Path.of(outName))) {
            System.out.println(String.format("File %s already exists.", outName));
            return;
        }

        try {
            this.planRepository.downloadPlan(planId, outName);
        } catch (PlanRepository.PlanNotFoundException e) {
            System.out.println(String.format("Plan download failed: %s", e.getMessage()));
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
            System.out.println(String.format("Add activity instance failed: %s", e.getMessage()));
            return;
        }

        try {
            this.planRepository.appendActivityInstances(planId, instanceListJson);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.InvalidJsonException | PlanRepository.InvalidPlanException e) {
            System.out.println(String.format("Add activity instance failed: %s", e.getMessage()));
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
            System.out.println(String.format("Display activity instance failed: %s", e.getMessage()));
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
            System.out.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return;
        }

        String activityUpdateJson = JsonUtilities.convertActivityInstanceToJson(activityInstance);

        try {
            this.planRepository.updateActivityInstance(planId, activityId, activityUpdateJson);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.ActivityInstanceNotFoundException | PlanRepository.InvalidJsonException | PlanRepository.InvalidActivityInstanceException e) {
            System.out.println(String.format("Update activity instance failed: %s", e.getMessage()));
            return;
        }

        System.out.println("SUCCESS: Activity successfully updated.");
    }

    @Override
    public void deleteActivityInstance(String planId, String activityId) {
        try {
            this.planRepository.deleteActivityInstance(planId, activityId);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.ActivityInstanceNotFoundException e) {
            System.out.println(String.format("Delete activity instance failed: %s", e.getMessage()));
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
    public void performSimulation(String planId, String outName) {
        try {
            System.out.println("Requesting simulation results...");
            this.planRepository.getSimulationResults(planId, outName);
        } catch (PlanRepository.PlanNotFoundException e) {
            System.out.println(String.format("Plan simulation results could not be retrieved: %s", e.getMessage()));
            return;
        }
        System.out.println(String.format("SUCCESS: Simulation of plan completed. Results written to %s", outName));
    }

    @Override
    public String createAdaptation(Path path, Adaptation adaptation) {
        if (!Files.exists(path)) {
            System.out.println(String.format("File not found: %s", path));
            return null;
        }

        String id;
        try {
            id = this.adaptationRepository.createAdaptation(adaptation, path.toFile());
        } catch (AdaptationRepository.InvalidAdaptationException e) {
            System.out.println(String.format("Create adaptation failed: %s", e.getMessage()));
            return null;
        }

        System.out.println(String.format("CREATED: Adaptation successfully created at: %s.", id));
        return id;
    }

    @Override
    public void deleteAdaptation(String adaptationId) {
        try {
            this.adaptationRepository.deleteAdaptation(adaptationId);
        } catch (AdaptationRepository.AdaptationNotFoundException e) {
            System.out.println(String.format("Delete plan failed: %s", e.getMessage()));
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
            System.out.println(String.format("Display adaptation failed: %s", e.getMessage()));
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
            System.out.println(String.format("List activity types failed: %s", e.getMessage()));
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
            System.out.println(String.format("Display activity type failed: %s", e.getMessage()));
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
            gov.nasa.jpl.aerie.apgen.model.Adaptation adaptation = AdaptationParser.parseDirectory(Path.of(dir));
            plan = ApfParser.parseFile(Path.of(input), adaptation);
        } catch (AdaptationParsingException | PlanParsingException e) {
            System.out.println(String.format("Conversion of apf file failed: %s", e.getMessage()));
            return;
        } catch (DirectoryNotFoundException e) {
            System.out.println(String.format("Conversion of apf file failed: adaptation directory %s not found", e.getMessage()));
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
