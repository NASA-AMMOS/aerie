package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

public class SettableEvent<T> implements Event<T> {

    private T value;
    private Time time;
    private String name;

    public SettableEvent(String name, T value, Time time){
        this.name = name;
        this.value = value;
        this.time = time;
    }

    @Override
    public String name(){
        return this.name;
    }

    @Override
    public T value() {
        return this.value;
    }

    @Override
    public Time time(){
        return this.time;
    }
}
