package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
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
    private OptionGroup planGroup = new OptionGroup();
    private String[] args = null;
    private String planId;
    private Command command;
    private boolean lastCommandStatus;

    // Empty constructor for use by Spring
    public CommandOptions() {}

    public CommandOptions(String[] args) {
        
        // For the tests, RestTemplate is autowired
        // but for actual use we don't use Spring
        // so we need to instantiate it
        if (restTemplate == null)
            restTemplate = new MerlinRestTemplate();

        consumeArgs(args);
    }

    public CommandOptions consumeArgs(String[] args) {
        this.args = args;

        Option addActivity   = Option.builder("a")
                .longOpt("add-activity")
                .argName("parameters")
                .hasArgs()
                .desc("Add activity to the plan passing the name and the parameters to be used.")
                .build();

        Option updateActivityOption = Option.builder("updact")
                .longOpt("update-activity")
                .hasArgs()
                .desc("Update an activity from a plan")
                .build();

        Option updatePlanOption = Option.builder("u")
                .longOpt("update-plan")
                .hasArgs()
                .desc("Update the plan metadata")
                .build();

        options.addOption("p", "plan-id", true, "Specify the plan ID to use");

        // Being in a group makes options mutually exclusive
        planGroup.addOption(new Option("P", "create-plan", true, "Add a new plan passing the name of a PlanDetail JSON"));
        planGroup.addOption(new Option("D", "delete-plan",false, "Delete a plan."));
        planGroup.addOption(new Option("U", "update-plan-from-file", true, "Update plan based on values in plan file"));
        planGroup.addOption(updatePlanOption);
        planGroup.addOption(new Option("A", "append-activities", true,"Append new activity instances to a planfrom a json"));
        planGroup.addOption(new Option("pull", "download-plan", true, "Download a plan into a file"));
        planGroup.addOption(new Option("dispact", "display-activity", true, "Display an activity from a plan"));
        planGroup.addOption(updateActivityOption);
        planGroup.addOption(new Option("delact", "delete-activity", true, "Delete an activity from a plan"));

        options.addOptionGroup(planGroup);

        return this;
    }

    public void parse() {

        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                printUsage();
                return;
            }

            if (planGroup.getSelected() != null) {
                if (!planGroup.getSelected().equals("P") && !cmd.hasOption("p")) {
                    System.err.println(String.format("Option '%s' requires --plan-id.", planGroup.getSelected()));
                    printUsage();
                    return;
                } else if (planGroup.getSelected().equals("P") && cmd.hasOption("p")) {
                    System.err.println("Option 'P' is mutually exclusive with --plan-id");
                }
            }

            if (cmd.hasOption("P")) {
                String path = cmd.getOptionValue("P");
                lastCommandStatus = createPlan(path);

            } else if (cmd.hasOption("U")) {
                String planId = cmd.getOptionValue("p");
                String path = cmd.getOptionValue("U");
                lastCommandStatus = updatePlanFromFile(planId, path);

            } else if (cmd.hasOption("u")) {
                String planId = cmd.getOptionValue("p");
                String[] tokens = cmd.getOptionValues("u");
                lastCommandStatus = updatePlan(planId, tokens);

            } else if (cmd.hasOption("D")) {
                String planId = cmd.getOptionValue("p");
                lastCommandStatus = deletePlan(planId);

            } else if (cmd.hasOption("pull")) {
                String planId = cmd.getOptionValue("p");
                String outName = cmd.getOptionValue("pull");
                lastCommandStatus = downloadPlan(planId, outName);

            } else if (cmd.hasOption("A")) {
                String planId = cmd.getOptionValue("p");
                String path = cmd.getOptionValue("A");
                lastCommandStatus = appendActivities(planId, path);

            } else if (cmd.hasOption("dispact")) {
                String planId = cmd.getOptionValue("p");
                String activityId = cmd.getOptionValue("dispact");
                lastCommandStatus = displayActivity(planId, activityId);

            } else if (cmd.hasOption("updact")) {
                String planId = cmd.getOptionValue("p");
                String[] args = cmd.getOptionValues("updact");
                String activityId = args[0];
                String[] tokens = Arrays.copyOfRange(args, 1, args.length);
                lastCommandStatus = updateActivity(planId, activityId, tokens);

            } else if (cmd.hasOption("delact")) {
                String planId = cmd.getOptionValue("p");
                String activityId = cmd.getOptionValue("delact");
                lastCommandStatus = deleteActivity(planId, activityId);
            }

        } catch (ParseException e) {
            System.err.println("Failed to parse command line properties" + e);
            printUsage();
        }
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

    private boolean appendActivities(String planId, String path) {
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

    private boolean displayActivity(String planId, String activityId) {
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

    private boolean updateActivity(String planId, String activityId, String[] tokens) {
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

    private boolean deleteActivity(String planId, String activityId) {
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
}
