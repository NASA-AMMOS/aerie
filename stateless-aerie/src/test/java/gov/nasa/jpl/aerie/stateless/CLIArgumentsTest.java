package gov.nasa.jpl.aerie.stateless;

import gov.nasa.jpl.aerie.stateless.utils.BlockExitSecurityManager;
import gov.nasa.jpl.aerie.stateless.utils.SystemExit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CLIArgumentsTest {
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;
  private PrintStream outputStream;
  private PrintStream errorStream;

  @BeforeEach
  void beforeEach() {
    // Redirect System streams to buffers that can be examined
    out = new ByteArrayOutputStream();
    err = new ByteArrayOutputStream();
    outputStream = new PrintStream(out);
    errorStream = new PrintStream(err);

    System.setOut(outputStream);
    System.setErr(errorStream);
  }

  @AfterEach
  void afterEach() {
    outputStream.close();
    errorStream.close();
  }

  /**
   * Top level help text only displays if:
   *  - the first argument is '-h' or '--help'
   *  - no arguments are passed
   *  - a nonexistent subcommand is passed
   */
  @Test
  void topLevelHelp() {
    final String helpString =
    """
    usage: stateless-aerie COMMAND [ARGS]...

    Available commands:
     - simulate: Simulate a plan using the specified model and configuration

    Stateless Aerie v""";

    final var validArgs = new String[][]{{"-h"}, {"--help"}, {}, {"fakeCommand"},
                                         {"-h", "simulate"}, {"--help", "simulate"},
                                         {"-h", "--help"}, {"--help", "-h"}};
    for(final var args : validArgs) {
      Main.main(args);
      outputStream.flush();
      assertTrue(out.toString().contains(helpString));
      assertTrue(err.toString().isBlank());
      out.reset();
      err.reset();
    }

    // '-h' or '--help' as the second argument does not display the top-level help string
    final var otherArgs = new String[][]{{"simulate", "-h"}, {"simulate", "--help"}};
    BlockExitSecurityManager.install();
    for(final var args : otherArgs) {
      try {
        Main.main(args);
      } catch (SystemExit se) {
        assertEquals(0, se.getStatusCode());
      }

      outputStream.flush();
      assertFalse(out.toString().contains(helpString));
      assertTrue(err.toString().isBlank());
      out.reset();
      err.reset();
    }
    BlockExitSecurityManager.uninstall();
  }

  @Nested
  public class SimulationArguments {
    /**
     * Subcommand help message appears if the '-h' or '--help' flag is passed after the subcommand,
     * regardless of presence of other arguments or position.
     */
    @Test
    void simulationHelp() {
      final var helpString =
       """
       usage: stateless-aerie simulate [-f <arg>] [-h] [-i <arg>] -m <arg> -p
              <arg> [-s <arg>] [-v]
       Simulate a plan using the specified model and configuration
        -f,--file <arg>              output file path
        -h,--help                    display this message and exit
        -i,--update_interval <arg>   minimum interval that simulation extent
                                     updates are posted, in milliseconds
        -m,--model <arg>             path to model jar
        -p,--plan <arg>              path to plan json
        -s,--sim_config <arg>        path to simulation configuration json
        -v,--verbose                 verbosity of simulation

       Stateless Aerie v""";

      final var helpArgs = new String[][] {{"simulate", "-h"}, {"simulate", "--help"},
                                           {"simulate", "-p", "foo plan.json", "-h"},
                                           {"simulate", "-i", "1000", "--help"}};

      BlockExitSecurityManager.install();
      for (final var args : helpArgs) {
        try {
          Main.main(args);
        } catch (SystemExit se) {
          assertEquals(0, se.getStatusCode());
        }

        outputStream.flush();
        assertTrue(out.toString().contains(helpString));
        assertTrue(err.toString().isBlank());
        out.reset();
        err.reset();
      }
      BlockExitSecurityManager.uninstall();
    }

    /**
     * When verbose is on, progress is reported prior to simulation results.
     */
    @Test
    void verboseOn() throws IOException {
      Main.main(new String[]{"simulate",
                             "-m", "../examples/foo-missionmodel/build/libs/foo-missionmodel.jar",
                             "-p", "src/test/resources/simpleFooPlan.json",
                             // Extent interval cranked way up to guarantee no extent is printed (5000s)
                             "-i", "5000000000",
                             "--verbose"});
      outputStream.flush();
      try(final var reader = new BufferedReader(new FileReader("src/test/resources/simpleFooPlanResults.json"))) {
        final var fileLines = reader.lines().toList();
        final var outputLines = out.toString().split("\n");

        assertEquals(fileLines.size() + 4, outputLines.length);

        assertEquals("Parsing plan src/test/resources/simpleFooPlan.json...", outputLines[0]);
        assertEquals("Loading mission model ../examples/foo-missionmodel/build/libs/foo-missionmodel.jar...", outputLines[1]);
        assertEquals("Simulating Plan...", outputLines[2]);
        assertEquals("Writing Results...", outputLines[3]);
        for(int i = 0; i < fileLines.size(); ++i) {
         assertEquals(fileLines.get(i), outputLines[i+4]);
        }
      }
    }

    /**
     * When verbose is off, only simulation results are output.
     */
    @Test
    void verboseOff() throws IOException {
      Main.main(new String[]{"simulate",
                             "-m", "../examples/foo-missionmodel/build/libs/foo-missionmodel.jar",
                             "-p", "src/test/resources/simpleFooPlan.json"});
      outputStream.flush();
      try(final var reader = new BufferedReader(new FileReader("src/test/resources/simpleFooPlanResults.json"))) {
        final var fileLines = reader.lines().toList();
        final var outputLines = out.toString().split("\n");

        assertEquals(fileLines.size(), outputLines.length);
        for(int i = 0; i < fileLines.size(); ++i) {
         assertEquals(fileLines.get(i), outputLines[i]);
        }
      }
    }
  }
}
