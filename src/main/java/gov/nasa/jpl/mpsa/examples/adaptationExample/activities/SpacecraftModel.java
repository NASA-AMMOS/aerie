package gov.nasa.jpl.mpsa.examples.adaptationExample.activities;

import gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models.ExampleModel;
import gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models.WheelModel;
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
                .forSubsystem("mobility")
                .withUnits("degrees")
                .withMin(0)
                .withMax(359)
                .build();

        wheel1.setValue(0);

        myResources.addResource(battery);
        myResources.addResource(wheel1);

        // now, I should be able to see it in the list of resources for the representation of the spacecraft
        System.out.println("Battery: " + myResources.getResourceByName("primaryBattery"));

        // This is mocking the invokation from the simulation service
        // Now run the simulation of the ExampleActivity
        ExampleActivity exampleActivity = new ExampleActivity();
        exampleActivity.setParameters();
        exampleActivity.setModel(new ExampleModel());

        MoveWheel1Activity move90Deg = new MoveWheel1Activity();
        move90Deg.setModel(new WheelModel());


        exampleActivity.executeModel();
        move90Deg.executeModel();
    }

}
