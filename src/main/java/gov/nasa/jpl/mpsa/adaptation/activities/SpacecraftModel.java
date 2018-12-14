package gov.nasa.jpl.mpsa.adaptation.activities;

import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;

public class SpacecraftModel {

    static ResourcesContainer myResources = ResourcesContainer.getInstance();

    public static void main(String args[]){

        // Create an instance of my battery
        BatteryResource battery = new BatteryResource();

        // now, I should be able to see it in the list of resources for the representation of the spacecraft
        System.out.println("Battery: " + myResources.getResourceByName("primaryBattery"));

    }

}
