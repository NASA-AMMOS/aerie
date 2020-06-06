package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states;

public class ConsumableState {

    private final String name;
    private final APGenStateFactory factory;

    public ConsumableState(String name, double value, APGenStateFactory factory){
        this.name = name;
        this.factory = factory;
    }

    public void add(double delta){
       this.factory.add(this.name, delta);
    }

    public double get(){
        return this.factory.get(this.name);
    }

}
