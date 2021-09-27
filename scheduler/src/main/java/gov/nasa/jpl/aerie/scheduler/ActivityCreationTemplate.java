package gov.nasa.jpl.aerie.scheduler;

import it.univr.di.cstnu.algorithms.STN;
import it.univr.di.cstnu.algorithms.WellDefinitionException;
import it.univr.di.cstnu.graph.LabeledNode;
import it.univr.di.cstnu.graph.STNEdge;
import it.univr.di.cstnu.graph.STNEdgeInt;
import it.univr.di.cstnu.graph.TNGraph;

import java.util.ArrayList;
import java.util.List;

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
  public static class Builder extends ActivityExpression.AbstractBuilder<Builder,ActivityCreationTemplate> {

    //REVIEW: perhaps separate search criteria vs default specification,
    //        eg Range(0...5) allowed, but create with 4

    /**
     * create activity instances with given default duration
     *
     * @param duration IN STORED the duration of the activity created by
     *        this template. not null
     * @return the same builder object updated with new criteria
     */
    public @NotNull Builder duration( @NotNull Duration duration ) {
      this.duration = duration;
      return getThis();
    }
    @Nullable Duration duration; //only null until set!

    /**
     * {@inheritDoc}
     */
    public @NotNull Builder getThis() {
      return this;
    }

    protected ActivityCreationTemplate fill(ActivityCreationTemplate template){
      template.startRange = startsIn;
      template.endRange  = endsIn;
      template.startOrEndRange = startsOrEndsIn;
      template.nameRE = ( nameMatches != null )
              ? java.util.regex.Pattern.compile(nameMatches) : null;

      template.type = type;
      if(duration != null) {
        template.durationRange = new Range<Duration>(duration, duration);
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
   * Builder for creating disjunction of activity creation templates
   */
  public static class OrBuilder extends AbstractBuilder<ActivityCreationTemplate.OrBuilder, ActivityCreationTemplate> {

    /**
     * {@inheritDoc}
     */
    public @NotNull
    ActivityCreationTemplate.OrBuilder getThis() {
      return this;
    }

    @Override
    public ActivityCreationTemplateDisjunction build() {
      ActivityCreationTemplateDisjunction dis = new ActivityCreationTemplateDisjunction(exprs);
      return dis;
    }

    protected boolean orBuilder = false;

    List<ActivityCreationTemplate> exprs = new ArrayList<ActivityCreationTemplate>();

    public ActivityCreationTemplate.OrBuilder or(ActivityCreationTemplate expr) {
      exprs.add(expr);
      return getThis();
    }
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
  public @NotNull ActivityInstance createActivity( String name ) {
    final var act = new ActivityInstance( name, type );
    //REVIEW: how to properly export any flexibility to instance?

    //STNcheck();

    if(startRange != null && durationRange!= null){
      act.setStartTime(startRange.getMinimum());
      act.setDuration(durationRange.getMinimum());
    } else if(endRange != null && durationRange != null){
      act.setStartTime(endRange.getMinimum().minus(durationRange.getMinimum()));
    } else if(startRange!= null && endRange!= null){
      act.setStartTime(startRange.getMinimum());
      act.setDuration(endRange.getMinimum().minus(startRange.getMinimum()));
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
   * Experimental feature that checks consistency of temporal constraints with a STN network
   * This becomes useful and easier to read when many types of constraints can be posted for activities
   * TODO: Make this global, rename, clean
   */
  private boolean STNcheck(){

    var startHorizon = TimeWindows.startHorizon.toSeconds();


    //need to do something to convert to int...

    TNGraph<STNEdge> graph = new TNGraph<STNEdge>(STNEdgeInt.class);
    STN stn = new STN(graph);

    var ts = new LabeledNode("TS");
    var s = new LabeledNode("S");
    var e = new LabeledNode("E");



    if(startRange != null){
      var edge1 = new STNEdgeInt();
      edge1.setValue((int) (startRange.getMinimum().toSeconds()-startHorizon));
      var edge2 = new STNEdgeInt();
      edge2.setValue((int) (startRange.getMaximum().toSeconds()-startHorizon));
      graph.addEdge(edge1, ts, s);
      graph.addEdge(edge2, s, ts);
    }
    if(endRange != null){
      var edge1 = new STNEdgeInt();
      edge1.setValue((int) (endRange.getMinimum().toSeconds() - startHorizon));
      var edge2 = new STNEdgeInt();
      edge2.setValue((int) (endRange.getMaximum().toSeconds() - startHorizon));
      graph.addEdge(edge1, ts, e);
      graph.addEdge(edge2, e, ts);
    }
    if(durationRange!= null) {
      var edge1 = new STNEdgeInt();
      edge1.setValue((int) (durationRange.getMinimum().toSeconds()- startHorizon));
      var edge2 = new STNEdgeInt();
      edge2.setValue((int) (durationRange.getMaximum().toSeconds()- startHorizon));
      graph.addEdge(edge1, s, e);
      graph.addEdge(edge2, e, s);
    }
    boolean ret = false;
    try {
      ret = stn.consistencyCheck().consistency;
    } catch (WellDefinitionException wellDefinitionException) {
      wellDefinitionException.printStackTrace();
    }
    stn.allPairsShortestPaths();

    graph.getEdges();
    System.out.println(graph.getEdges());
    return ret;

  }

}
