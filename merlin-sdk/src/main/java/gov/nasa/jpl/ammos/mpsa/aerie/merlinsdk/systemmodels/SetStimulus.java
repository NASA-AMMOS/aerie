package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

public final class SetStimulus implements Stimulus {
    private final Object newValue;

    public SetStimulus(final Object value) {
        this.newValue = value;
    }

    public <ResourceType> ResourceType getNewValue(Class<ResourceType> valueClass) {
        return valueClass.cast(this.newValue);
    }
}

final class AccumulateStimulus implements Stimulus {
    private final Object delta;

    public AccumulateStimulus(final Object delta) {
        this.delta = delta;
    }

    public <ResourceType> ResourceType getDelta(Class<ResourceType> deltaClass) {
        return deltaClass.cast(this.delta);
    }
}
