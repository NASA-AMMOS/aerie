package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

public abstract class Task<$Schema, Event, TaskSpec, Resources>
    extends Module<$Schema, Event, TaskSpec>
{
  public abstract void run(Resources resources);
}
