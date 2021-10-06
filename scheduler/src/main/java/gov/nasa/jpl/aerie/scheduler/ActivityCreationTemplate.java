package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.scheduler.aerie.AerieActivityInstance;
import gov.nasa.jpl.aerie.scheduler.aerie.AerieActivityType;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.NegativeCycleDetectedException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

/**
 * criteria used to identify create activity instances in scheduling goals
 *
 * the creation template is a partial specification of what is required of
 * an activity instance in order to meet the goal criteria, but also includes
 * additional information on how to create such matching instances in the
 * event that no matches were found
 *
 * for example "an image of at least 10s (or 30s by default) duration taken
 * with green filter"
 *
 * corresponds roughly to the concept of an "activity preset"
 *
 * creation templates may be fluently constructed via builders that parse like
 * first order logic predicate clauses used in building up scheduling rules
 */
public class ActivityCreationTemplate extends ActivityExpression {

  /**
   * ctor is private to prevent inconsistent construction
   *
   * please use the enclosed fluent Builder class instead
   *
   * leaves all criteria fields unspecified
   */
  protected ActivityCreationTemplate() { }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends AbstractBuilder<B, AT>,AT extends ActivityExpression> AbstractBuilder<B,AT> getNewBuilder(){
    return (AbstractBuilder<B, AT>) new Builder();
  }

  public static ActivityCreationTemplate ofType(ActivityType actType){
    var act = new ActivityCreationTemplate();
    act.type = actType;
    return act;
  }


  /**
   * fluent builder class for constructing creation templates
   *
   * each different term added to the builder via method calls become part of
   * a logical conjection, ie matching activities must meet all of the
   * specified criteria
   *
   * existing terms can be replaced by calling the same method again, ie
   * matching activities must only meet the last-specified term
   *
   * if the scheduling algorithm needs to create a new activity instance, it
   * will use either the last-specified default value for the template or, if
   * this template doesn't specify a value, the activity type's own default
   * value
   *
   * creation templates must always specify an activity type (and for now also
   * a duration)
   * //REVIEW: eventually duration should come from simulation instead
   */
  public static class Builder extends AbstractBuilder<Builder,ActivityCreationTemplate> {

    //REVIEW: perhaps separate search criteria vs default specification,
    //        eg Range(0...5) allowed, but create with 4

    /**
     * create activity instances with given default duration
     *
     * @param duration IN STORED the duration of the activity created by
     *        this template. not null
     * @return the same builder object updated with new criteria
     */
    public @NotNull
    Builder duration(@NotNull Duration duration ) {
      this.durationIn = new Range<Duration>(duration, duration);
      return getThis();
    }
    public @NotNull
    Builder duration(@NotNull Range<Duration> duration ) {
      this.durationIn = duration;
      return getThis();
    }

    @Nullable
    Range<Duration> duration; //only null until set!

    @Override
    public Builder basedOn(ActivityCreationTemplate template) {
      type = template.type;
      startsIn = template.startRange;
      endsIn = template.endRange;
      durationIn = template.durationRange;
      startsOrEndsIn = template.startOrEndRange;
      nameMatches = ( template.nameRE != null ) ? template.nameRE.pattern() : null;
      parameters = template.parameters;
      return getThis();
    }

    /**
     * {@inheritDoc}
     */
    public @NotNull
    Builder getThis() {
      return this;
    }

    protected ActivityCreationTemplate fill(ActivityCreationTemplate template){
      template.startRange = startsIn;
      template.endRange  = endsIn;
      template.startOrEndRange = startsOrEndsIn;
      template.nameRE = ( nameMatches != null )
              ? java.util.regex.Pattern.compile(nameMatches) : null;

      template.type = type;
      if(durationIn != null) {
        template.durationRange =durationIn;
      }
      //REVIEW: probably want to store permissible rane separate from creation
      //        default value

      template.parameters = parameters;
      return template;
    }

