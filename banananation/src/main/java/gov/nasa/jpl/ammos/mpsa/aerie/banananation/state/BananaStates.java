package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import java.util.List;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

public class BananaStates implements StateContainer {
    public SettableState<Double> fruitState = new SettableState<>(4.0);
    public SettableState<Double> peelState = new SettableState<>(4.0);

    @Override
    public List<SettableState<?>> getStateList() {
        return List.of(fruitState, peelState);
    }

}
