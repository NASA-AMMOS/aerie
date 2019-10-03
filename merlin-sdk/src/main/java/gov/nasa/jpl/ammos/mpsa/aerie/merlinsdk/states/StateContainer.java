package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.List;

/**
 * An adaptation-specific state index structure
 */
public interface StateContainer {

    /**
     * Returns a list of all of the state objects in the index structure
     * 
     * @return a list of all states within the structure
     */
    public List<State<?>> getStateList();
}
