package gov.nasa.jpl.ammos.mpsa.apgen.parser.utilities;

import org.junit.Test;

import static gov.nasa.jpl.ammos.mpsa.apgen.parser.utilities.ParsingUtilities.removeComment;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ParsingUtilitiesTests {

    @Test
    public void testRemoveComment() {
        assertThat(removeComment("/")).isEqualTo("/");
        assertThat(removeComment("#")).isEqualTo("");
        assertThat(removeComment("//")).isEqualTo("");
        assertThat(removeComment("///")).isEqualTo("");
        assertThat(removeComment("//# test")).isEqualTo("");
        assertThat(removeComment("#// test")).isEqualTo("");
        assertThat(removeComment("")).isEqualTo("");
        assertThat(removeComment("test")).isEqualTo("test");
        assertThat(removeComment("test # comment")).isEqualTo("test ");
        assertThat(removeComment("test // comment")).isEqualTo("test ");
        assertThat(removeComment("test 4 / 2 // comment")).isEqualTo("test 4 / 2 ");
        assertThat(removeComment("test \"#\"# comment")).isEqualTo("test \"#\"");
        assertThat(removeComment("test \"//\"// comment")).isEqualTo("test \"//\"");
        assertThat(removeComment("\"Hello world!\" # test")).isEqualTo("\"Hello world!\" ");
        assertThat(removeComment("\"Hello # world!\" # test")).isEqualTo("\"Hello # world!\" ");
        assertThat(removeComment("\"Hello // world!\" // test")).isEqualTo("\"Hello // world!\" ");
        assertThat(removeComment("\"\\\\\"// test \\\"// test")).isEqualTo("\"\\\\\"");
    }
}
