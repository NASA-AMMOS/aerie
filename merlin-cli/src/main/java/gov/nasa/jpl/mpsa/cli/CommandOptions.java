package gov.nasa.jpl.mpsa.cli;

import gov.nasa.jpl.mpsa.cli.commands.Command;
import gov.nasa.jpl.mpsa.cli.commands.impl.NewActivityCommand;
import gov.nasa.jpl.mpsa.cli.models.ActivityInstance;
import gov.nasa.jpl.mpsa.cli.models.ActivityInstanceArray;
import gov.nasa.jpl.mpsa.cli.models.Parameter;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandOptions {

    Options options = new Options();
    private String[] args = null;
    private String planId;
    Command command;


    public CommandOptions(String[] args) {
        this.args = args;

        Option addActivity   = Option.builder("a")
                .longOpt("add-activity")
                .argName("parameters")
                .hasArgs()
                .desc("Add activity to the plan passing the name and the parameters to be used")
                .build();

        Option setPlan   = Option.builder("p")
                .longOpt("plan-id")
                .argName("id")
                .hasArg()
                .required()
                .desc("Specify the plan Id to use")
                .build();

        options.addOption(addActivity);
        options.addOption("d", "decompose", true, "Run decomposition for activity again");
        options.addOption("e", "edit-activity", true, "Pass the activity to edit");
        options.addOption("f", "final-condition", false, "Generate final state of the system. Returns the ID of the recorded state");
        options.addOption("h", "help", false, "show help.");
        options.addOption("i", "init-condition", true, "Set initial state of the system, the argument is the ID of the state");
        options.addOption("k", "load-kernels", true, "List of paths for Kernels to use");
        options.addOption("m", "move-activity", true, "Where to move the activity");
        options.addOption(setPlan);
        options.addOption("P", "create-plan", true, "Add a new plan passing the name, adaptation to use and the version");
        options.addOption("q", "sequence", true, "Extracts and stores the start/end times for writing sequences.");
        options.addOption("r", "remodel", true, "Run simulation for the plan");
        options.addOption("s", "schedule", true, "Generate schedule of the plan");
        options.addOption("S", "severe-activity", true, "Severe an activity");
        options.addOption("w", "write", true, "Generate a product");
        options.addOption("x", "set-parameter", true, "Set parameter for activity instance");

    }


    public void parse() {


        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            // WE MUST RECEIVE A PLAN ID, OTHERWISE WE DON'T KNOW WHAT TO DO WITH ACTIVITIES
            if (cmd.hasOption("p")) {
                System.out.println("Specifying plan -p=" + cmd.getOptionValue("p"));
                this.planId = cmd.getOptionValue("p");
            }

            if (cmd.hasOption("a")) {
                addActivity(cmd.getOptionValues("a"));

            } else if (cmd.hasOption("h")) {
                printUsage();
            } else {
                System.err.println("Missing option [arguments]");
                printUsage();
            }
        } catch (ParseException e) {
            System.err.println("Failed to parse command line properties" + e);
            printUsage();
        }

    }



    private void addActivity(String[] args) {

        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("Starting execution of ADD ACTIVITY");


        String activity = "";

        if( this.planId != null && !this.planId.equals("")) {

            String inputString = "";
            for(String s: args) {
                inputString += s + " ";
            }

            List<Parameter> params = new ArrayList<>();

            // The parameter should come in a form like: <name>:<type>=<value>
            Pattern pattern = Pattern.compile("^(.+?)\\s+");
            Pattern patternParams = Pattern.compile("(\\S+):{1}(\\S+)={1}(\\S*)");
            Matcher matcher = pattern.matcher(inputString);

            List<String> allMatches = new ArrayList<String>();

            // use regex to parse the value
            while (matcher.find()) {
                if(matcher.group(1) != null) {
                    activity = matcher.group(1);
                }
            }

            Matcher matcherParams = patternParams.matcher(inputString);
            while(matcherParams.find()) {
                params.add(new Parameter(matcherParams.group(1), matcherParams.group(2), matcherParams.group(3)));

            }

            List<ActivityInstance> instancesToInsert = new ArrayList<>();

            for(int i=0; i<150000; i++) {
                ActivityInstance activityInstance = new ActivityInstance();
                activityInstance.setActivityId(UUID.randomUUID().toString());
                activityInstance.setActivityType(activity);
                activityInstance.setParameters(params);
                instancesToInsert.add(activityInstance);
            }

            command = new NewActivityCommand(activity, this.planId, new ActivityInstanceArray(instancesToInsert));
            command.execute();


        } else {
            // TODO: Return an error indicating that the planId is missing.
        }

        watch.stop();

        System.out.println("It took " + watch.getTime(TimeUnit.MILLISECONDS) + " milliseconds to execute");

    }

    private void printUsage() {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Merlin Adaptation", options);
    }
}
