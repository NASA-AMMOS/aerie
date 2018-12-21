package gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models;

import gov.nasa.jpl.mpsa.activities.operations.AdaptationModel;
import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;

public class WheelModel implements AdaptationModel {

    private int x = 0;

    @Override
    public void setup() {
        // DO SOME PREPARATION
        System.out.println("calculates a formula needed for something");
        this.x = 10;
    }

    @Override
    public void execute() {

        ResourcesContainer myResources = ResourcesContainer.getInstance();

        // GET THE RESOURCE TO MODIFY
        Resource w1 = myResources.getResourceByName("wheel1");

        // SET A NEW VALUE FOR THE RESOURCE
        for(int i= 0; i<90; i++) {
            try {
                Thread.sleep(1000);
                w1.setValue(x + i);
                System.out.println("moved by 1 deg to " + i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}



