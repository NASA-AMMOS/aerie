package gov.nasa.jpl.mpsa.old;

import gov.nasa.jpl.mpsa.conditions.Condition;

public interface SchedulerActivity {

    // all activities can have conditions, but schedulers MUST have conditions and we enforce this by making them
    // implement this method, whose value will be assigned to the condition variable
    Condition setCondition();

    // this method will automatically be entered into during modeling when the resource constraints are met - it can
    // do anything like affect resources to add new activities to the queue
    void okToSchedule();
}
