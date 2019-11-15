package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import java.util.List;

/**
 * implements a mocked-out state container for use in unit tests
 *
 * the mock contains no actual states of its own
 */
public class MockEmptyStateContainer implements StateContainer {

    /**
     * reports no internal states at all
     *
     * @return an empty list of states in this container
     */
    @Override
    public List<State<?>> getStateList() {
        return List.of();
    }
}
