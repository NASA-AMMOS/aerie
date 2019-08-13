package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import java.util.List;

public interface StateContainer {
    public List<SettableState<?>> getStateList();
}