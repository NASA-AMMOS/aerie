package gov.nasa.jpl.aerie.scheduler;

/**
 * configuration of the scheduling engine/library
 *
 * bundles together the various control parameters that guide the execution
 * behavior of the schedulers
 */
public class HuginnConfiguration {

  /**
   * use all default configuration options
   */
  public HuginnConfiguration() {}

  /**
   * parse the provided command line options to modulate configuration
   *
   * @param args IN the command line arguments to the demo
   */
  public HuginnConfiguration(String[] args) {
    //TODO use a real argument processing library, eg apache commons-cli
  }

  /**
   * span of time over which the scheduler should run
   */
  private PlanningHorizon horizon = new PlanningHorizon(Time.fromString("2025-001T00:00:00.000"),
                                                        Time.fromString("2034-001T00:00:00.000"));
  public PlanningHorizon getHorizon() { return horizon; }

  public void setHorizon(PlanningHorizon h) { horizon = h; }

  /**
   * leading filename stem used to create and distinguish various output files
   *
   * matches the demo scenario name provided at runtime
   */
  private String outputStem = "huginn";

  public String getOutputStem() { return outputStem; }

  public void setOutputStem(String stem) { outputStem = stem; }

  /**
   * colors to apply (in xml-tol) to activities with matching names
   *
   * maps from name regex pattern to hex color code
   *
   * order is important: the first matched rule applies
   */
  //TODO: reconsider how color options are handled between core/client
  private java.util.LinkedHashMap<String, String> actNameColorMap = new java.util.LinkedHashMap<>() {{
    put(".*", "#000000"); //black else
  }};

  public java.util.LinkedHashMap<String, String> getActColorMap() { return actNameColorMap; }

  public void setActColorMap(java.util.LinkedHashMap<String, String> colors) { actNameColorMap = colors; }

}
