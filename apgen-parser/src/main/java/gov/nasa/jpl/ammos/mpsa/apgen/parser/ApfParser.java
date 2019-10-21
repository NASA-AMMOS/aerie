package gov.nasa.jpl.ammos.mpsa.apgen.parser;

import gov.nasa.jpl.ammos.mpsa.apgen.constants.ApgenPatterns;
import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.PlanParsingException;
import gov.nasa.jpl.ammos.mpsa.apgen.model.*;
import gov.nasa.jpl.ammos.mpsa.apgen.parser.utilities.ParsingUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

public final class ApfParser {

    private enum Mode {
        ACTIVITY,
        ATTRIBUTES,
        BEGIN_PARAMETERS,
        PARAMETERS
    }

    public static Plan parseFiles(List<Path> files, Adaptation adaptation) throws PlanParsingException {
        Plan plan = new Plan();

        for (Path file : files) {
            parseFile(file, plan, adaptation);
        }

        return plan;
    }

    public static Plan parseFile(Path file, Adaptation adaptation) throws PlanParsingException {
        Plan plan = new Plan();
        parseFile(file, plan, adaptation);
        return plan;
    }

    private static void parseFile(Path file, Plan plan, Adaptation adaptation) throws PlanParsingException {
        final Scanner scanner;
        try {
            scanner = new Scanner(Files.newBufferedReader(file));
        } catch (IOException e) {
            throw new PlanParsingException(file, e.getMessage());
        }

        Matcher m = null;
        ActivityInstance activityInstance = null;
        ActivityType activityType = null;
        int parameterIndex = 0;
        Mode mode = null;
        for (int lineNumber = 0; scanner.hasNextLine(); lineNumber++) {
            String line = ParsingUtilities.removeComment(scanner.nextLine());

            if ((m = ApgenPatterns.ACTIVITY_INSTANCE_START_PATTERN.matcher(line)).lookingAt()) {
                if (mode != null) throw new PlanParsingException(file, String.format("Encountered unexpected activity instance start on line %d", lineNumber));

                String name = m.group("name");
                String type = m.group("type");
                String id = m.group("id");
                activityInstance = new ActivityInstance(type, name, id);
                mode = Mode.ACTIVITY;

                activityType = adaptation.getActivityType(type);
                if (activityType == null) {
                    throw new PlanParsingException(file, String.format("Encountered unknown activity type %s on line %d", type, lineNumber));
                }

            } else if (mode != null && (m = ApgenPatterns.BEGIN_ATTRIBUTES_PATTERN.matcher(line)).lookingAt()) {
                mode = Mode.ATTRIBUTES;

            } else if (mode != null && (m = ApgenPatterns.BEGIN_PARAMETERS_PATTERN.matcher(line)).lookingAt()) {
                line = line.substring(m.end(1));
                if (ApgenPatterns.INSTANCE_PARAMETER_LIST_START_PATTERN.matcher(line).lookingAt()) {
                    mode = Mode.PARAMETERS;
                }
                else {
                    mode = Mode.BEGIN_PARAMETERS;
                }

            } else if (mode == Mode.BEGIN_PARAMETERS && (m = ApgenPatterns.INSTANCE_PARAMETER_LIST_START_PATTERN.matcher(line)).lookingAt()) {
                mode = Mode.PARAMETERS;
                parameterIndex = 0;

            } else if (mode == Mode.PARAMETERS && (m = ApgenPatterns.INSTANCE_PARAMETER_LIST_END_PATTERN.matcher(line)).lookingAt()) {

                if (parameterIndex != activityType.getParameters().size()) {
                    throw new PlanParsingException(file, String.format("Activity instance of type %s contains too few parameters on line %d", activityType.getName(), lineNumber));
                }

                mode = Mode.ACTIVITY;
                parameterIndex = 0;

            } else if ((m = ApgenPatterns.ACTIVITY_INSTANCE_END_PATTERN.matcher(line)).lookingAt()) {
                if (activityInstance == null || mode == Mode.PARAMETERS){
                    throw new PlanParsingException(file, String.format("Encountered unexpected activity instance end on line %d", lineNumber));
                }

                plan.addActivityInstance(activityInstance);
                activityInstance = null;
                activityType = null;
                mode = null;

            } else if (mode == Mode.ATTRIBUTES && (m = ApgenPatterns.ATTRIBUTE_PATTERN.matcher(line)).lookingAt()) {
                String name = m.group("name");
                String value = m.group("value");

                activityInstance.addAttribute(new Attribute(name, value));

            } else if (mode == Mode.PARAMETERS) {
                String value;
                String activityTypeName = activityInstance.getType();
                if (lineNumber > 4780)
                    System.out.println("");
                if ((m = ApgenPatterns.INSTANCE_PARAMETER_PATTERN.matcher(line)).lookingAt()) {
                    value = m.group("value");

                } else if ((m = ApgenPatterns.ARRAY_PATTERN.matcher(line)).lookingAt()) {
                    value = m.group("value");

                } else if ((m = ApgenPatterns.DICT_ARRAY_PATTERN.matcher(line)).lookingAt()) {
                    value = m.group("value");
                } else {
                    value = null;
                }

                if (value != null) {

                    List<ActivityTypeParameter> parameterList = activityType.getParameters();
                    if (parameterIndex >= parameterList.size()) {
                        throw new PlanParsingException(file, String.format("Too many parameters specified for activity instance of type %s on line %d", activityTypeName, lineNumber));
                    }

                    ActivityTypeParameter parameter = parameterList.get(parameterIndex++);
                    String name = parameter.getName();
                    String type = parameter.getType();

                    activityInstance.addParameter(new ActivityInstanceParameter(name, type, value));
                }
            }
        }

        if (mode != null) throw new PlanParsingException(file, "Unexpected end of file while parsing plan");
    }
}
