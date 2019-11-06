package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.BasicState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GNCStates implements StateContainer {

    public static String scanAxisName = "roll axis vector";
    public static BasicState<Vector3D> scanAxis = new BasicState<>(scanAxisName, new Vector3D(1,0,0));

    private static List<State<?>> stateList = new ArrayList<>();
    private static Map<String, BasicState<Vector3D>> vectorStateMap = new HashMap<>();


    public GNCStates(){
        stateList.add(scanAxis);
        vectorStateMap.put(scanAxisName, this.scanAxis);
    }

    public BasicState<Vector3D> getVectorState(String name){
        return vectorStateMap.get(name);
    }

    @Override
    public List<State<?>> getStateList() {
        return this.stateList;
    }
}
