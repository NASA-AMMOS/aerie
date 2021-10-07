package gov.nasa.jpl.aerie.scheduler;

import org.junit.Test;

public class TestLocalState {


  public Effector<Double> createDepletableEffector(double value, State<Double> state, ActivityType type) {
    Effector<Double> effector = new Effector<Double>(state, type) {
      @Override
      public Double getEffectAtTime(Time t) {
        return (state.getValueAt(t) + value);
      }
    };
    return effector;
  }


  public Effector<Double> createConsumableEffector(double value, State<Double> state, ActivityType type) {
    Effector<Double> effector = new Effector<Double>(state, type) {
      @Override
      public Double getEffectAtTime(Time t) {
        if (t.compareTo(Time.ofZero()) > 0) {
          return state.getValueAt(t.minus(Duration.ofSeconds(1))) + value;
        } else {
          return 0.;
        }
      }
    };
    return effector;
  }


  @Test
  public void test1() {

    ActivityType activityType = new ActivityType("acttype1");


    ActivityType resourceCapacityActivity = new ActivityType("acttype2");

    State<Double> locallyManagedState = new State<Double>(new Time(0), new Time(20)) {
      @Override
      public Double plus(Double value1, Double value2) {
        return value1 + value2;
      }

      @Override
      public Double minus(Double value1, Double value2) {
        return value1 - value2;
      }

      @Override
      public Double zero() {
        return 0.;
      }
    };
    locallyManagedState.setValue(0., new Range<Time>(new Time(0), new Time(20)));

    Effector<Double> effectorResource = createDepletableEffector(15, locallyManagedState, resourceCapacityActivity);
    ActivityInstance actInstR = new ActivityInstance(
        "actCapacity",
        resourceCapacityActivity,
        new Time(0),
        new Duration(20));

    locallyManagedState.addEffector(effectorResource);
    locallyManagedState.addActivityInstance(actInstR);

    //locallyManagedState.setValue(12.0, new Range<Time>(new Time(5), new Time(7)));
    //locallyManagedState.setValue(6.0, new Range<Time>(new Time(4), new Time(8)));
    System.out.println(locallyManagedState.toString());

    //assert(locallyManagedState.getValueAt(new Time(6)) == 6.0);

    Effector<Double> effector = new Effector<Double>(locallyManagedState, activityType) {
      @Override
      public Double getEffectAtTime(Time t) {
        return state.getValueAt(t) - 2.0;
      }
    };


    locallyManagedState.addEffector(effector);

    ActivityInstance actInst = new ActivityInstance("act1", activityType, new Time(3), new Duration(2));

    locallyManagedState.addActivityInstance(actInst);

    System.out.println(locallyManagedState.toString());

    ActivityInstance actInst2 = new ActivityInstance("act2", activityType, new Time(1), new Duration(5));

    locallyManagedState.addActivityInstance(actInst2);

    System.out.println(locallyManagedState.toString());


  }

  @Test
  public void testRangeOrder() {
    assert (new Range<Time>(new Time(1), new Time(5)).compareTo(new Range<Time>(new Time(5), new Time(7))) == -1);
    assert (new Range<Time>(new Time(1), new Time(5)).compareTo(new Range<Time>(new Time(1), new Time(5))) == 0);
    assert (new Range<Time>(new Time(5), new Time(7)).compareTo(new Range<Time>(new Time(1), new Time(5))) == 1);

  }

  @Test
  public void testEffector() {

  }

}
