package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * serializes output for use in an aspen mdl/ini file set
 */
public class AspenINIWriter {

  /**
   * configure a new aspen writer
   *
   * @param config IN the controlling configuration for the output operations
   */
  public AspenINIWriter(HuginnConfiguration config) {
    /**
     * the controlling configuration for the serializer
     */
    try {
      this.mdl = new java.io.PrintStream(config.getOutputStem() + ".mdl");
      this.ini = new java.io.PrintStream(config.getOutputStem() + ".ini");
    } catch (java.io.FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * the output stream to use for the aspen model
   */
  private final java.io.PrintStream mdl;

  /**
   * the output stream to use for the aspen initialization
   */
  private final java.io.PrintStream ini;

  /**
   * serialize the entire plan to aspen outputs
   *
   * @param plan IN the plan to serialize to configured output streams
   */
  public void write(Plan plan) {

    //model header info
    mdl.println("model test {");
    mdl.println("  time_scale = minute;");
    mdl.println("  horizon_start = 2024-358T00:00:00;");
    mdl.println("  horizon_duration = 740d;");
    mdl.println("}");

    for (final var actTypeName : collectActTypes(plan)) {
      mdl.println("activity " + actTypeName + " { };");
    }

    for (final var act : plan.getActivitiesByTime()) {
      final var dur = act.getDuration() == null ? Duration.of(1, Duration.SECONDS) : act.getDuration();
      ini.println(act.getType().getName() + " " + act.getId() + " {");
      ini.println("  start_time = " + act.getStartTime().toString() + ";");
      ini.println("  duration = " + dur.in(Duration.MILLISECOND) / 1000 + ";");
      ini.println("};");
    }

  }

  /**
   * find all activity types used in the plan
   *
   * @return the names of all activity types used in the plan
   */
  java.util.Set<String> collectActTypes(Plan plan) {
    final var set = new java.util.TreeSet<String>();
    for (final var act : plan.getActivitiesByTime()) {
      set.add(act.getType().getName());
    }
    return set;
  }


}
