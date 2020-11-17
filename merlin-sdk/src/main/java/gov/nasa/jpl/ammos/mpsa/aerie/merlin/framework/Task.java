package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

public abstract class Task<$Schema, TaskSpec, Resources>
    extends Module<$Schema, TaskSpec>
{
  public abstract void run(Resources resources);
}
