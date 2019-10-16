package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class MerlinCLIApfConversionTests {

    private final Path testPath = Path.of("test_file.json");

    // Used to intercept System.out and System.err
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setup() throws IOException {
        Files.deleteIfExists(testPath);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanup() throws IOException {
        Files.deleteIfExists(testPath);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }


    @Test
    public void testConvertValidPlan() {
        assertFalse(Files.exists(testPath));

        String[] args = { "--convert-apf", "src/test/resources/apgen/apf_files/Banana.apf", testPath.toString(),  "src/test/resources/apgen/aaf_files"};
        CommandOptions commandOptions = new CommandOptions();
        commandOptions.consumeArgs(args).parse();

        assertTrue(commandOptions.lastCommandSuccessful());

        String output = outContent.toString();
        assertTrue(output.contains("SUCCESS"));

        // TODO: Checking the actual file contents would be better, but at least we know it was created
        assertTrue(Files.exists(testPath));
    }
}