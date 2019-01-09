package gov.nasa.jpl.mpsa.examples.adaptationExample.activities;

import gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models.ExampleModel;
import gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models.WheelModel;
import gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models.WheelModelX;
import gov.nasa.jpl.mpsa.resources.ArrayedResource;
import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;

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

        wheel1.setValue(0);

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

        exampleActivity.executeModel();
//        move90Deg.executeModel();
        move90Seconds.executeModel();
    }

}
