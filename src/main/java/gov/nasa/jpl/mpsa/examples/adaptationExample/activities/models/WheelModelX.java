package gov.nasa.jpl.mpsa.examples.adaptationExample.activities.models;

import gov.nasa.jpl.mpsa.activities.Parameter;
import gov.nasa.jpl.mpsa.activities.operations.AdaptationModel;
import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;

import java.util.List;

// A different wheel1 model which suggests that
// wheel1 is an x-fixed reaction wheel. Because it is calculated ev
public class WheelModelX implements AdaptationModel {

    private double step = 2.0;

    @Override
    public void setup(List<Parameter> parameters) {
        // Check the x-direction angular velocity of the reaction wheel
        ResourcesContainer myResources = ResourcesContainer.getInstance();
        Resource w1x_velocity = myResources.getResourceByName("RWA_angular_momentum_x");
        // cant do this yet, logic is not there
//        this.step = (double)w1x_velocity.getCurrentValue();
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
                System.out.println("moved by " + step + " deg to " + i*step);
                // cant do this yet, logic is not there
//                w1.setValue(i*step);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}



