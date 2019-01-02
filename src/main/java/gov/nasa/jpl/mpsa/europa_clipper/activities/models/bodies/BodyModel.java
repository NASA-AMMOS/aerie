package gov.nasa.jpl.mpsa.europa_clipper.activities.models.bodies;

import gov.nasa.jpl.mpsa.activities.operations.AdaptationModel;
import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;
import gov.nasa.jpl.mpsa.time.Time;
import spice.basic.*;

public class BodyModel implements AdaptationModel {

    private static String ABCORR = "LT+S";
    private double[] initial_state;
    private double[] final_state;
    private int sc_ID = -159;
    public int body_NAIF_ID;
    public Time initial_time;
    public Time final_time;
    public String body;

    @Override
    public void setup() {
        // Here, we can calculate the geometry information for a given body
        // Pass: body NAIF ID, time as et, J2000, ABCORR, spacecraft NAIF ID,
        //       state (empty 6 element array to return to), lt (empty one element to return to)
        double[] initial_state = new double[6];
        double[] initial_lt = new double[1];
        try {
            CSPICE.spkezr(String.valueOf(body_NAIF_ID), initial_time.toET(), "J2000",
                    ABCORR, String.valueOf(sc_ID), initial_state, initial_lt);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
    }

    @Override
    /** Trying to make this generic for multiple bodies such that states can be accessed through spice
     * for multiple times
     */
    public void execute() {

        ResourcesContainer myResources = ResourcesContainer.getInstance();

        // GET THE RESOURCE TO MODIFY
        Resource w1 = myResources.getResourceByName(body);

        // SET A NEW VALUE FOR THE RESOURCE
//        double[] initial_state = new double[6];
//        double[] initial_lt = new double[1];
//        try {
//            CSPICE.spkezr(String.valueOf(body_NAIF_ID), initial_time.toET(), "J2000",
//                    ABCORR, String.valueOf(sc_ID), initial_state, initial_lt);
//        } catch (SpiceErrorException e) {
//            e.printStackTrace();
//        }
//                w1.setValue(x + i);

    }
}
