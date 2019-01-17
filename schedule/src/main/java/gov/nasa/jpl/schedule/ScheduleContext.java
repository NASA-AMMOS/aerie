package gov.nasa.jpl.schedule;

public class ScheduleContext {

    private Strategy strategy;

    public ScheduleContext(Strategy strategy) {
        this.strategy = strategy;
    }

    public void execute(){
        this.strategy.execute();
    }
}
