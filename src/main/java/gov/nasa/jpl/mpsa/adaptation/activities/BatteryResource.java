package gov.nasa.jpl.mpsa.adaptation.activities;

import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;

public class BatteryResource {

    ResourcesContainer myResources = ResourcesContainer.getInstance();

    public BatteryResource() {
        Resource battery = new Resource.Builder("primaryBattery")
                .forSubsystem("mySubsystem")
                .withUnits("Ahr")
                .withMin(0)
                .withMax(100)
                .build();

        myResources.addResource(battery);
    }

    public BatteryResource(String name) {
        Resource battery = new Resource.Builder(name)
                .forSubsystem("mySubsystem")
                .withUnits("Ahr")
                .withMin(0)
                .withMax(100)
                .build();

        myResources.addResource(battery);
    }


}