    /**
     * cross-check all specified terms and construct a creation template
     *
     * creates a new template object based on the conjunction of all of the
     * criteria specified so far in this builder, with creation default
     * values as specified in this builder to override the activity type
     * default values
     *
     * multiple specifications of the same term sequentially overwrite the
     * prior term specification
     *
     * @return a newly constructed activity creation template that either
     *         matches activities meeting the conjunction of criteria
     *         specified or else creates new instances with given defaults
     */
    public ActivityCreationTemplate build() {


      if( type == null ) { throw new IllegalArgumentException(
          "activity creation template requires non-null activity type" ); }
      //if( duration == null ) { throw new IllegalArgumentException(
      //    "activity creation template requires non-null duration range" ); }
      final var template = new ActivityCreationTemplate();
      fill(template);
      return template;
    }

  }

  /**
   * create activity if possible
   * @param name
   * @param windows
   * @return
   */
  public @NotNull
  ActivityInstance createActivity(String name , TimeWindows windows) {
    //REVIEW: how to properly export any flexibility to instance?
    boolean success = false;
    for(var window : windows.getRangeSet()) {
      success = STNProcess(window);
      if(success){
        break;
      }
    }
    if(!success){
      return null;
    }
    var act = createInstanceForReal(name);
    return act;

  }


  private ActivityInstance createInstanceForReal(String name) {
    final ActivityInstance act;
    if (type instanceof AerieActivityType) {
      act = new AerieActivityInstance(name, (AerieActivityType) type);
    } else {
      act = new ActivityInstance(name, type);
    }


    if(startRange != null && durationRange!= null){
      act.setStartTime(tmpSr.getMinimum());
      act.setDuration(tmpDr.getMinimum());
    } else if(endRange != null && durationRange != null){
      act.setStartTime(tmpEr.getMinimum().minus(tmpDr.getMinimum()));
    } else if(startRange!= null && endRange!= null){
      act.setStartTime(tmpSr.getMinimum());
      act.setDuration(tmpEr.getMinimum().minus(tmpSr.getMinimum()));
    } else{
      throw new RuntimeException("ActivityCreationTemplate : Not enough parametrization");
    }

    for(var param: parameters.entrySet()){
      if(param.getValue() instanceof ExternalState){
        @SuppressWarnings("unchecked")
        ExternalState<?> state = (ExternalState<?>) param.getValue();
        act.addParameter(param.getKey(),state.getValueAtTime(act.getStartTime()));
      } else{
        act.addParameter(param.getKey(),param.getValue());
      }
    }
    return act;
  }
  /**
   * generate a new activity instance based on template defaults
   *
   * used by scheduling logic to instance a new activity that can satisfy
   * all of the template criteria in the case another matching activity
   * could not be found
   *
   * uses any defaults specified in the template to override the otherwise
   * prevailing defaults from the activity type itself
   *
   * @param name IN the activity instance identifier to associate to the
   *        newly constructed activity instance
   * @return a newly constructed activity instance with values chosen
   *         according to any specified template criteria
   */
  public @NotNull
  ActivityInstance createActivity(String name ) {
    //REVIEW: how to properly export any flexibility to instance?
    boolean stnCheck = STNProcess(null);
    if(!stnCheck){
      return null;
    }
    var act = createInstanceForReal(name);
    return act;
  }

  Range<Time> tmpSr;
  Range<Time> tmpEr;
  Range<Duration> tmpDr;


