package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MerlInsightRules extends Problem {

  static final PlanningHorizon DEFAULT_PLANNING_HORIZON = new PlanningHorizon(new Time(0), new Time(48 * 3600));

  public MerlInsightRules(MissionModel<?> missionModel) {
    super(missionModel, DEFAULT_PLANNING_HORIZON);
  }
  public MerlInsightRules(MissionModel<?> missionModel, PlanningHorizon planningHorizon) {
    super(missionModel, planningHorizon);
  }

  @Override
  public java.util.Collection<Goal> getGoals() {
    Collection<Goal> goals = new ArrayList<>();
    goals.add(generateDSNVisibilityAllocationGoal());
    goals.addAll(getFirstRuleGoals());
    goals.addAll(getSecondRuleGoals());
    goals.addAll(getThirdRuleGoals());
    return goals;
  }

  public Goal generateDSNVisibilityAllocationGoal(){
    var actType1 = getActivityType("AllocateDSNStation");
    var actType2 = getActivityType("SetDSNStationVisibility");
    final var period = Duration.of(8, Duration.HOURS);
    final var ratioAlloc = 5;
    final var actList = new ArrayList<ActivityInstance>();

    final var values =  new String[]{"Canberra","Madrid","Goldstone"};
    int index = 0;
    int actIndex = 0;
    int curAlloc = 1;
    var time = DEFAULT_PLANNING_HORIZON.getHor().start;
    while(time.shorterThan(DEFAULT_PLANNING_HORIZON.getHor().end)){
      var curStation = values[index];

      var actInstance = new ActivityInstance(actType2, time, Duration.min(DEFAULT_PLANNING_HORIZON.getHor().end.minus(time), period));
      actInstance.addArgument("dsnStation", SerializedValue.of(curStation));
      actList.add(actInstance);
      index +=1;

      if(index == values.length-1){
        index = 0;
      }
      curAlloc +=1;
      if(curAlloc == ratioAlloc){
        //allocate this to insight
        var actInstanceAlloc = new ActivityInstance(actType1, time, Duration.min(DEFAULT_PLANNING_HORIZON.getHor().end.minus(time), period));
        actInstanceAlloc.addArgument("dsnStation", SerializedValue.of(curStation));
        actList.add(actInstanceAlloc);
        curAlloc = 1;
      }


      time = time.plus(period);
    }

    ProceduralCreationGoal dsnGoal = new ProceduralCreationGoal.Builder()
        .named("Schedule DSN contacts for initial setup")
        .withPriority(50)
        .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
        .generateWith((plan) -> actList)
        .build();

    return dsnGoal;
  }


  public Collection<Goal> getFirstRuleGoals(){

    List<Goal> goals = new ArrayList<>();

  /**
   *
   * Rule 1a: periodic heat probe temperature sampling
   (show off periodic scheduling, simple resource condition, and parameter filling from res)
   - schedule an HP3_TEMP activity
   - with parameters
   - duration = current value of resource HP3_Parameters_Current["PARAM_HP3_MON_TEMP_DURATION"] (typically 5min)
   - setNewSSATime = true
   - once every 60 minutes
   - while the HP3_SSA_State is "Monitoring"
   (may need acts in initial plan to turn on/off Monitoring resource)
   */

    var HP3actType = getActivityType("HP3TemP");
    var mutex = GlobalConstraints.atMostOneOf(List.of(ActivityExpression.ofType(HP3actType)));
    add(mutex);

    var actT1 = getActivityType("SSAMonitoring");

    List<ActivityInstance> turnONFFMonitoring = List.of(
        new ActivityInstance(actT1, DEFAULT_PLANNING_HORIZON.getStartAerie()
                                                            .plus(Duration.of(1,Duration.MINUTE)),
                             Duration.of(1,Duration.MINUTE)));
    ProceduralCreationGoal pro = new ProceduralCreationGoal.Builder()
        .named("TurnOnAndOFFMonitoring")
        .withPriority(10)
        .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
        .generateWith((plan) -> turnONFFMonitoring)
        .build();
    goals.add(pro);

    var sce = new StateConstraintExpression.Builder()
        .equal(getResource("/hp3/ssaState"), SerializedValue.of("Monitoring"))
        .build();

    var HP3Acts = new ActivityCreationTemplate.Builder()
        .ofType(HP3actType)
        .duration(
            getResource("/hp3/currentParams/PARAM_HP3_MON_TEMP_DURATION"),
            TimeExpression.atStart())
        .withArgument("setNewSSATime", SerializedValue.of(true))
        .build();

    RecurrenceGoal goal1a = new RecurrenceGoal.Builder()
        .named("1a")
        .withPriority(9)
        .repeatingEvery(Duration.of(60, Duration.MINUTE))
        .attachStateConstraint(sce)
        .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
        .thereExistsOne(HP3Acts)
        .build();
    goals.add(goal1a);


    /*
    * Rule 1c: total heat probe temperature data duration
      (show off joint custody with rule #1, show off total duration scheduling)
    - schedule HP3_TEMP activities
    - so that total duration >60min
      (or whatever duration results in at least a couple additional activities of its own)
    ref: not in apgen code, just a made up rule to show joint custody
     */

    CardinalityGoal goal1c = new CardinalityGoal.Builder()
        .named("1c")
        .inPeriod(new TimeRangeExpression.Builder().from(new Windows(DEFAULT_PLANNING_HORIZON.getHor())).build())
        .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
        .withPriority(8)
        .thereExistsOne(HP3Acts)
        .attachStateConstraint(sce)
        .duration(Window.between(Duration.of(3, Duration.HOUR), Duration.MAX_VALUE))
        .build();

    goals.add(goal1c);
    return goals;
  }


  public Collection<Goal> getSecondRuleGoals(){
    var goals = new ArrayList<Goal>();

  /**
   *
   === Rule 2: device placement with arm and contextual imaging, with necessary heating ===
   basically: move arm to offboard device, grapple the device, move arm to target location, release grapple
   with images: ICC image before and after unstowing device, IDC before and after each grapple operation
   with heating: each zone heated only when needed
   should all be one large composite rule!

   Rule 2a: move the arm to the stowed device
   - schedule IDAMoveArm
   - once
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/IDAMoveArm.java
   ref: apgen activities
   https://github.jpl.nasa.gov/insight-mst/apgen_surface/blob/b909cf5eb570ab5d4020795f181cf81190d3bfb4/model/IDS_tactical_activities.aaf
*/

  var actTypeIDAMoveArm = getActivityType("IDAMoveArm");
  //starts at middle of horizon
  var stMoveArm = DEFAULT_PLANNING_HORIZON.getAerieHorizonDuration().dividedBy(2);
  var duroveArm = Duration.of(20,Duration.MINUTE);

  ProceduralCreationGoal goal2a = new ProceduralCreationGoal.Builder()
      .named("SchedIDAMoveArm")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .withPriority(10)
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .generateWith((plan) -> List.of(new ActivityInstance(actTypeIDAMoveArm,stMoveArm, duroveArm)))
      .build();

  goals.add(goal2a);

  /*
   * Rule 2b: pick up the device from its stowed location
   - schedule IDAGrapple
   - starts at end of first IDSMoveArm
   ref: merlin act model https://github.jpl.nasa.gov/Aerie/aerie/blob/7598e014788595bf323e93185ffbd1dfa12fce68/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/IDAGrapple.java
*/
  var atGrapple = getActivityType("IDAGrapple");

  CoexistenceGoal goal2b= new CoexistenceGoal.Builder()
      .named("Grapple IDA")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(atGrapple)
                          .duration(Duration.of(20, Duration.MINUTE))
                          .build())
      .forEach(ActivityExpression.ofType(actTypeIDAMoveArm))
      .withPriority(9)
      .startsAt(TimeAnchor.END)
      .build();
    goals.add(goal2b);

  /*
   * Rule 2c: relocate the device from its stowed location to the target location
   - schedule IDAMoveArm
   - starts at end of prev IDAGrapple
  */
  CoexistenceGoal goal2c= new CoexistenceGoal.Builder()
      .named("SchedIDAMoveArm Back")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIDAMoveArm)
                          .duration(Duration.of(1, Duration.HOUR))
                          .build())
      .forEach(ActivityExpression.ofType(atGrapple))
      .withPriority(8)
      .startsAt(TimeAnchor.END)
      .build();
  goals.add(goal2c);

  /*
   * Rule 2d: release the device at its target location
   - schedule IDAGrapple
   - starts at end of second IDSMoveArm
*/

    var secondIdaMoveArm = new TimeRangeExpression.Builder()
        .from(ActivityExpression.ofType(actTypeIDAMoveArm))
        .thenFilter(Filters.numbered(1))
        .build();

    CoexistenceGoal goal2d= new CoexistenceGoal.Builder()
        .named("Grapple IDA second")
        .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(atGrapple)
                            .duration(Duration.of(20, Duration.MINUTE))
                            .build())
        .forEach(secondIdaMoveArm)
        .withPriority(7)
        .startsAt(TimeAnchor.END)
        .build();

    goals.add(goal2d);


  /*
   * Rule 2e: preheat the arm
   - schedule IDAHeatersOn
   - ends before start of earliest IDAGrapple or IDAMoveArm
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/7598e014788595bf323e93185ffbd1dfa12fce68/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/IDAHeatersOn.java
*/
  var enveloppeAllGrappleMove = new TimeRangeExpression.Builder()
      .from(Windows.forever())
      .thenTransform(new Transformers.EnveloppeBuilder()
                         .withinEach(TimeRangeExpression.of(Windows.forever()))
                         .when(ActivityExpression.ofType(atGrapple))
                         .when(ActivityExpression.ofType(actTypeIDAMoveArm))
                         .build())
      .build();

  var actTypeIDAHeatersOn = getActivityType("IDAHeatersOn");

  CoexistenceGoal goal2e= new CoexistenceGoal.Builder()
      .named("Heaters ON")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIDAHeatersOn)
                          .duration(Duration.of(3, Duration.MINUTE))
                          .build())
      .forEach(enveloppeAllGrappleMove)
      .withPriority(6)
      .endsBefore(TimeExpression.offsetByBeforeStart(Duration.of(30, Duration.MINUTE)))
      .build();

  goals.add(goal2e);


  /*
   * Rule 2f: arm heaters off when done
   - schedule IDAHeatersOff
   - starts after end of latest IDAGrapple or IDAMoveArm
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/7598e014788595bf323e93185ffbd1dfa12fce68/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/IDAHeatersOff.java
*/

  var actTypeIDAHeatersOff = getActivityType("IDAHeatersOff");

  CoexistenceGoal goal2f= new CoexistenceGoal.Builder()
      .named("Heaters Off")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIDAHeatersOff)
                          .duration(Duration.of(3, Duration.MINUTE))
                          .build())
      .forEach(enveloppeAllGrappleMove)
      .withPriority(6)
      .startsAfterEnd()
      .build();

  goals.add(goal2f);

  /*
   * Rule 2g: image from arm just before each grapple operation
   - schedule IDCImage
   - ends immediately before start each IDAGrapple (both pickup and dropoff)
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/IDCImages.java#L28
*/
  var actTypeIDCImage= getActivityType("IDCImages");

  CoexistenceGoal goal2g= new CoexistenceGoal.Builder()
      .named("Image before grapple")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIDCImage)
                          .duration(Duration.of(6, Duration.MINUTE))
                          .withArgument("nFrames", SerializedValue.of(1))
                          .withArgument("apid", SerializedValue.of("APID_IDC_6"))
                          .withArgument("compQuality", SerializedValue.of(97))
                          .build())
      .forEach(ActivityExpression.ofType(atGrapple))
      .withPriority(6)
      .endsAt(TimeAnchor.START)
      .build();

  goals.add(goal2g);


  /*
   * Rule 2h: image from arm just after each grapple operation
   - schedule IDCImage
   - ends immediately after end each IDAGrapple (both pickup and dropoff)
*/
  CoexistenceGoal goal2h= new CoexistenceGoal.Builder()
      .named("Image after grapple")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIDCImage)
                          .duration(Duration.of(6, Duration.MINUTE))
                          .withArgument("nFrames", SerializedValue.of(1))
                          .withArgument("apid", SerializedValue.of("APID_IDC_6"))
                          .withArgument("compQuality", SerializedValue.of(97))
                          .build())
      .forEach(ActivityExpression.ofType(atGrapple))
      .withPriority(6)
      .startsAt(TimeAnchor.END)
      .build();

  goals.add(goal2h);


  /*
   * Rule 2i: preheat before IDC image
   - schedule IDCHeatersOn
   - so that ends before earliest start of any IDCImage acts above
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/IDCHeatersOn.java
*/

  var enveloppeAllIDCImage = new TimeRangeExpression.Builder()
      .from(Windows.forever())
      .thenTransform(new Transformers.EnveloppeBuilder()
                         .withinEach(TimeRangeExpression.of(Windows.forever()))
                         .when(ActivityExpression.ofType(actTypeIDCImage))
                         .build())
      .build();

  var actTypeIDCHeatersOn= getActivityType("IDCHeatersOn");


  CoexistenceGoal goal2i= new CoexistenceGoal.Builder()
      .named("Heaters before earliest image")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIDCHeatersOn)
                          .duration(Duration.of(15, Duration.MINUTE))
                          .build())
      .forEach(enveloppeAllIDCImage)
      .withPriority(5)
      .endsBefore(TimeExpression.offsetByBeforeStart(Duration.of(30, Duration.MINUTE)))
      .build();

  goals.add(goal2i);


  /*
   * Rule 2j: heaters off after last IDC image
   - schedule IDCHeatersOff
   - so that ends after end of any IDCImage act above
   * CORRECTION ADRIEN : Starts after end of any IDC image
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/IDCHeatersOff.java
*/

  var actTypeIDCHeatersOff= getActivityType("IDCHeatersOff");

  CoexistenceGoal goal2j= new CoexistenceGoal.Builder()
      .named("Heaters after latest image")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIDCHeatersOff)
                          .duration(Duration.of(15, Duration.MINUTE))
                          .build())
      .forEach(enveloppeAllIDCImage)
      .withPriority(5)
      .startsAfterEnd()
      .build();

  goals.add(goal2j);

  /*
   * Rule 2k: image stowed device in context before pickup
   - schedule ICCImages
   - ends immediately before start of first IDAMoveArm
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/IDCImages.java
*/
  var firstIdaMoveArm = new TimeRangeExpression.Builder()
      .from(ActivityExpression.ofType(actTypeIDAMoveArm))
      .thenFilter(Filters.first())
      .build();

  var actTypeICCImages =  getActivityType("ICCImages");

  CoexistenceGoal goal2k= new CoexistenceGoal.Builder()
      .named("image stowed device in context before pickup")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeICCImages)
                          .duration(Duration.of(6, Duration.MINUTE))
                          .withArgument("nFrames", SerializedValue.of(1))
                          .withArgument("apid", SerializedValue.of("APID_ICC_6"))
                          .withArgument("compQuality", SerializedValue.of(95))
                          .build())
      .forEach(firstIdaMoveArm)
      .withPriority(4)
      .endsAt(TimeAnchor.START)
      .build();

  goals.add(goal2k);


  /*
   * Rule 2l: image stowage area after relocating device
   - schedule ICCImages
   - starts at +2 min from start of the second (placement) IDAMoveArm
*/

  CoexistenceGoal goal2l= new CoexistenceGoal.Builder()
      .named("image stowage area after relocating device")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeICCImages)
                          .duration(Duration.of(6, Duration.MINUTE))
                          .withArgument("nFrames", SerializedValue.of(1))
                          .withArgument("apid", SerializedValue.of("APID_ICC_6"))
                          .withArgument("compQuality", SerializedValue.of(95))
                          .build())
      .forEach(secondIdaMoveArm)
      .withPriority(4)
      .startsAt(TimeExpression.offsetByAfterEnd(Duration.of(2, Duration.MINUTE)))
      .build();

  goals.add(goal2l);

  /*
   * Rule 2m: preheat for ICC image
   - schedule ICCHeatersOn
   - so that ends before earliest start of any ICCImage acts above
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/ICCHeatersOn.java
*/

  var firstICCImages = new TimeRangeExpression.Builder()
      .from(ActivityExpression.ofType(actTypeICCImages))
      .thenFilter(Filters.first())
      .build();
  var actTypeIccHeatersOn = getActivityType("ICCHeatersOn");

  CoexistenceGoal goal2m= new CoexistenceGoal.Builder()
      .named("preheat for ICC image")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIccHeatersOn)
                          .duration(Duration.of(15, Duration.MINUTE))
                          .build())
      .forEach(firstICCImages)
      .withPriority(4)
      .endsAt(TimeAnchor.START)
      .build();

  goals.add(goal2m);


  /*
   * Rule 2n: heaters off after last ICC image
   - schedule ICCHeatersOff
   - so that ends after end of any ICCImage act above
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/ids/ICCHeatersOff.java#L15

*/
  var lastICCImages = new TimeRangeExpression.Builder()
      .from(ActivityExpression.ofType(actTypeICCImages))
      .thenFilter(Filters.last())
      .build();

  var actTypeIccHeatersOff = getActivityType("ICCHeatersOff");

  CoexistenceGoal goal2n= new CoexistenceGoal.Builder()
      .named("turn off heaters for ICC image")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeIccHeatersOff)
                          .duration(Duration.of(10, Duration.SECONDS))
                          .build())
      .forEach(lastICCImages)
      .withPriority(4)
      .endsAfterEnd()
      .build();

  goals.add(goal2n);
  return goals;
}
  public Collection<Goal> getThirdRuleGoals(){

    var goals = new ArrayList<Goal>();

    //the dsn visibility activities from generateDSNVisibilityAllocationGoal are required for the following goals

    StateConstraintExpression sc1 = new StateConstraintExpression.Builder()
        .andBuilder()
        .name("CanberraSC")
        .equal(getResource("/dsn/visible/Canberra"), SerializedValue.of("InView"))
        .equal(getResource("/dsn/allocated/Canberra"), SerializedValue.of("Allocated"))
        .build();
    StateConstraintExpression sc2 = new StateConstraintExpression.Builder()
        .andBuilder()
        .name("MadridSC")
        .equal(getResource("/dsn/visible/Madrid"), SerializedValue.of("InView"))
        .equal(getResource("/dsn/allocated/Madrid"), SerializedValue.of("Allocated"))
        .build();
    StateConstraintExpression sc3 = new StateConstraintExpression.Builder()
        .andBuilder()
        .name("GoldstoneSC")
        .equal(getResource("/dsn/visible/Goldstone"), SerializedValue.of("InView"))
        .equal(getResource("/dsn/allocated/Goldstone"), SerializedValue.of("Allocated"))
        .build();

    var disj = new StateConstraintExpressionDisjunction(List.of(sc1,sc2,sc3), "disjunction");

    TimeRangeExpression expr = new TimeRangeExpression.Builder()
        .from(disj)
        .name("TRE")
        .thenFilter(Filters.minDuration(Duration.of(20, Duration.MINUTE)))
        .build();

    Duration prepDur = Duration.of(5, Duration.MINUTE).plus(Duration.of(23, Duration.SECONDS));
    Duration cleanupDur =Duration.of(19, Duration.SECONDS);


  /*

   === Rule 3: xband comm with prep/cleanup ===
   should all be one composite rule!

   * Rule 3a:
   - schedule XbandActive
   - with parameters
   - duration: max of 2hr or full dsn station visibility/allocation window
   - when a dsn station is both visible and allocated
   (will need initial plan to include changing visibility/allocation states)
   - and the contact window is at least 20min
   ref: merlin activity model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/comm/xband/XBandActive.java#L17
   ref: merlin comm model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/models/comm/CommModel.java#L12
   (does not have states for visibility, so will have to create them, per dsn complex)
   ref: merlin accumulated data model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/models/data/DataModel.java#L64
*/

  var actTypeXbandActive = getActivityType("XBandActive");

  CoexistenceGoal goal3a= new CoexistenceGoal.Builder()
      .named("xbandactivegoal")
      .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
      .thereExistsOne(new ActivityCreationTemplate.Builder()
                          .ofType(actTypeXbandActive)
                          .build())
      .forEach(expr)
      .withPriority(7)
      .startsAt(TimeExpression.offsetByAfterStart(prepDur))
      .durationIn(DurationExpressions.max(
          DurationExpressions.constant(Duration.of(2, Duration.HOUR)),
          DurationExpressions.windowDuration().minus(DurationExpressions.constant(cleanupDur)).minus(DurationExpressions.constant(prepDur))))
      .build();

    goals.add(goal3a);

  /*
   * Rule 3b: comm prep
   - schedule XBandPrep
   - with parameters
   - duration: not sure! see ref below
   - so that ends at beginning of XBandActive
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/comm/xband/XBandPregoals.java
   ref: duration calculation from old decompositional parent act
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/comm/xband/XBandComm.java#L39
*/

    var actTypeXbandPrep = getActivityType("XBandPrep");

    CoexistenceGoal goal3b= new CoexistenceGoal.Builder()
        .named("xbandprepgoal")
        .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actTypeXbandPrep)
                            .duration(prepDur)
                            .build())
        .forEach(ActivityExpression.ofType(actTypeXbandActive))
        .withPriority(6)
        .endsAt(TimeAnchor.START)
        .build();

    goals.add(goal3b);

  /*
   * Rule 3c: comm cleanup
   - schedule XBandCleanup for each XBandActive
   - with parameters
   - duration: not sure! see ref below
   - so that starts at end of XBandActive
   ref: merlin act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/comm/xband/XBandCleanugoals.java#L16
   ref: duration calculation from old decompositional parent act
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/comm/xband/XBandComm.java#L39
*/
    var actTypeXbandCleanup= getActivityType("XBandCleanup");


    CoexistenceGoal goal3c= new CoexistenceGoal.Builder()
        .named("xbandcleanupgoal")
        .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actTypeXbandCleanup)
                            .duration(cleanupDur)
                            .build())
        .forEach(ActivityExpression.ofType(actTypeXbandActive))
        .withPriority(6)
        .startsAt(TimeAnchor.END)
        .build();

    goals.add(goal3c);
  /*
   * Rule 3d: comm summary wrapper
   - schedule XBandCommND activity
   - with parameters
   - dsntrack matching dsn station chosen for XBandActive
   - but don't want all the inner timing params...
   - that tightly envelopes each entire prep-comm-cleanup triple
   ref: existing higher level merlin decompositional act model
   https://github.jpl.nasa.gov/Aerie/aerie/blob/develop/insight/src/main/java/gov/nasa/jpl/aerie/insight/activities/comm/xband/XBandComm.java#L48
   (will need a place to store the summary info like dsnTrack station and dlRate, as well as do the one setXBandAntenna() call)
   (can create a new non-decomposition mirror of this act, still with setXBandAnt and only params unrelated to decomp timing)
   ref: apgen decompositional parent
   https://github.jpl.nasa.gov/insight-mst/apgen_surface/blob/3f8c09da174315b57a28f08fa2a9df9688b6edc4/model/Master_activities.aaf#L1269

   */
    var actTypeXbandCommched= getActivityType("XBandCommSched");

    var enveloppe = new Transformers.EnveloppeBuilder()
        .withinEach(expr)
        .when(ActivityExpression.ofType(actTypeXbandActive))
        .when(ActivityExpression.ofType(actTypeXbandCleanup))
        .when(ActivityExpression.ofType(actTypeXbandPrep))
        .build();

    var tre2 = new TimeRangeExpression.Builder()
        .from(expr)
        .thenTransform(enveloppe)
        .build();

    CoexistenceGoal goal3d= new CoexistenceGoal.Builder()
        .named("xbancommgoal")
        .forAllTimeIn(DEFAULT_PLANNING_HORIZON.getHor())
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actTypeXbandCommched)
                            .withArgument("DSNTrack", getResource("/dsn/allocstation"))
                            .withArgument("xbandAntSel", SerializedValue.of("EAST_MGA"))
                            .build())
        .forEach(tre2)
        .withPriority(3)
        .startsAt(TimeAnchor.START)
        .endsAt(TimeAnchor.END)
        .build();

    goals.add(goal3d);
    return(goals);
  }


}
