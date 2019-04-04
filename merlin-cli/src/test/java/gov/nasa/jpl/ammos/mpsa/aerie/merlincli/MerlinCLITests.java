package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstanceParameter;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class MerlinCLITests {

    private String resourcesRoot = "src/test/resources";

    @Test
    public void parseActivityParametersTest() {
        try {
            String name = "name";
            String type = "type";
            String value = "value";
            String[] stringParams = {
                    String.format("%s:%s=%s", name, type, value)
            };
            List<ActivityInstanceParameter> instanceParams = CommandOptions.parseActivityParameters(stringParams);
            assertTrue("Parameter lists differ in length",instanceParams.size() == stringParams.length);
            assertTrue("Parsed parameter name mismatch", instanceParams.get(0).getName().equals(name));
            assertTrue("Parsed parameter type mismatch", instanceParams.get(0).getType().equals(type));
            assertTrue("Parsed parameter value mismatch", instanceParams.get(0).getValue().equals(value));
        } catch (CommandOptions.MalformedParameterStringException e) {
            assertTrue("Valid activity parameter parsing failed.",false);
        }

        try {
            String name3 = "color";
            String type3 = "string";
            String value3 = "red";
            String[] stringParams = {
                    "param:type=value",
                    "param2:type2=value2",
                    String.format("%s:%s=%s", name3, type3, value3),
                    "start:time=2019-087T03:45:12.425"
            };
            List<ActivityInstanceParameter> instanceParams = CommandOptions.parseActivityParameters(stringParams);
            assertTrue("Parameter lists differ in length",instanceParams.size() == stringParams.length);
            assertTrue("Parsed parameter name mismatch", instanceParams.get(2).getName().equals(name3));
            assertTrue("Parsed parameter type mismatch", instanceParams.get(2).getType().equals(type3));
            assertTrue("Parsed parameter value mismatch", instanceParams.get(2).getValue().equals(value3));
        } catch (CommandOptions.MalformedParameterStringException e) {
            assertTrue("Valid activity parameter parsing failed.",false);
        }

        try {
            String[] params = {
                    "name:type:value"
            };
            CommandOptions.parseActivityParameters(params);
            assertTrue(String.format("Invalid string \"%s\" was parsed without error", params[0]), false);
        } catch (CommandOptions.MalformedParameterStringException e) {
            // We expected that error!
        }

        String invalidParam = "unspecified";
        try {
            invalidParam = "name3:type3:value3";
            String[] params = {
                    "name1:type1=value1",
                    "name2:type2=value2",
                    invalidParam,
                    "name4:type4=value4"
            };
            CommandOptions.parseActivityParameters(params);
            assertTrue(String.format("Invalid string \"%s\" was parsed without error", params[2]), false);
        } catch (CommandOptions.MalformedParameterStringException e) {
            assertTrue("Unexpected parameter parsing failure.", e.getMessage().contains(invalidParam));
        }
    }

}
