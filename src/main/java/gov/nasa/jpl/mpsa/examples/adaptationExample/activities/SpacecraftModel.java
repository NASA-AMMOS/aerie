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

        ConditionalConstraint leaf_one = new ConditionalConstraint("Leaf 1").withLeftLeaf(wheel1).withRightLeaf(10.0).withOperand("<");

        Scanner scanner = new Scanner(System.in);
        //We expect to see an updated evaluation
        System.out.println("This is what happens if we change a resource that is being listened to by a disconnected leaf");
        String readString = scanner.nextLine();
        wheel1.setValue(6.0);


        //This is what happens when we do change the value
        System.out.println("\n\nThis is what happens if we change a resource that is being listened to by a disconnected leaf");
        readString = scanner.nextLine();
        wheel1.setValue(12.0);
        readString = scanner.nextLine();


        ConditionalConstraint leaf_two = new ConditionalConstraint("Leaf 2").withLeftLeaf(wheel1).withRightLeaf(18.0).withOperand(">");
        ConditionalConstraint parent_of_one_two = new ConditionalConstraint("Parent (1,2)").withLeftLeaf(leaf_one).withRightLeaf(leaf_two).withOperand("||");

        //This is what happens when create a tree structure
        //Currently, Parent(1,2) is false
        //this is what happens if we change a resource value to make both conditions true
        System.out.println("\n\nWe've created a parent of two leaf nodes whose value is currently false, let's change it to true");
        readString = scanner.nextLine();
        wheel1.setValue(33.3);

        readString = scanner.nextLine();
        System.out.println("\n\nNow let's change a resource value that does not result in a change in the parent");
        readString = scanner.nextLine();
        wheel1.setValue(50.5);

        ConditionalConstraint leaf_three = new ConditionalConstraint("Leaf 3").withLeftLeaf(primaryBattery).withRightLeaf(50.0).withOperand(">");
        ConditionalConstraint root = new ConditionalConstraint("Root").withLeftLeaf(parent_of_one_two).withRightLeaf(leaf_three).withOperand("&&");

        //Create another node and parent
        //This is currently false
        //Let's see what happens when we set primary battery to 12.6 (should still be false)
        readString = scanner.nextLine();
        System.out.println("\n\nNow we've added two more nodes, a root and a battery node.  Currently root is false, let's update resource to keep it false");
        readString = scanner.nextLine();
        primaryBattery.setValue(12.6);

        //Let's check for race conditions
        readString = scanner.nextLine();
        System.out.println("\n\nHow does change propagation look with multiple changes in a short time span?");
        wheel1.setValue(12.3);
        primaryBattery.setValue(100.0);

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