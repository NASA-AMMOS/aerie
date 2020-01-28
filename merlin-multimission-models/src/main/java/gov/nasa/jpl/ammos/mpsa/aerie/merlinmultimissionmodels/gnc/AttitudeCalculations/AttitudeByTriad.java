package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc.AttitudeCalculations;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class AttitudeByTriad {

    /**
     * Uses the triad method to calculate the current attitude of the S/C
     *
     * @param primary_b This is the primary vector you will use to calculate the S/C's attitude.
     *                  Use your most accurate measurement for this parameter (e.g. vector to the sun as opposed to magnetic field).
     *                  This vector is measured in your spacecraft body frame, and is given by known measurements from the spacecraft.
     * @param secondary_n This is the secondary vector you will use to calculate the S/C's attutude.
     * @param primary_n
     * @param secondary_n
     */

    public static void triad(Vector3D primary_b, Vector3D secondary_b, Vector3D primary_n, Vector3D secondary_n){

    }





}
