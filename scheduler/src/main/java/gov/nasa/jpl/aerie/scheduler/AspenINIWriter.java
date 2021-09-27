package gov.nasa.jpl.aerie.scheduler;

/**
 * serializes output for use in an aspen mdl/ini file set
 */
public class AspenINIWriter {

  /**
   * configure a new aspen writer
   *
   * @param config IN the controlling configuration for the output operations
   */
  public AspenINIWriter( HuginnConfiguration config ) {
    this.config = config;
    try {
      this.mdl = new java.io.PrintStream( config.getOutputStem() + ".mdl" );
      this.ini = new java.io.PrintStream( config.getOutputStem() + ".ini" );
    } catch( java.io.FileNotFoundException e ) {
      throw new RuntimeException( e );
    }
  }

  /**
   * the controlling configuration for the serializer
   */
  private HuginnConfiguration config;

  /**
   * the output stream to use for the aspen model
   */
  private java.io.PrintStream mdl;

  /**
   * the output stream to use for the aspen initialization
   */
  private java.io.PrintStream ini;

  /**
   * serialize the entire plan to aspen outputs
   *
   * @param plan IN the plan to serialize to configured output streams
   */
  public void write( Plan plan ) {

    //model header info
    mdl.println("model test {");
    mdl.println("  time_scale = minute;");
    mdl.println("  horizon_start = 2024-358T00:00:00;");
    mdl.println("  horizon_duration = 740d;");
    mdl.println("}");

    for( final var actTypeName : collectActTypes(plan) ) {
      mdl.println("activity "+actTypeName+" { };");
    }

    for( final var act : plan.getActivitiesByTime() ) {
      final var dur = act.getDuration()==null?Duration.ofSeconds(1):act.getDuration();
      ini.println(act.getType().getName()+" "+act.getName().replace('-','_')+" {");
      ini.println("  start_time = " + act.getStartTime().toString() +";");
      ini.println("  duration = " + dur.toMilliseconds()/1000 +";");
      ini.println("};");
    }

  }

  /**
   * find all activity types used in the plan
   *
   * @return the names of all activity types used in the plan
   */
  java.util.Set<String> collectActTypes( Plan plan ) {
    final var set = new java.util.TreeSet<String>();
    for( final var act : plan.getActivitiesByTime() ) {
      set.add( act.getType().getName() );
    }
    return set;
  }


}
