package gov.nasa.jpl.ammos.mpsa.apgen.parser;

import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.DirectoryNotFoundException;
import gov.nasa.jpl.ammos.mpsa.apgen.model.Adaptation;
import gov.nasa.jpl.ammos.mpsa.apgen.model.ActivityType;
import gov.nasa.jpl.ammos.mpsa.apgen.model.Attribute;
import gov.nasa.jpl.ammos.mpsa.apgen.model.ActivityTypeParameter;
import gov.nasa.jpl.ammos.mpsa.apgen.exceptions.AdaptationParsingException;
import gov.nasa.jpl.ammos.mpsa.apgen.constants.ApgenPatterns;
import gov.nasa.jpl.ammos.mpsa.apgen.parser.utilities.ParsingUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.regex.*;
import java.io.IOException;

public final class AdaptationParser {

    private static enum Mode {
        NONE,
        ACTIVITY,
        ATTRIBUTES,
        PARAMETERS,
        CREATION,
        MODELING,
        DECOMPOSITION,
        NONEXCLUSIVE_DECOMPOSITION,
        RESOURCE_USAGE,
        EXPANSION,
        CONCURRENT_EXPANSION,
        SCHEDULING,
        DESTRUCTION
    }

    public static Adaptation parseDirectory(Path dir) throws DirectoryNotFoundException, AdaptationParsingException {
        return parseFiles(getAdaptationFilePaths(dir));
    }

    public static Adaptation parseFiles(List<Path> files) throws AdaptationParsingException {
        Adaptation adaptation = new Adaptation();

        for (Path file : files) {
            parseFile(file, adaptation);
        }

        return adaptation;
    }

    public static Adaptation parseFile(final Path file) throws AdaptationParsingException {
        Adaptation adaptation = new Adaptation();
        parseFile(file, adaptation);
        return adaptation;
    }

    private static void parseFile(final Path file, final Adaptation adaptation) throws AdaptationParsingException {
        final Scanner scanner;
        try {
            scanner = new Scanner(Files.newBufferedReader(file));
        } catch (IOException e) {
            throw new AdaptationParsingException(file, e.getMessage());
        }

        ActivityType activityType = null;
        Mode mode = Mode.NONE;
        for (int lineNumber = 0; scanner.hasNextLine(); lineNumber++) {
            Matcher m;
            String line = ParsingUtilities.removeComment(scanner.nextLine());

            if ((m = ApgenPatterns.ACTIVITY_TYPE_START_PATTERN.matcher(line)).lookingAt()) {
                if (mode != Mode.NONE) throw new AdaptationParsingException(file, String.format("Encountered unexpected activity type definition on line %d", lineNumber));

                String type = m.group("type");
                activityType = new ActivityType(type);
                mode = Mode.ACTIVITY;

                continue;
            }

            Mode newMode = sectionMatch(line);
            if(mode != Mode.NONE && newMode != null) {
                if (newMode != null) mode = newMode;

                continue;
            }

            if ((m = ApgenPatterns.ACTIVITY_TYPE_END_PATTERN.matcher(line)).lookingAt()) {
                if (activityType == null) throw new AdaptationParsingException(file, String.format("Unexpected end of activity on line %d", lineNumber));

                adaptation.addActivityType(activityType);
                activityType = null;
                mode = Mode.NONE;

                continue;
            }

            if (mode == Mode.ATTRIBUTES && (m = ApgenPatterns.ATTRIBUTE_PATTERN.matcher(line)).lookingAt()) {
                String name = m.group("name");
                String value = m.group("value");

                activityType.addAttribute(new Attribute(name, value));

                continue;
            }

            if (mode == Mode.PARAMETERS && (m = ApgenPatterns.TYPE_PARAMETER_PATTERN.matcher(line)).lookingAt()) {
                String name = m.group("name");
                String type = m.group("type");
                String defaultValue = m.group("default");

                // The TYPE_PARAMETER_PATTERN will not match entire arrays
                if (type.equals("array")) {
                    line = line.substring(m.start("default"));
                    if ((m = ApgenPatterns.ARRAY_PATTERN.matcher(line)).lookingAt()) {
                        defaultValue = m.group("value");
                    } else if ((m = ApgenPatterns.DICT_ARRAY_PATTERN.matcher(line)).lookingAt()) {
                        defaultValue = m.group("value");
                    } else {
                        throw new AdaptationParsingException(file, String.format("Expected array value on line %d.", lineNumber));
                    }
                }

                activityType.addParameter(new ActivityTypeParameter(name, type, defaultValue));

                continue;
            }
        }

        if (mode != Mode.NONE) throw new AdaptationParsingException(file, "Unexpected end of file while parsing adaptation");
    }

    private static Mode sectionMatch(String line) {
        Matcher m;
        if (ApgenPatterns.BEGIN_ATTRIBUTES_PATTERN.matcher(line).lookingAt()) {
            return Mode.ATTRIBUTES;
        } else if (ApgenPatterns.BEGIN_PARAMETERS_PATTERN.matcher(line).lookingAt()) {
            return Mode.PARAMETERS;
        } else if (ApgenPatterns.BEGIN_CREATION_PATTERN.matcher(line).lookingAt()) {
            return Mode.CREATION;
        } else if (ApgenPatterns.BEGIN_MODELING_PATTERN.matcher(line).lookingAt()) {
            return Mode.MODELING;
        } else if (ApgenPatterns.BEGIN_DECOMPOSITION_PATTERN.matcher(line).lookingAt()) {
            return Mode.DECOMPOSITION;
        } else if (ApgenPatterns.BEGIN_NONEXCLUSIVE_DECOMPOSITION_PATTERN.matcher(line).lookingAt()) {
            return Mode.NONEXCLUSIVE_DECOMPOSITION;
        } else if (ApgenPatterns.BEGIN_RESOURCE_USAGE_PATTERN.matcher(line).lookingAt()) {
            return Mode.RESOURCE_USAGE;
        } else if (ApgenPatterns.BEGIN_EXPANSION_PATTERN.matcher(line).lookingAt()) {
            return Mode.EXPANSION;
        } else if (ApgenPatterns.BEGIN_CONCURRENT_EXPANSION_PATTERN.matcher(line).lookingAt()) {
            return Mode.CONCURRENT_EXPANSION;
        } else if (ApgenPatterns.BEGIN_SCHEDULING_PATTERN.matcher(line).lookingAt()) {
            return Mode.SCHEDULING;
        } else if (ApgenPatterns.BEGIN_DESCTRUCTION_PATTERN.matcher(line).lookingAt()) {
            return Mode.DESTRUCTION;
        } else {
            return null;
        }
    }

    private static List<Path> getAdaptationFilePaths(final Path dir) throws DirectoryNotFoundException {
        final List<Path> aafFiles = new ArrayList<>();

        final File file = new File(dir.toString());
        if ( !file.isDirectory() ) throw new DirectoryNotFoundException(dir);

        final File[] listing = file.listFiles();
        for (File f : listing) {
            if (f.getName().endsWith(".aaf")) {
                aafFiles.add(Path.of(f.getAbsolutePath()));
            }
        }

        return aafFiles;
    }
}
