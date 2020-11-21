package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

public abstract class Task<$Schema, Resources> extends Module<$Schema> {
  public abstract void run(Resources resources);
}
