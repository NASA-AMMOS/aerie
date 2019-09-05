package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidNumberOfArgsException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class CommandOptions {

    @Bean
    public RestTemplate restTemplate() {
        return new MerlinRestTemplate();
    }

    @Autowired
    RestTemplate restTemplate;

    private Options options = new Options();
    private OptionGroup requiredGroup = new OptionGroup();
    private OptionGroup planIdRequiredGroup = new OptionGroup();
    private OptionGroup adaptationIdRequiredGroup = new OptionGroup();
    private String[] args = null;
    private boolean lastCommandStatus;

    // Empty constructor for use by Spring
    public CommandOptions() {
        buildArguments();
    }

    public CommandOptions(String[] args) {
        
        // For the tests, RestTemplate is autowired
        // but for actual use we don't use Spring
        // so we need to instantiate it
        if (restTemplate == null)
            restTemplate = new MerlinRestTemplate();

        buildArguments();
        consumeArgs(args);
    }

    public CommandOptions consumeArgs(String[] args) {
        this.args = args;
        return this;
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

        // Set the request type group as required
        requiredGroup.setRequired(true);


        // TODO: Figure out how to resolve the names for arguments so they make sense without being obnoxious
        // Being in a group makes options mutually exclusive
        planIdRequiredGroup.addOption(new Option("D", "delete-plan",false, "Delete a plan"));
        planIdRequiredGroup.addOption(new Option("U", "update-plan-from-file", true, "Update plan based on values in plan file"));
        planIdRequiredGroup.addOption(new Option(null, "append-activities", true,"Append new activity instances to a planfrom a json"));
        planIdRequiredGroup.addOption(new Option("pull", "download-plan", true, "Download a plan into a file"));
        planIdRequiredGroup.addOption(new Option(null, "display-activity", true, "Display an activity from a plan"));
        planIdRequiredGroup.addOption(new Option(null, "delete-activity", true, "Delete an activity from a plan"));

        // Options that take more than one arg must be made separately
        opt = new Option(null, "update-plan", true,"Update the plan metadata");
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
        adaptationIdRequiredGroup.addOption(new Option("parameters", "activity-type-parameters", true, "View parameters of an activity type from the specified adaptation"));

        options.addOptionGroup(adaptationIdRequiredGroup);
        options.addOptionGroup(requiredGroup);
    }

    public void parse() {

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
                String[] args = cmd.getOptionValues("A");
                if (args.length < 3) {
                    throw new InvalidNumberOfArgsException("Option 'A' requires at least three arguments");
                }
                String path = args[0];
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
                    lastCommandStatus = updatePlan(planId, tokens);
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
                    String[] args = cmd.getOptionValues("update-activity");
                    String activityId = args[0];
                    String[] tokens = Arrays.copyOfRange(args, 1, args.length);
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
                else if (cmd.hasOption("parameters")) {
                    String activityId = cmd.getOptionValue("parameters");
                    lastCommandStatus = displayActivityTypeParameterList(adaptationId, activityId);
                    return;
                }
            }
            else {
                System.out.println("No required argument specified.");
            }
        } catch (ParseException | InvalidNumberOfArgsException e) {
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
        try {
            NewPlanCommand command = new NewPlanCommand(restTemplate, path);
            command.execute();
            int status = command.getStatus();

            switch(status) {
                case 201:
                    String planId = command.getId();
                    System.out.println(String.format("CREATED: Plan successfully created at: %s.", planId));
                    return true;

                case 409:
                    System.err.println("CONFLICT: Plan already exists.");
                    break;

                case 422:
                    System.err.println("BAD REQUEST: Check validity of Plan JSON.");
                    break;

                default:
                    System.err.println(String.format("Unexpected status: %s", status));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        System.err.println("Plan upload failed.");
        return false;
    }

    private boolean updatePlanFromFile(String planId, String path) {
        try {
            UpdatePlanFileCommand command = new UpdatePlanFileCommand(restTemplate, planId, path);
            command.execute();
            int status = command.getStatus();

            switch(status) {
                case 200:
                case 204:
                    System.out.println("SUCCESS: Plan successfully updated.");
                    return true;

                case 404:
                    System.err.println(String.format("NOT FOUND: Plan with id %s does not exist.", planId));
                    break;

                case 422:
                    System.err.println("BAD REQUEST: Check validity of Plan JSON.");
                    break;

                default:
                    System.err.println(String.format("Unexpected status: %s", status));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        System.err.println("Plan update failed.");
        return false;
    }

    private boolean updatePlan(String planId, String[] tokens) {
        try {
            UpdatePlanCommand command = new UpdatePlanCommand(restTemplate, planId, tokens);
            command.execute();
            int status = command.getStatus();

            switch(status) {
                case 200:
                case 204:
                    System.out.println("SUCCESS: Plan successfully updated.");
                    return true;

                case 404:
                    System.err.println(String.format("NOT FOUND: Plan with id %s does not exist.", planId));
                    break;

                default:
                    System.err.println(String.format("Unexpected status: %s", status));
            }

            System.err.println("Plan update failed.");
            return false;
        } catch (InvalidTokenException e) {
            System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return false;
        }
    }

    private boolean deletePlan(String planId) {
        DeletePlanCommand command = new DeletePlanCommand(restTemplate, planId);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
            case 204:
                System.out.println("SUCCESS: Plan successfully deleted.");
                return true;

            case 404:
                System.err.println(String.format("NOT FOUND: Plan with id %s does not exist.", planId));
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Plan delete failed.");
        return false;
    }

    private boolean downloadPlan(String planId, String outName) {
        if (new File(outName).exists()) {
            System.err.println(String.format("File %s already exists.", outName));
            return false;
        }

        DownloadPlanCommand command = new DownloadPlanCommand(restTemplate, planId, outName);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
                System.out.println("SUCCESS: Plan successfully downloaded.");
                return true;

            case 404:
                System.err.println(String.format("NOT FOUND: Plan with id %s does not exist.", planId));
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Plan download failed.");
        return false;
    }

    private boolean appendActivityInstances(String planId, String path) {
        try {
            AppendActivitiesCommand command = new AppendActivitiesCommand(restTemplate, planId, path);
            command.execute();
            int status = command.getStatus();
            switch(status) {
                case 201:
                    System.out.println(String.format("CREATED: Activities successfully created."));
                    return true;

                case 422:
                    System.err.println("BAD REQUEST: Check validity of Activity JSON.");
                    break;

                default:
                    System.err.println(String.format("Unexpected status: %s", status));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        System.err.println("Activity creation failed.");
        return false;
    }

    private boolean displayActivityInstance(String planId, String activityId) {
        GetActivityCommand command = new GetActivityCommand(restTemplate, planId, activityId);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
                System.out.println("OK: Activity retrieval successful.");
                System.out.println(command.getResponseBody());
                return true;

            case 404:
                System.err.println("NOT FOUND: The requested activity could not be found");
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Activity request failed.");
        return false;
    }

    private boolean updateActivityInstance(String planId, String activityId, String[] tokens) {
        try {
            UpdateActivityCommand command = new UpdateActivityCommand(restTemplate, planId, activityId, tokens);

            command.execute();
            int status = command.getStatus();

            switch(status) {
                case 200:
                case 204:
                    System.out.println("SUCCESS: Activity successfully updated.");
                    return true;

                case 404:
                    System.err.println(String.format("NOT FOUND: Activity with id %s in plan with id %s does not exist.", activityId, planId));
                    break;

                default:
                    System.err.println(String.format("Unexpected status: %s", status));
            }

            System.err.println("Activity update failed.");
            return false;
        } catch (InvalidTokenException e) {
            System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return false;
        }
    }

    private boolean deleteActivityInstance(String planId, String activityId) {
        DeleteActivityCommand command = new DeleteActivityCommand(restTemplate, planId, activityId);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
            case 204:
                System.out.println("SUCCESS: Activity successfully deleted.");
                return true;

            case 404:
                System.err.println("NOT FOUND: The requested activity could not be found.");
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Plan delete failed.");
        return false;
    }

    private boolean listPlans() {
        GetPlanListCommand command = new GetPlanListCommand(restTemplate);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
                System.out.println("OK: Plan list retrieval successful.");
                System.out.println(command.getResponseBody());
                return true;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Plan request failed.");
        return false;
    }

    private boolean createAdaptation(String path, String[] tokens) {
        try {
            NewAdaptationCommand command = new NewAdaptationCommand(restTemplate, path, tokens);
            command.execute();
            int status = command.getStatus();

            switch(status) {
                case 201:
                    String planId = command.getId();
                    System.out.println(String.format("CREATED: Adaptation successfully created at: %s.", planId));
                    return true;

                case 409:
                    System.err.println("CONFLICT: Adaptation already exists.");
                    break;

                case 422:
                    System.err.println("BAD REQUEST: Check validity of Adaptation JAR.");
                    break;

                default:
                    System.err.println(String.format("Unexpected status: %s", status));
            }

            System.err.println("Adaptation creation failed.");
            return false;
        } catch (InvalidTokenException e) {
            System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
            return false;
        }
    }

    private boolean deleteAdaptation(String adaptationId) {
        DeleteAdaptationCommand command = new DeleteAdaptationCommand(restTemplate, adaptationId);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
            case 204:
                System.out.println("SUCCESS: Adaptation successfully deleted.");
                return true;

            case 404:
                System.err.println(String.format("NOT FOUND: Adaptation with id %s does not exist.", adaptationId));
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Adaptation delete failed.");
        return false;
    }

    private boolean displayAdaptation(String adaptationId) {
        GetAdaptationCommand command = new GetAdaptationCommand(restTemplate, adaptationId);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
                System.out.println("OK: Adaptation retrieval successful.");
                System.out.println(command.getResponseBody());
                return true;

            case 404:
                System.err.println("NOT FOUND: The requested adaptation could not be found");
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Adaptation request failed.");
        return false;
    }

    private boolean listAdaptations() {
        GetAdaptationListCommand command = new GetAdaptationListCommand(restTemplate);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
                System.out.println("OK: Adaptation list retrieval successful.");
                System.out.println(command.getResponseBody());
                return true;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Adaptation request failed.");
        return false;
    }

    private boolean listActivityTypes(String adaptationId) {
        GetActivityTypeListCommand command = new GetActivityTypeListCommand(restTemplate, adaptationId);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
                System.out.println("OK: Activity type list retrieval successful.");
                System.out.println(command.getResponseBody());
                return true;

            case 404:
                System.err.println("NOT FOUND: The requested adaptation could not be found");
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Activity type list request failed.");
        return false;
    }

    private boolean displayActivityType(String adaptationId, String activityTypeId) {
        GetActivityTypeCommand command = new GetActivityTypeCommand(restTemplate, adaptationId, activityTypeId);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
                System.out.println("OK: Activity type retrieval successful.");
                System.out.println(command.getResponseBody());
                return true;

            case 404:
                System.err.println("NOT FOUND: The requested adaptation could not be found");
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Activity type request failed.");
        return false;
    }

    private boolean displayActivityTypeParameterList(String adaptationId, String activityTypeId) {
        GetActivityTypeParameterListCommand command = new GetActivityTypeParameterListCommand(restTemplate, adaptationId, activityTypeId);
        command.execute();
        int status = command.getStatus();

        switch(status) {
            case 200:
                System.out.println("OK: Activity type parameter list retrieval successful.");
                System.out.println(command.getResponseBody());
                return true;

            case 404:
                System.err.println("NOT FOUND: The requested adaptation could not be found");
                break;

            default:
                System.err.println(String.format("Unexpected status: %s", status));
        }

        System.err.println("Activity type parameter list request failed.");
        return false;
    }
}
