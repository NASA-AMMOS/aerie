package gov.nasa.jpl.mpsa.examples.adaptationExample.activities;

import gov.nasa.jpl.mpsa.constraints.conditional.ConditionalConstraint;
import gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models.ExampleModel;
import gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models.WheelModel;
import gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models.WheelModelX;
import gov.nasa.jpl.mpsa.resources.ArrayedResource;
import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;

import java.util.Scanner;

public class SpacecraftModel {

    static ResourcesContainer myResources = ResourcesContainer.getInstance();

    public static void main(String args[]){


        // Create an instance of my battery
        Resource battery = new Resource.Builder("primaryBattery")
                .forSubsystem("mySubsystem")
                .withUnits("Ahr")
                .withMin(0)
                .withMax(100)
                .build();

        Resource wheel1 = new Resource.Builder("wheel1")
                .forSubsystem("GNC")
                .withUnits("degrees")
                .withMin(0)
                .withMax(359)
                .build();

        Resource primaryBattery = new Resource.Builder("primaryBattery")
                .forSubsystem("GNC")
                .withUnits("degrees")
                .withMin(0)
                .withMax(359)
                .build();

        wheel1.setValue(0.0);
        primaryBattery.setValue(0.0);

        ConditionalConstraint leaf_one = new ConditionalConstraint("Leaf 1").logicXpr(wheel1, 10.0, "<");
      //  ConditionalConstraint leaf_two = new ConditionalConstraint("Leaf 2").logicXpr(wheel1, 18.0, ">");




        //ConditionalConstraint parent_of_one_two = new ConditionalConstraint("Parent (1,2)").logicXpr(leaf_one, leaf_two, "||");
        ConditionalConstraint leaf_three = new ConditionalConstraint("Leaf 3").logicXpr(primaryBattery, 50.0, ">");
        //ConditionalConstraint root = new ConditionalConstraint("Root").logicXpr(parent_of_one_two, leaf_three, "&&");

        leaf_one.addTreeNodeChangeListener(leaf_three);

        System.out.println("Enter a value, statement 1");
        Scanner in = new Scanner(System.in);
        in.hasNext();


        System.out.println("\n\nSet wheel 1 to 30, statement 2");
        wheel1.setValue(30.0);

        Scanner in = new Scanner(System.in);
        in.hasNext();

        System.out.println("\n\nSet wheel 1 to 30, statement 2");
        wheel1.setValue(30.0);

       // System.out.println("Leaf one leftnode val is " + leaf_one.leftLeaf.getCurrentValue());
       // System.out.println("Wheel 1 current value is " + wheel1.getCurrentValue());


        System.out.println("\n\nEnter a value, statement 3");
        in = new Scanner(System.in);
        in.hasNext();

        System.out.println("\n\nSet wheel 1 to 0 and battery to 100");
        wheel1.setValue(0);
        primaryBattery.setValue(100);

/*

        ArrayedResource wheel_velocity = new ArrayedResource.Builder("RWA_angular_momentum")
                .forSubsystem("GNC")
                .withUnits("degrees/second")
                .withEntries(new String[]{"x", "y", "z"})
                .build();

        wheel_velocity.get("x").setValue(2.0);
        wheel_velocity.get("y").setValue(10.0);
        wheel_velocity.get("z").setValue(-15.0);

        myResources.addResource(battery);
        myResources.addResource(wheel1);
        wheel_velocity.registerArrayedResource(myResources);

        // now, I should be able to see it in the list of resources for the representation of the spacecraft
        System.out.println("Battery: " + myResources.getResourceByName("primaryBattery"));
        System.out.println("Wheel: " + myResources.getResourceByName("wheel1"));
        System.out.println("Wheel Angular Velocity x: " + myResources.getResourceByName("RWA_angular_momentum_x"));

        // This is mocking the invocation from the simulation service
        // Now run the simulation of the ExampleActivity
        ExampleActivity exampleActivity = new ExampleActivity();
        exampleActivity.setParameters();
        exampleActivity.setModel(new ExampleModel());

        MoveWheel1Activity move90Deg = new MoveWheel1Activity();
        move90Deg.setModel(new WheelModel());

        MoveWheel1Activity move90Seconds = new MoveWheel1Activity();
        move90Seconds.setModel(new WheelModelX());

        */

    //    exampleActivity.executeModel();
//        move90Deg.executeModel();
   //     move90Seconds.executeModel();
    }

}