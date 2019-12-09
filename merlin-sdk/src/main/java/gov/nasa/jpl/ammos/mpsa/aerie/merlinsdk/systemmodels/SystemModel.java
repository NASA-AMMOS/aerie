package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SystemModel {

    /**
     * This method should be implemented by the System Modeler, who is an expert in this system
     * The System Modeler should be aware that their model will be evaluated between Times x and y
     * The System Modeler should declare any variables used in their progress method as parameters outside this method
     * @param x
     * @param y
     */
    public void progress(Time x, Time y);

    /**
     * The mission modeler should implement this method.
     * The mission modeler should pass in the relevant state container, and locally reference either the entire container or the relevant states
     * @param stateContainer
     */
    public void setStateDependencies(StateContainer stateContainer);

    /**
     * The mission modeler should implement this method.
     * The mission modeler should set any parameters they want to track to the value of the state representing said parameter
     */
    public void pullHooks();

    /**
     * The mission modeler should implement this method.
     * The mission modeler should set all the dependent states' values to the parameters representing said states
     */
    public void pushUpdates();
}
