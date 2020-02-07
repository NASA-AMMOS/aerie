package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidNumberOfArgsException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.Adaptation;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.nio.file.Path;
import java.util.Arrays;

public class CommandOptions {
    private final Options options = new Options();
    private final OptionGroup requiredGroup = new OptionGroup();

    public CommandOptions() {
        // Add option to request help
        requiredGroup.addOption(new Option("h", "help", false, "Print help message"));

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
        final OptionGroup planIdRequiredGroup = new OptionGroup();
        planIdRequiredGroup.addOption(new Option("D", "delete-plan", false, "Delete a plan"));
        planIdRequiredGroup.addOption(new Option("U", "update-plan-from-file", true, "Update plan based on values in plan file"));
        planIdRequiredGroup.addOption(new Option(null, "append-activities", true, "Append new activity instances to a plan from a json"));
        planIdRequiredGroup.addOption(new Option("pull", "download-plan", true, "Download a plan into a file"));
        planIdRequiredGroup.addOption(new Option(null, "display-activity", true, "Display an activity from a plan"));
        planIdRequiredGroup.addOption(new Option(null, "delete-activity", true, "Delete an activity from a plan"));
        planIdRequiredGroup.addOption(new Option("S", "simulate", false, "Simulate a plan"));

        // Options that take more than one arg must be made separately
        opt = new Option(null, "update-plan", true, "Update the plan metadata");
        opt.setArgs(Option.UNLIMITED_VALUES);
        planIdRequiredGroup.addOption(opt);

        opt = new Option(null, "update-activity", true, "Update an activity from a plan");
        opt.setArgs(Option.UNLIMITED_VALUES);
        planIdRequiredGroup.addOption(opt);

        options.addOptionGroup(planIdRequiredGroup);

        final OptionGroup adaptationIdRequiredGroup = new OptionGroup();
        adaptationIdRequiredGroup.addOption(new Option(null, "delete-adaptation", false, "Delete an adaptation"));
        adaptationIdRequiredGroup.addOption(new Option("display", "view-adaptation", false, "View an adaptation's metadata"));
        adaptationIdRequiredGroup.addOption(new Option("activities", "activity-types", false, "View an adaptation's activity types"));
        adaptationIdRequiredGroup.addOption(new Option("activity", "activity-type", true, "View an activity type from the specified adaptation"));

        options.addOptionGroup(adaptationIdRequiredGroup);
        options.addOptionGroup(requiredGroup);
    }

    public boolean parse(final MerlinCommandReceiver commandReceiver, final String[] args) {
        final CommandLine cmd;
        try {
            cmd = (new DefaultParser()).parse(options, args);
        } catch (ParseException e) {
            System.err.println("Failed to parse command line properties: " + e.getMessage());
            return false;
        }

        if (cmd.hasOption("h")) {
            printUsage();
            return true;
        }

        try {
            // TODO: Eventually, we should probably check that other options aren't specified, or at least point out
            //       that we ignore them if they are
            switch (requiredGroup.getSelected()) {
                case "plans":
                    commandReceiver.listPlans();
                    return true;

                case "adaptations":
                    commandReceiver.listAdaptations();
                    return true;

                case "P": {
                    String path = cmd.getOptionValue("P");
                    commandReceiver.createPlan(path);
                    return true;
                }

                case "A": {
                    String[] params = cmd.getOptionValues("A");
                    if (params.length < 3) {
                        throw new InvalidNumberOfArgsException("Option 'A' requires at least three arguments");
                    }

                    String path = params[0];
                    String[] tokens = Arrays.copyOfRange(params, 1, params.length);

                    Adaptation adaptation;
                    try {
                        adaptation = Adaptation.fromTokens(tokens);
                    } catch (InvalidTokenException e) {
                        System.err.println(String.format("Error while parsing token: %s\n%s", e.getToken(), e.getMessage()));
                        return false;
                    }

                    commandReceiver.createAdaptation(Path.of(path), adaptation);
                    return true;
                }

                case "p":
                    String planId = cmd.getOptionValue("p");

                    if (cmd.hasOption("U")) {
                        String path = cmd.getOptionValue("U");
                        commandReceiver.updatePlanFromFile(planId, path);
                        return true;
                    } else if (cmd.hasOption("update-plan")) {
                        String[] tokens = cmd.getOptionValues("update-plan");
                        commandReceiver.updatePlanFromTokens(planId, tokens);
                        return true;
                    } else if (cmd.hasOption("delete-plan")) {
                        commandReceiver.deletePlan(planId);
                        return true;
                    } else if (cmd.hasOption("pull")) {
                        String outName = cmd.getOptionValue("pull");
                        commandReceiver.downloadPlan(planId, outName);
                        return true;
                    } else if (cmd.hasOption("append-activities")) {
                        String path = cmd.getOptionValue("append-activities");
                        commandReceiver.appendActivityInstances(planId, path);
                        return true;
                    } else if (cmd.hasOption("display-activity")) {
                        String activityId = cmd.getOptionValue("display-activity");
                        commandReceiver.displayActivityInstance(planId, activityId);
                        return true;
                    } else if (cmd.hasOption("update-activity")) {
                        String[] params = cmd.getOptionValues("update-activity");
                        String activityId = params[0];
                        String[] tokens = Arrays.copyOfRange(params, 1, params.length);
                        commandReceiver.updateActivityInstance(planId, activityId, tokens);
                        return true;
                    } else if (cmd.hasOption("delete-activity")) {
                        String activityId = cmd.getOptionValue("delete-activity");
                        commandReceiver.deleteActivityInstance(planId, activityId);
                        return true;
                    } else if (cmd.hasOption("simulate")) {
                        commandReceiver.performSimulation(planId);
                    } else {
                        return false;
                    }

                case "a":
                    String adaptationId = cmd.getOptionValue("a");
                    if (cmd.hasOption("delete-adaptation")) {
                        commandReceiver.deleteAdaptation(adaptationId);
                        return true;
                    } else if (cmd.hasOption("display")) {
                        commandReceiver.displayAdaptation(adaptationId);
                        return true;
                    } else if (cmd.hasOption("activities")) {
                        commandReceiver.listActivityTypes(adaptationId);
                        return true;
                    } else if (cmd.hasOption("activity")) {
                        String activityId = cmd.getOptionValue("activity");
                        commandReceiver.displayActivityType(adaptationId, activityId);
                        return true;
                    } else {
                        return false;
                    }

                case "c": {
                    String[] params = cmd.getOptionValues("c");
                    if (params.length < 3) {
                        throw new InvalidNumberOfArgsException("Option 'apf' requires three arguments <infile> <outfile> <dir> <tokens>");
                    }
                    String[] tokens = Arrays.copyOfRange(params, 3, params.length);
                    commandReceiver.convertApfFile(params[0], params[1], params[2], tokens);
                    return true;
                }

                default:
                    System.out.println("No required argument specified.");
                    return false;
            }
        } catch (InvalidNumberOfArgsException e) {
            System.err.println("Failed to parse command line properties: " + e);
            return false;
        }
    }

    public void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Merlin Adaptation", options);
    }
}
