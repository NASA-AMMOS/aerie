package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;


public final class State {
    private double value;
    private String name;
    private StateModel stateModel;

    public State(final State other) {
        this.value = other.value;
        this.name = other.name;
        this.stateModel = other.stateModel;
    }

    public State(String name, double value, StateModel stateModel){
        this.value = value;
        this.name = name;
        this.stateModel = stateModel;
    }

    public void add(final double delta) {
        this.value+= delta;
        this.stateModel.logChangedValue(this.name, this.value);
    }

    public void set(final double value) {
        this.value = value;
        this.stateModel.logChangedValue(this.name, this.value);
    }

    public double get() { return this.value; }

    public String name() { return this.name; }

    @Override
    public String toString() {
        return String.format("(Value: %s)", value);
    }
}
