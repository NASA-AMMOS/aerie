package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidNumberOfArgsException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JsonUtilities;
import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.AdaptationParsingException;
import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.DirectoryNotFoundException;
import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.PlanParsingException;
import gov.nasa.jpl.ammos.mpsa.apgen.model.Plan;
import gov.nasa.jpl.ammos.mpsa.apgen.parser.AdaptationParser;
import gov.nasa.jpl.ammos.mpsa.apgen.parser.ApfParser;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CommandOptions {
    private PlanRepository planRepository;
    private AdaptationRepository adaptationRepository;
    private Options options = new Options();
    private OptionGroup requiredGroup = new OptionGroup();
    private OptionGroup planIdRequiredGroup = new OptionGroup();
    private OptionGroup adaptationIdRequiredGroup = new OptionGroup();
    private boolean lastCommandStatus;

    public CommandOptions(PlanRepository planRepository, AdaptationRepository adaptationRepository) {
        buildArguments();
        this.planRepository = planRepository;
        this.adaptationRepository = adaptationRepository;
    }

    public void buildArguments() {
        // Add option to specify plan ID
        requiredGroup.addOption(new Option("p", "plan-id", true, "Specify the plan ID to use"));

        // Add option to specify adaptation ID
        requiredGroup.addOption(new Option("a", "adaptation-id", true, "Specify the adaptation ID to use"));

        // Add options to view list of adaptations and create adaptations
        requiredGroup.addOption(new Option("adaptations", "list-adaptations", false, "View a list of available adaptations"));
        Option opt = new Option("A", "create-adaptation", true, "Add a new adaptation, passing the name of an Adaptation JAR");
        opt.setArgs(Option.UNLIMITED_VALUES);
        requiredGroup.addOption(opt);

        // Add options to view list of plans and create plans
        requiredGroup.addOption(new Option("plans", "list-plans", false, "View a list of available plans"));
        requiredGroup.addOption(new Option("P", "create-plan", true, "Add a new plan passing the name of a PlanDetail JSON"));

        // Add option to convert apf
        Option apfOpt = new Option("c", "convert-apf", true, "Convert an apf file to JSON: <infile> <outfile> <dir> <tokens>");
        apfOpt.setArgs(Option.UNLIMITED_VALUES);
        requiredGroup.addOption(apfOpt);

        // Set the request type group as required
        requiredGroup.setRequired(true);

        // TODO: Figure out how to resolve the names for arguments so they make sense without being obnoxious
        // Being in a group makes options mutually exclusive
        planIdRequiredGroup.addOption(new Option("D", "delete-plan", false, "Delete a plan"));
        planIdRequiredGroup.addOption(new Option("U", "update-plan-from-file", true, "Update plan based on values in plan file"));
        planIdRequiredGroup.addOption(new Option(null, "append-activities", true, "Append new activity instances to a plan from a json"));
        planIdRequiredGroup.addOption(new Option("pull", "download-plan", true, "Download a plan into a file"));
        planIdRequiredGroup.addOption(new Option(null, "display-activity", true, "Display an activity from a plan"));
        planIdRequiredGroup.addOption(new Option(null, "delete-activity", true, "Delete an activity from a plan"));

        // Options that take more than one arg must be made separately
        opt = new Option(null, "update-plan", true, "Update the plan metadata");
        opt.setArgs(Option.UNLIMITED_VALUES);
        planIdRequiredGroup.addOption(opt);

        opt = new Option(null, "update-activity", true, "Update an activity from a plan");
        opt.setArgs(Option.UNLIMITED_VALUES);
        planIdRequiredGroup.addOption(opt);

        options.addOptionGroup(planIdRequiredGroup);

        adaptationIdRequiredGroup.addOption(new Option(null, "delete-adaptation", false, "Delete an adaptation"));
        adaptationIdRequiredGroup.addOption(new Option("display", "view-adaptation", false, "View an adaptation's metadata"));
        adaptationIdRequiredGroup.addOption(new Option("activities", "activity-types", false, "View an adaptation's activity types"));
        adaptationIdRequiredGroup.addOption(new Option("activity", "activity-type", true, "View an activity type from the specified adaptation"));

        options.addOptionGroup(adaptationIdRequiredGroup);
        options.addOptionGroup(requiredGroup);
    }

    public void parse(final String[] args) {

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                printUsage();
                lastCommandStatus = true;
                return;
            }

            // TODO: Eventually, we should probably check that other options aren't specified, or at least point out
            //       that we ignore them if they are
            if (requiredGroup.getSelected().equals("plans")) {
                lastCommandStatus = listPlans();
                return;
            }
            else if (requiredGroup.getSelected().equals("adaptations")) {
                lastCommandStatus = listAdaptations();
                return;
            }
            else if (requiredGroup.getSelected().equals("P")) {
                String path = cmd.getOptionValue("P");
                lastCommandStatus = createPlan(path);
                return;
            }
            else if (requiredGroup.getSelected().equals("A")) {
                String[] params = cmd.getOptionValues("A");
                if (params.length < 3) {
                    throw new InvalidNumberOfArgsException("Option 'A' requires at least three arguments");
                }
                String path = params[0];
                String[] tokens = Arrays.copyOfRange(args, 1, args.length);
                lastCommandStatus = createAdaptation(path, tokens);
                return;
            }
            else if (requiredGroup.getSelected().equals("p")) {
                String planId = cmd.getOptionValue("p");

                if (cmd.hasOption("U")) {
                    String path = cmd.getOptionValue("U");
                    lastCommandStatus = updatePlanFromFile(planId, path);
                    return;
                }
                else if (cmd.hasOption("update-plan")) {
                    String[] tokens = cmd.getOptionValues("update-plan");
                    lastCommandStatus = updatePlanFromTokens(planId, tokens);
                    return;
                }
                else if (cmd.hasOption("delete-plan")) {
                    lastCommandStatus = deletePlan(planId);
                    return;
                }
                else if (cmd.hasOption("pull")) {
                    String outName = cmd.getOptionValue("pull");
                    lastCommandStatus = downloadPlan(planId, outName);
                    return;
                }
                else if (cmd.hasOption("append-activities")) {
                    String path = cmd.getOptionValue("append-activities");
                    lastCommandStatus = appendActivityInstances(planId, path);
                    return;
                }
                else if (cmd.hasOption("display-activity")) {
                    String activityId = cmd.getOptionValue("display-activity");
                    lastCommandStatus = displayActivityInstance(planId, activityId);
                    return;
                }
                else if (cmd.hasOption("update-activity")) {
                    String[] params = cmd.getOptionValues("update-activity");
                    String activityId = params[0];
                    String[] tokens = Arrays.copyOfRange(params, 1, params.length);
                    lastCommandStatus = updateActivityInstance(planId, activityId, tokens);
                    return;
                }
                else if (cmd.hasOption("delete-activity")) {
                    String activityId = cmd.getOptionValue("delete-activity");
                    lastCommandStatus = deleteActivityInstance(planId, activityId);
                    return;
                }
            }
            else if (requiredGroup.getSelected().equals("a")) {
                String adaptationId = cmd.getOptionValue("a");
                if (cmd.hasOption("delete-adaptation")) {
                    lastCommandStatus = deleteAdaptation(adaptationId);
                    return;
                }
                else if (cmd.hasOption("display")) {
                    lastCommandStatus = displayAdaptation(adaptationId);
                    return;
                }
                else if (cmd.hasOption("activities")) {
                    lastCommandStatus = listActivityTypes(adaptationId);
                    return;
                }
                else if (cmd.hasOption("activity")) {
                    String activityId = cmd.getOptionValue("activity");
                    lastCommandStatus = displayActivityType(adaptationId, activityId);
                    return;
                }
            }
            else if (requiredGroup.getSelected().equals("c")) {
                String[] params = cmd.getOptionValues("c");
                if (params.length < 3) {
                    throw new InvalidNumberOfArgsException("Option 'apf' requires three arguments <infile> <outfile> <dir> <tokens>");
                }
                String[] tokens = Arrays.copyOfRange(params, 3, params.length);
                lastCommandStatus = convertApfFile(params[0], params[1], params[2], tokens);
                return;
            }
            else {
                System.out.println("No required argument specified.");
            }
        }
        catch (ParseException | InvalidNumberOfArgsException e) {
            System.err.println("Failed to parse command line properties: " + e);
        }
        lastCommandStatus = false;
        printUsage();
    }

    public boolean lastCommandSuccessful() {
        return this.lastCommandStatus;
    }

    private void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Merlin Adaptation", options);
    }

    private boolean createPlan(String path) {
        String planJson;
        try {
            planJson = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }

        String id;
        try {
            id = this.planRepository.createPlan(planJson);
        } catch (PlanRepository.InvalidPlanException | PlanRepository.InvalidJsonException e) {
            System.err.println(e);
            return false;
        }

        System.out.println(String.format("CREATED: Plan successfully created at: %s.", id));
        return true;
    }

    private boolean updatePlanFromFile(String planId, String path) {
        String planUpdateJson;
        try {
            planUpdateJson = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }

        return updatePlan(planId, planUpdateJson);
    }

    private boolean updatePlanFromTokens(String planId, String[] tokens) {
        PlanDetail plan;
        try {
            plan = PlanDetail.fromTokens(tokens);
        } catch (InvalidTokenException e) {
            System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return false;
        }

        String planUpdateJson = JsonUtilities.convertPlanToJson(plan);
        return updatePlan(planId, planUpdateJson);
    }

    public boolean updatePlan(String planId, String planUpdateJson) {
        try {
            this.planRepository.updatePlan(planId, planUpdateJson);
        } catch (PlanRepository.InvalidPlanException | PlanRepository.PlanNotFoundException | PlanRepository.InvalidJsonException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Plan successfully updated.");
        return true;
    }

    private boolean deletePlan(String planId) {
        try {
            this.planRepository.deletePlan(planId);
        } catch (PlanRepository.PlanNotFoundException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Plan successfully deleted.");
        return true;
    }

    private boolean downloadPlan(String planId, String outName) {
        if (Files.exists(Path.of(outName))) {
            System.err.println(String.format("File %s already exists.", outName));
            return false;
        }

        try {
            this.planRepository.downloadPlan(planId, outName);
        } catch (PlanRepository.PlanNotFoundException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Plan successfully downloaded.");
        return true;
    }

    private boolean appendActivityInstances(String planId, String path) {
        String instanceListJson;
        try {
            instanceListJson = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
        } catch (IOException e) {
            System.err.println(e);
            return false;
        }

        try {
            this.planRepository.appendActivityInstances(planId, instanceListJson);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.InvalidJsonException | PlanRepository.InvalidPlanException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("CREATED: Activities successfully created.");
        return true;
    }

    private boolean displayActivityInstance(String planId, String activityId) {
        String activityInstanceJson;
        try {
            activityInstanceJson = this.planRepository.getActivityInstance(planId, activityId);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.ActivityInstanceNotFoundException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Activity retrieval successful.");
        System.out.println(activityInstanceJson);
        return true;
    }

    private boolean updateActivityInstance(String planId, String activityId, String[] tokens) {
        ActivityInstance activityInstance;
        try {
            activityInstance = ActivityInstance.fromTokens(tokens);
        } catch (InvalidTokenException e) {
            System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return false;
        }

        String activityUpdateJson = JsonUtilities.convertActivityInstanceToJson(activityInstance);

        try {
            this.planRepository.updateActivityInstance(planId, activityId, activityUpdateJson);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.ActivityInstanceNotFoundException | PlanRepository.InvalidJsonException | PlanRepository.InvalidActivityInstanceException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Activity successfully updated.");
        return true;
    }

    private boolean deleteActivityInstance(String planId, String activityId) {
        try {
            this.planRepository.deleteActivityInstance(planId, activityId);
        } catch (PlanRepository.PlanNotFoundException | PlanRepository.ActivityInstanceNotFoundException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Activity successfully deleted.");
        return true;
    }

    private boolean listPlans() {
        String planListJson = this.planRepository.getPlanList();

        System.out.println("SUCCESS: Plan list retrieval successful.");
        System.out.println(planListJson);
        return true;
    }

    private boolean createAdaptation(String path, String[] tokens) {
        Adaptation adaptation;
        try {
            adaptation = Adaptation.fromTokens(tokens);
        } catch (InvalidTokenException e) {
            System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return false;
        }

        File jarFile = new File(path);
        if (!jarFile.exists()) {
            System.err.println(String.format("File not found: %s", path));
            return false;
        }

        String id;
        try {
            id = this.adaptationRepository.createAdaptation(adaptation, jarFile);
        } catch (AdaptationRepository.InvalidAdaptationException e) {
            System.err.println(e);
            return false;
        }

        System.out.println(String.format("CREATED: Adaptation successfully created at: %s.", id));
        return true;
    }

    private boolean deleteAdaptation(String adaptationId) {
        try {
            this.adaptationRepository.deleteAdaptation(adaptationId);
        } catch (AdaptationRepository.AdaptationNotFoundException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Adaptation successfully deleted.");
        return true;
    }

    private boolean displayAdaptation(String adaptationId) {
        Adaptation adaptation;
        try {
            adaptation = this.adaptationRepository.getAdaptation(adaptationId);
        } catch (AdaptationRepository.AdaptationNotFoundException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Adaptation retrieval successful.");
        System.out.println(JsonUtilities.convertAdaptationToJson(adaptation));
        return true;
    }

    private boolean listAdaptations() {
        String adaptationListJson = this.adaptationRepository.getAdaptationList();

        System.out.println("SUCCESS: Adaptation list retrieval successful.");
        System.out.println(adaptationListJson);
        return true;
    }

    private boolean listActivityTypes(String adaptationId) {
        String activityTypeListJson;
        try {
            activityTypeListJson = this.adaptationRepository.getActivityTypes(adaptationId);
        } catch (AdaptationRepository.AdaptationNotFoundException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Activity type list retrieval successful.");
        System.out.println(activityTypeListJson);
        return true;
    }

    private boolean displayActivityType(String adaptationId, String activityType) {
        String activityTypeJson;
        try {
            activityTypeJson = this.adaptationRepository.getActivityType(adaptationId, activityType);
        } catch (AdaptationRepository.AdaptationNotFoundException | AdaptationRepository.ActivityTypeNotDefinedException e) {
            System.err.println(e);
            return false;
        }

        System.out.println("SUCCESS: Activity type retrieval successful.");
        System.out.println(activityTypeJson);
        return true;
    }

    private boolean convertApfFile(String input, String output, String dir, String[] tokens) {

        /* Parse adaptation and plan file */
        Plan plan;
        try {
            gov.nasa.jpl.ammos.mpsa.apgen.model.Adaptation adaptation = AdaptationParser.parseDirectory(Path.of(dir));
            plan = ApfParser.parseFile(Path.of(input), adaptation);
        } catch (AdaptationParsingException | PlanParsingException e) {
            System.err.println(e.getMessage());
            return false;
        } catch (DirectoryNotFoundException e) {
            System.err.println(String.format("Adaptation directory not found: %s", e.getMessage()));
            return false;
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
            return true;
        }
        return false;
    }
}
