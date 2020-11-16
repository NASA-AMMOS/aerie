package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

public abstract class Task<$Schema, Event, Activity, Resources>
    extends Module<$Schema, Event, Activity>
{
  public abstract void run(Resources resources);
}
