package gov.nasa.jpl.aerie.scheduler;

public abstract class Effector<T extends Comparable<T>> {

    State<T> state;
    ActivityType actType;

    public Effector(State<T> localState, ActivityType actType){
        this.state = localState;
        this.actType = actType;
    }

    public abstract T getEffectAtTime(Time t);



}


