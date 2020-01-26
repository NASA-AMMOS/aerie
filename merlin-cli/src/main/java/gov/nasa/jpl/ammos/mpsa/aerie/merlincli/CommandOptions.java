package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidNumberOfArgsException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Arrays;

public class CommandOptions {
    private Options options = new Options();
    private OptionGroup requiredGroup = new OptionGroup();
    private OptionGroup planIdRequiredGroup = new OptionGroup();
    private OptionGroup adaptationIdRequiredGroup = new OptionGroup();
    private boolean lastCommandStatus;

    public CommandOptions() {
        buildArguments();
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

    public void parse(final MerlinCommandReceiver commandReceiver, final String[] args) {
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
                lastCommandStatus = commandReceiver.listPlans();
                return;
            }
            else if (requiredGroup.getSelected().equals("adaptations")) {
                lastCommandStatus = commandReceiver.listAdaptations();
                return;
            }
            else if (requiredGroup.getSelected().equals("P")) {
                String path = cmd.getOptionValue("P");
                lastCommandStatus = commandReceiver.createPlan(path);
                return;
            }
            else if (requiredGroup.getSelected().equals("A")) {
                String[] params = cmd.getOptionValues("A");
                if (params.length < 3) {
                    throw new InvalidNumberOfArgsException("Option 'A' requires at least three arguments");
                }
                String path = params[0];
                String[] tokens = Arrays.copyOfRange(args, 1, args.length);
                lastCommandStatus = commandReceiver.createAdaptation(path, tokens);
                return;
            }
            else if (requiredGroup.getSelected().equals("p")) {
                String planId = cmd.getOptionValue("p");

                if (cmd.hasOption("U")) {
                    String path = cmd.getOptionValue("U");
                    lastCommandStatus = commandReceiver.updatePlanFromFile(planId, path);
                    return;
                }
                else if (cmd.hasOption("update-plan")) {
                    String[] tokens = cmd.getOptionValues("update-plan");
                    lastCommandStatus = commandReceiver.updatePlanFromTokens(planId, tokens);
                    return;
                }
                else if (cmd.hasOption("delete-plan")) {
                    lastCommandStatus = commandReceiver.deletePlan(planId);
                    return;
                }
                else if (cmd.hasOption("pull")) {
                    String outName = cmd.getOptionValue("pull");
                    lastCommandStatus = commandReceiver.downloadPlan(planId, outName);
                    return;
                }
                else if (cmd.hasOption("append-activities")) {
                    String path = cmd.getOptionValue("append-activities");
                    lastCommandStatus = commandReceiver.appendActivityInstances(planId, path);
                    return;
                }
                else if (cmd.hasOption("display-activity")) {
                    String activityId = cmd.getOptionValue("display-activity");
                    lastCommandStatus = commandReceiver.displayActivityInstance(planId, activityId);
                    return;
                }
                else if (cmd.hasOption("update-activity")) {
                    String[] params = cmd.getOptionValues("update-activity");
                    String activityId = params[0];
                    String[] tokens = Arrays.copyOfRange(params, 1, params.length);
                    lastCommandStatus = commandReceiver.updateActivityInstance(planId, activityId, tokens);
                    return;
                }
                else if (cmd.hasOption("delete-activity")) {
                    String activityId = cmd.getOptionValue("delete-activity");
                    lastCommandStatus = commandReceiver.deleteActivityInstance(planId, activityId);
                    return;
                }
            }
            else if (requiredGroup.getSelected().equals("a")) {
                String adaptationId = cmd.getOptionValue("a");
                if (cmd.hasOption("delete-adaptation")) {
                    lastCommandStatus = commandReceiver.deleteAdaptation(adaptationId);
                    return;
                }
                else if (cmd.hasOption("display")) {
                    lastCommandStatus = commandReceiver.displayAdaptation(adaptationId);
                    return;
                }
                else if (cmd.hasOption("activities")) {
                    lastCommandStatus = commandReceiver.listActivityTypes(adaptationId);
                    return;
                }
                else if (cmd.hasOption("activity")) {
                    String activityId = cmd.getOptionValue("activity");
                    lastCommandStatus = commandReceiver.displayActivityType(adaptationId, activityId);
                    return;
                }
            }
            else if (requiredGroup.getSelected().equals("c")) {
                String[] params = cmd.getOptionValues("c");
                if (params.length < 3) {
                    throw new InvalidNumberOfArgsException("Option 'apf' requires three arguments <infile> <outfile> <dir> <tokens>");
                }
                String[] tokens = Arrays.copyOfRange(params, 3, params.length);
                lastCommandStatus = commandReceiver.convertApfFile(params[0], params[1], params[2], tokens);
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
}
