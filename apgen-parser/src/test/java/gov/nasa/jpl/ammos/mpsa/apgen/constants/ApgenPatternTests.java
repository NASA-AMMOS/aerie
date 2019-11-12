package gov.nasa.jpl.ammos.mpsa.apgen.constants;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApgenPatternTests {


    @Test
    public void testActivityTypeStartPattern() {
        final String line = "activity type BiteBanana";
        final Matcher m = ApgenPatterns.ACTIVITY_TYPE_START_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("type")).isEqualTo("BiteBanana");
    }

    @Test
    public void testActivityTypeEndPattern() {
        final String line = "    end activity type BiteBanana";
        final Matcher m = ApgenPatterns.ACTIVITY_TYPE_END_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
    }

    @Test
    public void testAttributesSectionPattern() {
        final String line = "    attributes";
        final Matcher m = ApgenPatterns.BEGIN_ATTRIBUTES_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("attributes");
    }

    @Test
    public void testParametersSectionPattern() {
        final String line = "    parameters";
        final Matcher m = ApgenPatterns.BEGIN_PARAMETERS_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("parameters");
    }

    @Test
    public void testCreationSectionPattern() {
        final String line = "    creation";
        final Matcher m = ApgenPatterns.BEGIN_CREATION_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("creation");
    }

    @Test
    public void testModelingSectionPattern() {
        final String line = "    modeling";
        final Matcher m = ApgenPatterns.BEGIN_MODELING_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("modeling");
    }

    @Test
    public void testDecompositionSectionPattern() {
        final String line = "    decomposition";
        final Matcher m = ApgenPatterns.BEGIN_DECOMPOSITION_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("decomposition");
    }

    @Test
    public void testNonexclusiveDecompositionSectionPattern() {
        final String line = "    nonexclusive_decomposition";
        final Matcher m = ApgenPatterns.BEGIN_NONEXCLUSIVE_DECOMPOSITION_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("nonexclusive_decomposition");
    }

    @Test
    public void testResourceUsageSectionPattern() {
        final String line = "    resource usage";
        final Matcher m = ApgenPatterns.BEGIN_RESOURCE_USAGE_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("resource usage");
    }

    @Test
    public void testExpansionSectionPattern() {
        final String line = "    expansion";
        final Matcher m = ApgenPatterns.BEGIN_EXPANSION_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("expansion");
    }

    @Test
    public void testConcurrentExpansionSectionPattern() {
        final String line = "    concurrent_expansion";
        final Matcher m = ApgenPatterns.BEGIN_CONCURRENT_EXPANSION_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("concurrent_expansion");
    }

    @Test
    public void testSchedulingSectionPattern() {
        final String line = "    scheduling";
        Matcher m = ApgenPatterns.BEGIN_SCHEDULING_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("scheduling");
    }

    @Test
    public void testDestructionSectionPattern() {
        final String line = "    destruction";
        Matcher m = ApgenPatterns.BEGIN_DESCTRUCTION_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("destruction");
    }

    @Test
    public void testAttributeValuePatternOnTime() {
        final String line = "   \"Start\"   =  2018-331T04:00:00;";
        Matcher m = ApgenPatterns.ATTRIBUTE_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("Start");
        assertThat(m.group("value")).isEqualTo("2018-331T04:00:00");
    }

    @Test
    public void testAttributeValuePatternOnDuration() {
        final String line = "   \"Start\"   =  04:00:00;";
        final Matcher m = ApgenPatterns.ATTRIBUTE_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("Start");
        assertThat(m.group("value")).isEqualTo("04:00:00");
    }

    @Test
    public void testAttributeValuePatternOnNumber() {
        final String line = "   \"Number\"   =  4.294;";
        final Matcher m = ApgenPatterns.ATTRIBUTE_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("Number");
        assertThat(m.group("value")).isEqualTo("4.294");
    }

    @Test
    public void testAttributeValuePatternOnString() {
        final String line = "\"Attribute\"=\"Value\";";
        final Matcher m = ApgenPatterns.ATTRIBUTE_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("Attribute");
        assertThat(m.group("value")).isEqualTo("\"Value\"");
    }

    @Test
    public void testParameterValuePatternOnTime() {
        final String line = "    2018-331T04:00:00.0000,";
        final Matcher m = ApgenPatterns.INSTANCE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("value")).isEqualTo("2018-331T04:00:00.0000");
    }

    @Test
    public void testParameterValuePatternOnDuration() {
        final String line = "    04:00:00.0000,";
        final Matcher m = ApgenPatterns.INSTANCE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("value")).isEqualTo("04:00:00.0000");
    }

    @Test
    public void testParameterValuePatternOnNumber() {
        final String line = "            4.284,";
        final Matcher m = ApgenPatterns.INSTANCE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("value")).isEqualTo("4.284");
    }

    @Test
    public void testParameterValuePatternOnString() {
        final String line = "          \"\"";
        final Matcher m = ApgenPatterns.INSTANCE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("value")).isEqualTo("\"\"");
    }

    @Test
    public void testTypeParameterPatternOnTime() {
        final String line = "    par: Time default to 2018-331T04:00:00;";
        final Matcher m = ApgenPatterns.TYPE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("par");
        assertThat(m.group("type")).isEqualTo("Time");
        assertThat(m.group("default")).isEqualTo("2018-331T04:00:00");
    }

    @Test
    public void testTypeParameterPatternOnDuration() {
        final String line = "    dur: Duration default to 06:03:30.034;";
        final Matcher m = ApgenPatterns.TYPE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("dur");
        assertThat(m.group("type")).isEqualTo("Duration");
        assertThat(m.group("default")).isEqualTo("06:03:30.034");
    }

    @Test
    public void testTypeParameterPatternOnNumber() {
        final String line = "    num: float default to 0.46;";
        final Matcher m = ApgenPatterns.TYPE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("num");
        assertThat(m.group("type")).isEqualTo("float");
        assertThat(m.group("default")).isEqualTo("0.46");
    }

    @Test
    public void testTypeParameterPatternOnString() {
        final String line = "    str: string default to \" test \\\" \";";
        final Matcher m = ApgenPatterns.TYPE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("str");
        assertThat(m.group("type")).isEqualTo("string");
        assertThat(m.group("default")).isEqualTo("\" test \\\" \"");
    }

    @Test
    public void testTypeParameterPatternOnLocal() {
        final String line = "    num: local integer default to 3;";
        final Matcher m = ApgenPatterns.TYPE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("num");
        assertThat(m.group("modifier")).isEqualTo("local");
        assertThat(m.group("type")).isEqualTo("integer");
        assertThat(m.group("default")).isEqualTo("3");
    }

    @Test
    public void testTypeParameterPatternOnGlobal() {
        final String line = "    num: global integer default to 3;";
        final Matcher m = ApgenPatterns.TYPE_PARAMETER_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("name")).isEqualTo("num");
        assertThat(m.group("modifier")).isEqualTo("global");
        assertThat(m.group("type")).isEqualTo("integer");
        assertThat(m.group("default")).isEqualTo("3");
    }

    @Test
    public void testArrayPattern() {
        final String line = "  [ 1, 2, 3, 4, 5, 6 ] ";
        final Matcher m = ApgenPatterns.ARRAY_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("value")).isEqualTo("[ 1, 2, 3, 4, 5, 6 ]");
    }

    @Test
    public void testArrayPatternWeirdFormatting() {
        final String line = " [1,2,   3         ,4,5,6           ]";
        final Matcher m = ApgenPatterns.ARRAY_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("value")).isEqualTo("[1,2,   3         ,4,5,6           ]");
    }

    @Test
    public void testDictArrayPattern() {
        final String line = " [ \"a\"=\"first param\", \"b\"=2.0, \"c\"=300]";
        final Matcher m = ApgenPatterns.DICT_ARRAY_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("value")).isEqualTo("[ \"a\"=\"first param\", \"b\"=2.0, \"c\"=300]");
    }

    @Test
    public void testDictArrayPatternWeirdFormatting() {
        final String line = "[\"\"    =    \",\",\",\"  = \"\", \"32\"=    32  ]";
        final Matcher m = ApgenPatterns.DICT_ARRAY_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group("value")).isEqualTo("[\"\"    =    \",\",\",\"  = \"\", \"32\"=    32  ]");
    }

    @Test
    public void testInstanceParameterListStartPattern() {
        final String line = "   (";
        final Matcher m = ApgenPatterns.INSTANCE_PARAMETER_LIST_START_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo("(");
    }

    @Test
    public void testInstanceParameterListEndPattern() {
        final String line = "   );";
        final Matcher m = ApgenPatterns.INSTANCE_PARAMETER_LIST_END_PATTERN.matcher(line);
        assertThat(m.lookingAt()).isTrue();
        assertThat(m.group(1)).isEqualTo(");");
    }
}
