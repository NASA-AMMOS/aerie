package gov.nasa.jpl.ammos.mpsa.apgen.constants;

import java.util.regex.Pattern;

public class ApgenPatterns {

    public static final Pattern ACTIVITY_TYPE_START_PATTERN =
            Pattern.compile("^\\s*activity\\s+type\\s+(?<type>[a-zA-z0-9_-]+)");

    public static final Pattern ACTIVITY_TYPE_END_PATTERN =
            Pattern.compile("^\\s*end\\s+activity\\s+type");

    public static final Pattern ACTIVITY_INSTANCE_START_PATTERN =
            Pattern.compile("^\\s*activity\\s+instance\\s+(?<name>[a-zA-z0-9_-]+)\\s+of\\s+type\\s+(?<type>[a-zA-Z0-9_-]+)\\s+id\\s+(?<id>[a-zA-Z0-9_-]+)");

    public static final Pattern ACTIVITY_INSTANCE_END_PATTERN =
            Pattern.compile("\\s*(end\\s+activity\\s+instance)");

    public static final Pattern BEGIN_ATTRIBUTES_PATTERN =
            Pattern.compile("^\\s*(attributes)\\s*$");

    public static final Pattern BEGIN_PARAMETERS_PATTERN =
            Pattern.compile("^\\s*(parameters)\\s*$");

    public static final Pattern BEGIN_CREATION_PATTERN =
            Pattern.compile("^\\s*(creation)\\s*$");

    public static final Pattern BEGIN_MODELING_PATTERN =
            Pattern.compile("^\\s*(modeling)\\s*$");

    public static final Pattern BEGIN_DECOMPOSITION_PATTERN =
            Pattern.compile("^\\s*(decomposition)\\s*$");

    public static final Pattern BEGIN_NONEXCLUSIVE_DECOMPOSITION_PATTERN =
            Pattern.compile("^\\s*(nonexclusive_decomposition)\\s*$");

    public static final Pattern BEGIN_RESOURCE_USAGE_PATTERN =
            Pattern.compile("^\\s*(resource\\s*usage)\\s*$");

    public static final Pattern BEGIN_EXPANSION_PATTERN =
            Pattern.compile("^\\s*(expansion)\\s*$");

    public static final Pattern BEGIN_CONCURRENT_EXPANSION_PATTERN =
            Pattern.compile("^\\s*(concurrent_expansion)\\s*$");

    public static final Pattern BEGIN_SCHEDULING_PATTERN =
            Pattern.compile("^\\s*(scheduling)\\s*$");

    public static final Pattern BEGIN_DESCTRUCTION_PATTERN =
            Pattern.compile("^\\s*(destruction)\\s*$");

    public static final Pattern ATTRIBUTE_PATTERN =
            Pattern.compile("^\\s*\"(?<name>[a-zA-Z0-9_]+)\"\\s*=\\s*(?<value>.*);");

    public static final Pattern TYPE_PARAMETER_PATTERN =
            Pattern.compile("^\\s*(?<name>[a-zA-Z0-9_]+)\\s*:\\s*(?<modifier>(?:local|global)?)\\s*(?<type>[a-zA-Z0-9_]+)\\s+default\\s+to\\s+(?<default>(?:\"(?:[^\"\\\\]|\\\\.)*\")|[^\\s;]+)");

    // FIXME: This will not parse array parameters
    public static final Pattern INSTANCE_PARAMETER_PATTERN =
            Pattern.compile("^\\s*(?<value>(?:\"(?:[^\"\\\\]|\\\\.)*\")|(?:[0-9:T\\-\\.]+)|true|false)", Pattern.CASE_INSENSITIVE);

    public static final Pattern ARRAY_PATTERN =
            Pattern.compile("^\\s*(?<value>\\[(?:\\s*(?:(?:\"(?:[^\"\\\\]|\\\\.)*\")|(?:[0-9:T\\-\\.]+)|true|false)\\s*,?)*\\s*\\])", Pattern.CASE_INSENSITIVE);

    public static final Pattern DICT_ARRAY_PATTERN =
            Pattern.compile("^\\s*(?<value>\\[(?:\\s*\"(?:[^\"\\\\]|\\\\.)*\"\\s*=\\s*(?:(?:\"(?:[^\"\\\\]|\\\\.)*\")|(?:[0-9:T\\-\\.]+)|true|false)\\s*,?)*\\s*\\])", Pattern.CASE_INSENSITIVE);

    public static final Pattern INSTANCE_PARAMETER_LIST_START_PATTERN =
            Pattern.compile("^\\s*(\\()");

    public static final Pattern INSTANCE_PARAMETER_LIST_END_PATTERN =
            Pattern.compile("^\\s*(\\);)");
}
