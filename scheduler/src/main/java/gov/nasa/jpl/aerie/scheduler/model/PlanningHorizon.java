package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.NotNull;

public final class PlanningHorizon{

  private final Time start;
  private final Time end;

  private final Window aerieHorizon;

  public PlanningHorizon(@NotNull Time start, @NotNull Time end){
    this.start= start;
    this.end = end;
    aerieHorizon = Window.betweenClosedOpen(Duration.ZERO,end.minus(start));
  }

  public Window getHor(){
    return aerieHorizon;
  }

  public Time getStartHuginn(){
    return start;
  }
  public Time getEndHuginn(){
    return end;
  }

  public boolean contains(Duration time){
    var huginnTime  = toTime(time);
    return start.smallerThan(huginnTime) && end.biggerThan(huginnTime);
  }

  public Duration getStartAerie(){
    return aerieHorizon.start;
  }

  public Duration getEndAerie(){
    return aerieHorizon.end;
  }

  public Duration getAerieHorizonDuration(){
    return getEndAerie();
  }

  public Time toTime(Duration dur){
    return start.plus(dur);
  }

  public Duration toDur(Time t){
    return t.minus(start);
  }

}
