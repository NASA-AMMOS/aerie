package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.ResourceBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.conditional.ConditionalConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ConstraintTest{


    public Resource wheel1 = null;
    public Resource primaryBattery = null;


    @Before
    public void setup(){
        this.wheel1 = new ResourceBuilder()
                .withName("wheel1")
                .forSubsystem("GNC")
                .withUnits("degrees")
                .withMin(0)
                .withMax(359)
                .getResource();

        this.primaryBattery = new ResourceBuilder()
                .withName("primaryBattery")
                .forSubsystem("GNC")
                .withUnits("amp-hours")
                .withMin(0)
                .withMax(100)
                .getResource();
    }

    @After
    public void teardown(){
        this.wheel1 = null;
        this.primaryBattery = null;
    }

    @Test
    public void testSetResourceValue(){
        wheel1.setValue(0.0);
        assertEquals(wheel1.getCurrentValue(),0.0);
        wheel1.setValue(54.7);
        assertEquals(wheel1.getCurrentValue(),54.7);
    }

    @Test
    public void testCreateOneConstraintWithResource(){
        wheel1.setValue(0.0);
        Constraint leaf_one = new ConditionalConstraint("Leaf 1").withLeftLeaf(wheel1).withRightLeaf(10.0).withOperand("<");
        wheel1.setValue(6.0);
        assertEquals(true, leaf_one.getValue());
        wheel1.setValue(12.4);
        assertEquals(false, leaf_one.getValue());
    }

    @Test
    public void testCreateTwoConstraintsWithSameResource(){
        wheel1.setValue(0.0);
        Constraint leaf_one = new ConditionalConstraint("Leaf 1").withLeftLeaf(wheel1).withRightLeaf(10.0).withOperand("<");
        ConditionalConstraint leaf_two = new ConditionalConstraint("Leaf 2").withLeftLeaf(wheel1).withRightLeaf(18.0).withOperand(">").build();
        assertEquals(false, leaf_two.getValue());
        assertEquals(true, (Boolean) leaf_two.getValue() || (Boolean) leaf_one.getValue());
    }

    @Test
    public void testCreateDepthOneConstraintTree(){
        wheel1.setValue(33.3);
        Constraint leaf_one = new ConditionalConstraint("Leaf 1").withLeftLeaf(wheel1).withRightLeaf(10.0).withOperand("<");
        ConditionalConstraint leaf_two = new ConditionalConstraint("Leaf 2").withLeftLeaf(wheel1).withRightLeaf(18.0).withOperand(">");
        ConditionalConstraint parent_of_one_two = new ConditionalConstraint("Parent (1,2)").withLeftLeaf((ConditionalConstraint) leaf_one).withRightLeaf(leaf_two).withOperand("||");
        assertEquals(true, parent_of_one_two.getValue());
    }

    @Test
    public void testCreateDepthTwoConstraintTree() {
        wheel1.setValue(33.3);
        primaryBattery.setValue(12.6);
        Constraint leaf_one = new ConditionalConstraint("Leaf 1").withLeftLeaf(wheel1).withRightLeaf(10.0).withOperand("<");
        ConditionalConstraint leaf_two = new ConditionalConstraint("Leaf 2").withLeftLeaf(wheel1).withRightLeaf(18.0).withOperand(">");
        ConditionalConstraint parent_of_one_two = new ConditionalConstraint("Parent (1,2)").withLeftLeaf((ConditionalConstraint) leaf_one).withRightLeaf(leaf_two).withOperand("||");
        ConditionalConstraint leaf_three = new ConditionalConstraint("Leaf 3").withLeftLeaf(primaryBattery).withRightLeaf(50.0).withOperand(">");
        ConditionalConstraint root = new ConditionalConstraint("Root").withLeftLeaf(parent_of_one_two).withRightLeaf(leaf_three).withOperand("&&");
        assertEquals(false, root.getValue());
        primaryBattery.setValue(122.3);
        assertEquals(true, root.getValue());
        wheel1.setValue(16.9);
        assertEquals(false, parent_of_one_two.getValue());
        assertEquals(false, root.getValue());
    }


    public class MockActivityTypeClass extends ActivityType{

        public boolean propertyChanged = false;
        public double value = 0;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue().getClass().toString().equals("class java.lang.Double")){
                value = (Double) evt.getNewValue();
                propertyChanged = true;
            }

            else if (evt.getNewValue().getClass().toString().equals("class java.lang.Boolean")){
                propertyChanged = true;
            }
        }
    }


    @Test
    public void testActivityListensToResourceChanges(){

        wheel1.setValue(123.2);
        MockActivityTypeClass actOne = new MockActivityTypeClass();
        Constraint leaf_one = new ConditionalConstraint("Act Constraint").withLeftLeaf(wheel1).withRightLeaf(10.0).withOperand("<").build();
        wheel1.addChangeListener(actOne);
        wheel1.setValue(200.0);
        assertEquals(actOne.propertyChanged, true);

    }


    @Test
    public void testActivityListensToActivityBooleanAndDoubleChanges(){

        MockActivityTypeClass actOne = new MockActivityTypeClass();
        MockActivityTypeClass actTwo = new MockActivityTypeClass();

        //actOne is now added to the set of listeners that actTwo has
        //this means that actOne will be notified everytime the signal changes in actTwo
        actTwo.addChangeListener(actOne);
        actTwo.setSignal(true);
        assertEquals(actOne.propertyChanged, true);

        assert(actOne.value == 0.0);
        actTwo.setValue(23.2);
        assert(actOne.value == 23.2);
    }

}