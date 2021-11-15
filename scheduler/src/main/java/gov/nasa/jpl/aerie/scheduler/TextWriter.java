package gov.nasa.jpl.aerie.scheduler;

/**
 * serializes output for use in an simple human "readable" text report
 */
public class TextWriter {

  /**
   * configure a new report writer
   *
   * @param config IN the controlling configuration for the output operations
   */
  public TextWriter(HuginnConfiguration config) {
    this.config = config;
    try {
      this.txt = new java.io.PrintStream(config.getOutputStem() + "_report.txt");
    } catch (java.io.FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * the controlling configuration for the writer
   */
  private HuginnConfiguration config;


  /**
   * the stream to serialize output to
   */
  private java.io.PrintStream txt;


  /**
   * serialize the entire plan to xml tol output
   *
   * @param plan IN the plan to serialize to configured output stream
   */
  public void write(Plan plan) {

    //TODO: split into sub-methods

    txt.println("Evaluation:");
    for (final var eval : plan.getEvaluations()) {
      for (final var goal : eval.getGoals()) {
        final var goalEval = eval.forGoal(goal);
        txt.println(goal.getName() + " = " + goalEval.getScore());
        for (final var act : goalEval.getAssociatedActivities()) {
          final var name = shortenName(act.getName());
          final var type = act.getType().getName();

          txt.println("  " + type + "  " + name);
        }
      }
    }

    txt.println();
    txt.println("Schedule:");
    //for now a simple time ordered listing of scheduled activity instances
    for (final var act : plan.getActivitiesByTime()) {

      final var name = shortenName(act.getName());
      final var type = act.getType().getName();
      final var t_start = act.getStartTime();
      final var dur = act.getDuration();

      //for now very simple console display
      txt.println("" + t_start + " +" + dur
                  + " " + type + "  \"" + name + "\"");

    }//for(act)

  }

  /**
   * compress the provided name so it will produce more readable output
   *
   * the first and last several characters of the name are retained
   *
   * if the name is already short enough, it is returned unmolested
   *
   * @param s IN the name to shorten
   * @return the input name itself if already short enough, otherwise an
   *     elided form of the name, including at least a few characters from
   *     the beginning and end
   */
  private static String shortenName(String s) {
    //compact names to avoid super long lines
    if (s.length() > 25) {
      return s.substring(0, 14) + "..." + s.substring(s.length() - 8);
    } else {
      return s;
    }
  }


}
