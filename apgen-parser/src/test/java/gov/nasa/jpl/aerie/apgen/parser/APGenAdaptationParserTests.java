package gov.nasa.jpl.aerie.apgen.parser;

import gov.nasa.jpl.aerie.apgen.exceptions.APGenAdaptationParsingException;
import gov.nasa.jpl.aerie.apgen.model.ActivityType;
import gov.nasa.jpl.aerie.apgen.model.ActivityTypeParameter;
import gov.nasa.jpl.aerie.apgen.model.Adaptation;
import gov.nasa.jpl.aerie.apgen.model.Attribute;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class APGenAdaptationParserTests {
    // TODO: Write more complex adaptations and tests

    @Test
    public void testLoadSimpleAdaptation() throws APGenAdaptationParsingException {
        Path path = resourcePath("/gov/nasa/jpl/aerie/apgen/simple/simple_missionModel.aaf");
        Adaptation adaptation = APGenAdaptationParser.parseFile(path);

        assertThat(adaptation.hasActivityType("Banana")).isTrue();
        assertThat(adaptation.hasActivityType("Apple")).isTrue();
        assertThat(adaptation.hasActivityType("Avocado")).isTrue();

        /* Verify Banana activity, attributes and parameters were loaded */
        ActivityType bananaType = adaptation.getActivityType("Banana");

        assertThat(bananaType.getAttribute("Color")).isNotNull();
        assertThat(bananaType.getAttribute("Duration")).isNotNull();

        Attribute bananaColor = bananaType.getAttribute("Color");
        Attribute bananaDuration = bananaType.getAttribute("Duration");
        assertThat(bananaColor.getValue()).isEqualTo("\"Yellow\"");
        assertThat(bananaDuration.getValue()).isEqualTo("duration");

        assertThat(bananaType.getParameter("duration")).isNotNull();
        ActivityTypeParameter bananaDurationParam = bananaType.getParameter("duration");
        assertThat(bananaDurationParam.getType()).isEqualTo("Duration");
        assertThat(bananaDurationParam.getDefault()).isEqualTo("00:30:00");

        /* Verify Apple activity, attributes and parameters were loaded */
        ActivityType appleType = adaptation.getActivityType("Apple");

        assertThat(appleType.getAttribute("Duration")).isNotNull();
        Attribute appleDuration = appleType.getAttribute("Duration");
        assertThat(appleDuration.getValue()).isEqualTo("01:00:00");

        assertThat(appleType.getParameter("parameters")).isNotNull();
        ActivityTypeParameter appleParameters = appleType.getParameter("parameters");
        assertThat(appleParameters.getType()).isEqualTo("array");
        assertThat(appleParameters.getDefault()).isEqualTo("[\"red\", 3.0, 4]");

        /* Verify Avocado activity, attributes and parameters were loaded */
        ActivityType avocadoType = adaptation.getActivityType("Avocado");

        assertThat(avocadoType.getParameter("softness")).isNotNull();
        assertThat(avocadoType.getParameter("size")).isNotNull();
        assertThat(avocadoType.getParameter("age")).isNotNull();
        assertThat(avocadoType.getParameter("expiration")).isNotNull();

        ActivityTypeParameter avocadoSoftness = avocadoType.getParameter("softness");
        assertThat(avocadoSoftness.getType()).isEqualTo("float");
        assertThat(avocadoSoftness.getDefault()).isEqualTo("1.0");

        ActivityTypeParameter avocadoSize = avocadoType.getParameter("size");
        assertThat(avocadoSize.getType()).isEqualTo("string");
        assertThat(avocadoSize.getDefault()).isEqualTo("\"medium\"");

        ActivityTypeParameter avocadoAge = avocadoType.getParameter("age");
        assertThat(avocadoAge.getType()).isEqualTo("integer");
        assertThat(avocadoAge.getDefault()).isEqualTo("7");

        ActivityTypeParameter avocadoExpiration = avocadoType.getParameter("expiration");
        assertThat(avocadoExpiration.getType()).isEqualTo("Time");
        assertThat(avocadoExpiration.getDefault()).isEqualTo("2019-365T23:59:59");
    }

    private static Path resourcePath(final String path) {
        try {
            return Path.of(ApfParserTests.class.getResource(path).toURI());
        } catch (final URISyntaxException e) {
            throw new Error(e);
        }
    }
}