  /**
   * Experimental feature that checks consistency of temporal constraints with a STN network
   * This becomes useful and easier to read when many types of constraints can be posted for activities
   * TODO: Make this global, rename, clean
   */
  private boolean STNProcess(Range<Time> interval){


    var sh = TimeWindows.startHorizon.toEpochMilliseconds();
    var eh = TimeWindows.endHorizon.toEpochMilliseconds();



    //need to do something to convert to int...
    var g = GraphTypeBuilder.<String, DefaultWeightedEdge> directed().allowingMultipleEdges(false)
            .allowingSelfLoops(false).edgeClass(DefaultWeightedEdge.class).weighted(true).buildGraph();


    g.addVertex("TS");
    g.addVertex("S");
    g.addVertex("E");

    Range<Time> localSR = startRange;
    Range<Time> localER = endRange;
    if(interval!= null){

      g.addVertex("SI");
      g.addVertex("EI");

      var srMin =interval.getMinimum().toEpochMilliseconds();
      if(srMin < sh){
        srMin = sh;
      }
      var srMax =interval.getMaximum().toEpochMilliseconds();
      if(srMax > eh){
        srMax = eh;
      }
      srMin = srMin-sh;
      srMax = srMax-sh;
      //System.out.println("Interval constraint " + srMin+" "+srMax);

      var edge1 = g.addEdge("TS", "SI");
      g.setEdgeWeight(edge1, srMin);

      var edge2 = g.addEdge("SI", "TS");
      g.setEdgeWeight(edge2, -srMin);


      var edge3 = g.addEdge("TS", "EI");
      g.setEdgeWeight(edge3, srMax);


      var edge4= g.addEdge("EI", "TS");
      g.setEdgeWeight(edge4, -srMax);


      var edge5= g.addEdge("S", "SI");
      g.setEdgeWeight(edge5, 0);


      var edge6= g.addEdge("EI", "E");
      g.setEdgeWeight(edge6, 0);



    }



    if(localSR != null){

      var srMin =localSR.getMinimum().toEpochMilliseconds();
      if(srMin < sh){
        srMin = sh;
      }
      var srMax =localSR.getMaximum().toEpochMilliseconds();
      if(srMax > eh){
        srMax = eh;
      }
      srMin = srMin-sh;
      srMax = srMax-sh;
      //System.out.println(srMin+" "+srMax);



      var edge1 = g.addEdge("TS", "S");
      g.setEdgeWeight(edge1, srMax);

      var edge2 = g.addEdge("S", "TS");
      g.setEdgeWeight(edge2, -srMin);

    }
    if(localER != null){

      var erMin =localER.getMinimum().toEpochMilliseconds();
      if(erMin < sh){
        erMin = sh;
      }
      var erMax =localER.getMaximum().toEpochMilliseconds();
      if(erMax > eh){
        erMax = eh;
      }
      erMin = erMin-sh;
      erMax = erMax-sh;
      //System.out.println(erMin+" "+erMax);


      var edge1 = g.addEdge("TS", "E");
      g.setEdgeWeight(edge1, erMax);

      var edge2 = g.addEdge("E", "TS");
      g.setEdgeWeight(edge2, -erMin);


    }
    if(durationRange!= null) {

      var edge1 = g.addEdge("S", "E");
      g.setEdgeWeight(edge1, (durationRange.getMaximum().toMilliseconds()));

      var edge2 = g.addEdge("E", "S");
      g.setEdgeWeight(edge2, -(durationRange.getMinimum().toMilliseconds()));

    }
    boolean ret = false;
    BellmanFordShortestPath<String, DefaultWeightedEdge> algo=null;
    try {
      algo = new BellmanFordShortestPath<String, DefaultWeightedEdge>(g);
      var a = algo.getPaths("TS");
      ret = true;
    } catch(NegativeCycleDetectedException e){
      //System.out.println("Error ");
    }


    if(!ret){
      return ret;
    }

    if(localER!=null) {

      long val1 = (long) algo.getPathWeight("E", "TS");
      long val2 = (long) algo.getPathWeight("TS", "E");

      var endRange = new Range<Time>(Time.fromMilli((-val1 + sh)), Time.fromMilli((val2 + sh)));
      this.tmpEr = endRange;
      //System.out.println("End range " + endRange);
    }
    if(localSR!=null) {

      long val1 = (long) algo.getPathWeight("S", "TS");
      long val2 = (long) algo.getPathWeight("TS", "S");

      var startRange = new Range<Time>(Time.fromMilli((-val1 + sh)), Time.fromMilli((val2 + sh)));
      this.tmpSr = startRange;
      //System.out.println("Start range " + startRange);

    }
    if(durationRange!=null) {

      long val1 = (long) algo.getPathWeight("E", "S");
      long val2 = (long) algo.getPathWeight("S", "E");

      var durationRange = new Range<Duration>(Duration.fromMillis(-val1), Duration.fromMillis(val2));
      this.tmpDr = durationRange;
      //System.out.println("Duration range " + durationRange);

    }

    return ret;

  }

}
