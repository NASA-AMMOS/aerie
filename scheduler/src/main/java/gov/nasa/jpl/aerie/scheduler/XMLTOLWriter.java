package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/**
 * serializes output for use in an xml tol file, eg for raven input
 */
//TODO: surely there is already an java implementation of this somewhere
public class XMLTOLWriter {

  /**
   * configure a new xml tol writer
   *
   * @param config IN the controlling configuration for the output operations
   */
  public XMLTOLWriter(HuginnConfiguration config) {
    this.config = config;
    try {
      this.xml = new java.io.PrintStream(config.getOutputStem() + "_tol.xml");
    } catch (java.io.FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * the configuration that controls the serializer
   */
  private HuginnConfiguration config;

  /**
   * stream to serialize output to
   */
  private java.io.PrintStream xml;

  /**
   * serialize the entire plan to xml tol output
   *
   * @param plan IN the plan to serialize to configured output stream
   */
  public void write(Plan plan) {

    //xml/xmltol header info
    xml.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xml.println("<XML_TOL>");

    //output time-ordered TOLrecord entries
    final var recordMap = collectRecords(plan);
    for (final var recordList : recordMap.values()) {
      for (final var record : recordList) {
        record.write();
      }
    }

    //xml/xmltol footer info
    xml.println("</XML_TOL>");
  }

  /**
   * collect all the activity records that should be output from the plan
   *
   * the records consist of start and end markers for each activity instance,
   * as expected by raven and other tools
   *
   * the records must be collected in a time-indexed container because the
   * TOL format requires time-ordered output
   *
   * @param plan IN the plan of activity instances to create records for
   * @return an time-indexed container of all the activity records from
   *     the input plan
   */
  //TODO: use some multimap library eg guava
  private java.util.TreeMap<Time, java.util.List<TolRecord>> collectRecords(Plan plan) {
    final var timedLists = new java.util.TreeMap<Time, java.util.List<TolRecord>>();
    for (final var act : plan.getActivitiesByTime()) {
      addRecord(new ActivityStartRecord(act), timedLists);
      addRecord(new ActivityEndRecord(act), timedLists);
    }
    return timedLists;
  }

  /**
   * add a single record to a given time-indexed container
   *
   * @param record IN the new record to add to the container
   * @param timedLists IN the time-indexed container to add the record to
   */
  //TODO: use some multimap library eg guava
  private void addRecord(TolRecord record, java.util.TreeMap<Time, java.util.List<TolRecord>> timedLists) {
    final var t = record.getTime();
    var list = timedLists.get(t);
    if (list == null) {
      timedLists.put(t, list = new java.util.LinkedList<TolRecord>());
    }
    list.add(record);
  }

  /**
   * small class representing an individual TOL entry for serialization
   */
  private abstract class TolRecord {

    /**
     * the plan time at which the record is relevant
     */
    private Time time;

    /**
     * create a new record at the given time
     *
     * @param time IN the plan time at which the record is relevant
     */
    TolRecord(Time time) {
      this.time = time;
    }

    /**
     * fetch the string representing the type of record, eg ACT_START
     *
     * individual record types override this method
     */
    public abstract String getTypeString();

    /**
     * serialize the record to the current position in xml output
     */
    public abstract void write();

    /**
     * fetch the plan time at which the record is relevant
     *
     * @return the plan time at which the record is relevant
     */
    public Time getTime() {
      return time;
    }

    /**
     * write the generic leading portion of the tol record to output
     */
    public void writeHeader() {
      xml.println("  <TOLrecord type=\"" + getTypeString() + "\">");
      xml.println("    <TimeStamp>" + getTime() + "</TimeStamp>");
    }

    /**
     * write the generic trailing portion of the tol record to output
     */
    public void writeFooter() {
      xml.println("  </TOLrecord>");
    }
  }

  /**
   * a tol record that details an activity-related event
   */
  private abstract class ActivityRecord extends TolRecord {

    /**
     * create a new record at the given time relating to given activity
     *
     * @param time IN the plan time at which the recorded event occured
     * @param act IN the activity that is being described by the record
     */
    public ActivityRecord(Time time, ActivityInstance act) {
      super(time);
      this.act = act;
    }

    /**
     * the activity that is being described by the record
     */
    private ActivityInstance act;

    /**
     * fetches the activity that is being described by the record
     *
     * @return the activity that is being described by the record
     */
    public ActivityInstance getAct() {
      return act;
    }
  }

  /**
   * a tol record for the beginning of an activity
   */
  private class ActivityStartRecord extends ActivityRecord {
    /**
     * create a tol record for the beginning of the given activity
     *
     * uses the activity's start time as the tol record time
     *
     * @param act the activity that is described by the record
     */
    public ActivityStartRecord(ActivityInstance act) {
      super(config.getHorizon().toTime(act.getStartTime()), act);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeString() { return "ACT_START"; }

    /**
     * {@inheritDoc}
     *
     * outputs the details of the activity, including attributes (like display
     * color!) and parameter values
     */
    @Override
    public void write() {
      writeHeader();
      final var act = getAct();
      final var dur = act.getDuration() == null ? Duration.of(1, Duration.SECONDS) : act.getDuration();
      xml.println("    <Instance>");
      xml.println("      <ID>" + act.getId() + "</ID>");
      xml.println("        <Name>" + act.getType().getName() + "</Name>");
      xml.println("        <Type>" + act.getType().getName() + "</Type>");
      xml.println("        <Parent></Parent>");
      xml.println("        <Visibility>visible</Visibility>");
      xml.println("        <Attributes>");
      xml.println("            <Attribute>");
      xml.println("                <Name>start</Name>");
      xml.println("                <TimeValue milliseconds=\"" + config.getHorizon().toTime(act.getStartTime()).toEpochMilliseconds() + "\">" + act
          .getStartTime()
          .toString() + "</TimeValue>");
      xml.println("            </Attribute>");
      xml.println("            <Attribute>");
      xml.println("                <Name>span</Name>");
      xml.println("                <DurationValue milliseconds=\""
                  + dur.in(Duration.MILLISECOND)
                  + "\">"
                  + dur.toString()
                  + "</DurationValue>");
      xml.println("            </Attribute>");
      xml.println("            <Attribute>");
      xml.println("                <Name>subsystem</Name>");
      xml.println("                <StringValue>generic</StringValue>");
      xml.println("            </Attribute>");
      xml.println("            <Attribute>");
      xml.println("              <Name>Color</Name>");
      xml.println("              <StringValue>" + getColor(this) + "</StringValue>");
      xml.println("             </Attribute>");
      xml.println("        </Attributes>");
      xml.println("        <Parameters>");
      xml.println("            <Parameter>");
      xml.println("                <Name>arg1</Name>");
      xml.println("                <DurationValue>" + dur.toString() + "</DurationValue>");
      xml.println("            </Parameter>");
      xml.println("            <Parameter>");
      xml.println("                <Name>arg2</Name>");
      xml.println("                <StringValue>" + act.getType().getName() + "</StringValue>");
      xml.println("            </Parameter>");
      xml.println("        </Parameters>");
      xml.println("    </Instance>");
      writeFooter();
    }
  }

  /**
   * a tol record for the end of an activity
   */
  private class ActivityEndRecord extends ActivityRecord {
    /**
     * create a new record reprsenting the end of given activity
     *
     * uses the activity instance's end time as the record time
     *
     * @param act IN the activity to record the end of
     */
    public ActivityEndRecord(ActivityInstance act) {
      super(config.getHorizon().toTime(act.getStartTime().plus(act.getDuration() == null ? Duration.of(1, Duration.SECONDS) : act.getDuration())), act);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeString() { return "ACT_END"; }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write() {
      writeHeader();
      xml.println("    <ActivityID>" + getAct().getId()+ "</ActivityID>");
      writeFooter();
    }
  }

  /**
   * a tol record representing a change in state value
   */
  private class ResourceValue extends TolRecord {
    /**
     * create a new tol record for a value change at given time
     *
     * @param time IN the plan time of the value change
     */
    public ResourceValue(Time time) {
      super(time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeString() { return "RES_VAL"; }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write() {
      writeHeader();
      //TODO: didn't need state output yet
      writeFooter();
    }
  }

  /**
   * create a record for the outgoing final value of a state
   *
   * only one such record is output for each state at the end of the plan
   */
  private class ResourceFinalValue extends ResourceValue {

    /**
     * create a state value record at the given final plan time
     *
     * @param time IN the plan time of the end of the plan
     */
    public ResourceFinalValue(Time time) {
      super(time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTypeString() { return "RES_FINAL_VAL"; }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write() {
      writeHeader();
      //TODO: didn't need state output yet
      writeFooter();
    }
  }

  /**
   * determines the display color that the activity instance should use
   *
   * currently uses a set of regular expression filters on the activity
   * name to determine the color to apply from a configuration map
   *
   * @param record IN the record of the activity to determine color of
   * @return a string represenging the color to use for the activity
   *     in a visualization, as a web hexadecimal color code
   */
  private String getColor(ActivityRecord record) {
    final var act = record.getAct();
    final var name = act.getId();
    final var type = act.getType().getName();
    String color = "#000000"; //black default
    return color;
  }

}
