package gov.nasa.jpl.mpsa.europa_clipper.activities.models.bodies;

import gov.nasa.jpl.mpsa.activities.operations.AdaptationModel;
import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;
import gov.nasa.jpl.mpsa.time.Time;
import spice.basic.*;

public class BodyModel implements AdaptationModel {

    private static String ABCORR = "LT+S";
    private double[] initial_state;
    private double[] state;
    private double[] initial_lt;
    private double[] lt;
    private int sc_ID = -159;
    public int body_NAIF_ID;
    public Time initial_time;
    public String body;

    @Override
    public void setup() {
        // Here, we can calculate the geometry information for a given body
        // Pass: body NAIF ID, time as et, J2000, ABCORR, spacecraft NAIF ID,
        //       state (empty 6 element array to return to), lt (empty one element to return to)
        initial_state = new double[6];
        initial_lt = new double[1];
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
        Resource aBody = myResources.getResourceByName(body);

        // SET A NEW VALUE FOR THE RESOURCE
        state = new double[6];
        lt = new double[1];
        try {
            CSPICE.spkezr(String.valueOf(body_NAIF_ID), initial_time.toET(), "J2000",
                    ABCORR, String.valueOf(sc_ID), state, lt);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
//                w1.setValue(x + i);

    }

    public void calculate_time_steps(String bodyName, Double eps, Duration minStep, Duration maxStep, Time startTime) {

    }


    /**
     @brief Determine the next step size for a sampling routine given a desired error
     @param[in] t0 - time the third-to-last point was measured
     @param[in] x0 - value when third-to-last point was measured
     @param[in] t1 - time the second-to-last point was measured
     @param[in] x1 - value when second-to-last point was measured
     @param[in] t2 - time the last point was measured
     @param[in] x2 - last point was measured
     @param[in] eps - relative error desired
     @return next_step_size (s) - duration after which next value should be measured
     */
//    private Duration nextStepSize(Time t0, Double x0, Time t1, Double x1, Time t2, Double x2) {
        // to calculate the 'acceleration' of the data we need the last two data 'velocities'
//        Double v2 = (x2-x1)/((t2.subtract(t1)).totalSeconds());
//        Double v1 = (x1-x0)/((t1.subtract(t0)).totalSeconds());
//
//        if(v1 - v2 == 0){
//            return maxStep;
//        }
//        Duration next_step_size = new Duration((long) (1000*(eps*Math.abs(x2))/Math.abs(v2-v1)));
//
//        // we can't let the step size be so large we miss things, or so small it slows to a crawl
//        if(next_step_size.lessThan(minStep)){
//            return minStep;
//        }
//        else if(next_step_size.greaterThan(maxStep)){
//            return maxStep;
//        }
//        else{
//            return next_step_size;
//        }
//
//    }
}
