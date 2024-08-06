package gov.nasa.jpl.aerie.stateless;

import gov.nasa.jpl.aerie.stateless.utils.BlockExitSecurityManager;
import gov.nasa.jpl.aerie.stateless.utils.SystemExit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
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

  }
}
