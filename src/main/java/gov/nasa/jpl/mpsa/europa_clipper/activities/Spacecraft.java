package gov.nasa.jpl.mpsa.europa_clipper.activities;

import gov.nasa.jpl.mpsa.resources.Resource;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;
import gov.nasa.jpl.mpsa.time.Time;

/** Starting point for a spacecraft adaptation. This is where the adapter could define the
 * logic of a sim, from the bodies in a mission to the ways the spacecraft slews or uses power.
 * This could turn out to be cumbersome due to the large amount of background logic necessary to
 * even start creating activities and such.
 */
public class Spacecraft {

    static ResourcesContainer myResources = ResourcesContainer.getInstance();

    public static int ClipperNaifID = -159;
    public static String ClipperFrame = "EUROPAM_SPACECRAFT";
    public static String ABCORR = "LT+S";

    /** Thinking that, for spice purposes, the main args that should be passed in are the start and
     * end times of the sim
     */
    public static void main(String args[]) {

        // BODIES //
        /** TODO: What if we made resources able to take any defined class as a value as well? Maybe this isn't necessary,
         * since we could also just add placeholder resources and have accessor activities that do the lifting? This is
         * notably a lot of footwork, but maybe it will be better this way?
         */
        Resource Earth = new Resource.Builder("Earth")
                .forSubsystem("Bodies")
                .withMin(new Time(args[0]))
                .withMax(new Time(args[1]))
                .build();

        Resource Moon = new Resource.Builder("Moon")
                .forSubsystem("Bodies")
                .withMin(new Time(args[0]))
                .withMax(new Time(args[1]))
                .build();

        myResources.addResource(Earth);
        myResources.addResource(Moon);

        // END BODIES //

        // Define instruments important to a mission

        // Define attitude maneuvers important to a mission

    }
}
