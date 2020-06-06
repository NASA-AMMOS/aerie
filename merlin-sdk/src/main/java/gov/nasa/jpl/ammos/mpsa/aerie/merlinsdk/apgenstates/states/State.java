package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;


public final class State {
    private double value;
    private String name;

    public State(final State other) {
        this.value = other.value;
        this.name = other.name;
    }

    public State(String name, double value){
        this.value = value;
        this.name = name;
    }

    public void add(final double delta) {
        this.value+= delta;
    }

    public void set(final double value) {
        this.value = value;
    }

    public double get() { return this.value; }

    public String name() { return this.name; }

    @Override
    public String toString() {
        return String.format("(Value: %s)", value);
    }
}
