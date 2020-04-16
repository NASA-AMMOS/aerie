package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

public class SettableEvent<T> implements Event<T> {

    private T value;
    private Instant time;
    private String name;

    public SettableEvent(String name, T value, Instant time){
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
    public Instant time(){
        return this.time;
    }

    @Override
    public EventType eventType(){
        return EventType.SETTABLE;
    }

}
