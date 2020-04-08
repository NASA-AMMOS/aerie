package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.BasicState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import java.util.List;

public final class BananaStates {
    private BananaStates() {}

    // TODO: stuff these in dynamic variables because they are their own state models.
    //   also they aren't really state models, it's a hold-over from before we had state models.
    public static final SettableState<Double> fruitState = new BasicState<>("fruit", 4.0);
    public static final SettableState<Double> peelState = new BasicState<>("peel", 4.0);
}
