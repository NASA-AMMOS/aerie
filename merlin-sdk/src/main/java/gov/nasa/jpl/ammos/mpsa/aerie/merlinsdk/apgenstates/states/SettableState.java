package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

public class SettableState {

    private final String name;
    private final APGenStateFactory factory;

    public SettableState(String name, double value, APGenStateFactory factory){
        this.name = name;
        this.factory = factory;
    }

    public void set(double value){
        this.factory.set(this.name, value);
    }

    public double get(){
        return this.factory.get(this.name);
    }

}
