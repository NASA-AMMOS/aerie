package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.UpdatePlanCommand;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstanceParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.NewActivityCommand;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstanceArray;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandOptions {

    private Options options = new Options();
    private String[] args = null;
    private String planId;
    private Command command;

    public CommandOptions(String[] args) {
        this.args = args;

        Option addActivity   = Option.builder("a")
                .longOpt("add-activity")
                .argName("parameters")
                .hasArgs()
                .desc("Add activity to the plan passing the name and the parameters to be used. Mutually exclusive with -u")
                .build();

        Option setPlan   = Option.builder("p")
                .longOpt("plan-id")
                .argName("id")
                .hasArg()
                .required()
                .desc("Specify the plan Id to use")
                .build();

        Option replacePlan = Option.builder("u")
                .longOpt("update")
                .argName("file")
                .hasArgs()
                .desc("Set the plan contents based on an adaptationId, start time, end time and plan file. Mutually exclusive with -a")
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
        options.addOption(replacePlan);
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

            if (cmd.hasOption("h")) {
                printUsage();
                return;
            }

            // WE MUST RECEIVE A PLAN ID, OTHERWISE WE DON'T KNOW WHAT TO DO WITH ACTIVITIES
            if (cmd.hasOption("p")) {
                System.out.println("Specifying plan -p=" + cmd.getOptionValue("p"));
                this.planId = cmd.getOptionValue("p");
            }

            if (cmd.hasOption("a") && cmd.hasOption("u")) {
                System.err.println("Options -a and -u both specified but only one allowed.");
                printUsage();
                return;
            }

            if (cmd.hasOption("a")) {
                addActivity(cmd.getOptionValues("a"));
            } else if (cmd.hasOption("u")) {
                String[] opts = cmd.getOptionValues("u");
                if (opts.length < 4) {
                    System.err.println("-u requires an adaptationId, start time, end time and plan file");
                    printUsage();
                } else {
                    String adaptationId = opts[0];
                    String start = opts[1];
                    String end = opts[2];
                    String path = opts[3];
                    updatePlan(adaptationId, start, end, path);
                }
            } else {
                System.err.println("Missing option [arguments]");
                printUsage();
            }
        } catch (ParseException e) {
            System.err.println("Failed to parse command line properties" + e);
            printUsage();
        }
    }

    private void printUsage() {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Merlin Adaptation", options);
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

            List<ActivityInstanceParameter> params = new ArrayList<>();

            // The parameter should come in a form like: <name>:<type>=<value>
            Pattern pattern = Pattern.compile("^(.+?)\\s+");
            Pattern patternParams = Pattern.compile("(\\S+):{1}(\\S+)={1}(\\S*)");
            Matcher matcher = pattern.matcher(inputString);

            // use regex to parse the value
            while (matcher.find()) {
                if(matcher.group(1) != null) {
                    activity = matcher.group(1);
                }
            }

            Matcher matcherParams = patternParams.matcher(inputString);
            while(matcherParams.find()) {
                String defaultValue = "";
                List<String> range = new ArrayList<String>();
                params.add(new ActivityInstanceParameter(defaultValue, matcherParams.group(1), range, matcherParams.group(2), matcherParams.group(3)));
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

    private void updatePlan(String adaptationId, String startTimestamp, String endTimestamp, String path) {
        File file = new File(path);

        try {
            List<ActivityInstance> instances = parsePlan(file);

            command = new UpdatePlanCommand(this.planId, startTimestamp, endTimestamp, adaptationId, new ActivityInstanceArray(instances));
            command.execute();
        } catch (MalformedFileException e) {
            System.err.println(e.getMessage());
        }
    }

    public static List<ActivityInstance> parsePlan(File file) throws MalformedFileException {
        List<ActivityInstance> instances = new ArrayList<>();

        try {
            Scanner sc = new Scanner(file);

            if (!sc.hasNextLine()) {
                throw new MalformedFileException(file, "File has no contents.");
            }

            while (sc.hasNextLine()) {
                String spec = sc.nextLine();
                String[] specs = spec.split("\\s+");

                // We expect at least 2 components to an activity instance definition
                // 1. Activity Type
                // 2. Start Time
                // There may be more parameters, but these are the minimum.
                if (specs.length >= 2) {
                    String type = specs[0];

                    List<ActivityInstanceParameter> parameters = parseActivityParameters(Arrays.copyOfRange(specs, 1, specs.length));

                    ActivityInstance instance = new ActivityInstance();
                    instance.setActivityType(type);
                    instance.setParameters(parameters);
                    instances.add(instance);
                }
                else {
                    throw new MalformedFileException(file, String.format("Activity instance specification insufficient: %s", spec));
                }
            }
        } catch (FileNotFoundException | MalformedParameterStringException e) {
            throw new MalformedFileException(file, e.getMessage());
        }

        return instances;
    }

    public static List<ActivityInstanceParameter> parseActivityParameters(String[] params) throws MalformedParameterStringException {
        Pattern inputParamPattern = Pattern.compile("(\\S+):(\\S+)=(\\S+)");
        List<ActivityInstanceParameter> parameters = new ArrayList<>();

        for (String param : params) {
            Matcher matcher = inputParamPattern.matcher(param);
            if (matcher.find()) {
                String defaultValue = "";
                String name = matcher.group(1);
                String type = matcher.group(2);
                List<String> range = new ArrayList<String>();
                String value = matcher.group(3);
                parameters.add(new ActivityInstanceParameter(defaultValue, name, range, type, value));
            } else {
                throw new MalformedParameterStringException(param);
            }
        }
        return parameters;
    }

    public static class MalformedFileException extends Exception {
        private String fileName;
        private String reason;

        public MalformedFileException(File file, String reason) {
            this.fileName = file.toString();
            this.reason = reason;
        }

        public String toString() {
            return String.format("Unable to parse file %s. %s", fileName, reason);
        }

        public String getMessage() {
            return this.toString();
        }
    }

    public static class MalformedParameterStringException extends Exception {
        private String param;

        public MalformedParameterStringException(String param) {
            this.param = param;
        }

        public String toString() {
            return String.format("Unexpected format for parameter definition: %s", param);
        }

        public String getMessage() {
            return this.toString();
        }
    }
}
