package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc.GNCStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.ArrayList;
import java.util.List;

public class SetScanAxis implements Activity<GNCStates> {


    //TODO: get annotation processor to accept other objects besides primitives such as Vector3D
    @Parameter
    double x_value = 1.0;

    @Parameter
    double y_value = 0.0;

    @Parameter
    double z_value = 0.0;

    public SetScanAxis(double x_value, double y_value, double z_value){
        this.x_value = x_value;
        this.y_value = y_value;
        this.z_value = z_value;
    }

    @Override
    public List<String> validateParameters(){
        final List<String> failures = new ArrayList<>();

        if ((x_value == 0) && (y_value == 0) && (z_value == 0)){
            failures.add("cannot accept a zero vector");
        }
        return failures;
    }

    @Override
    public void modelEffects(SimulationContext<GNCStates> ctx, GNCStates states){
        Vector3D gncVector = states.getVectorState(GNCStates.rollAxisName).get();
        Vector3D axisVector = new Vector3D(this.x_value, this.y_value, this.z_value).normalize();
        gncVector = axisVector;
    }
}