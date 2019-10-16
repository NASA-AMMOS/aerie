package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.BasicState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import java.util.List;

public class BananaStates implements StateContainer {
    public SettableState<Double> fruitState = new BasicState<>("fruit", 4.0);
    public SettableState<Double> peelState = new BasicState<>("peel", 4.0);

    @Override
    public List<State<?>> getStateList() {
        return List.of(fruitState, peelState);
    }

}
