package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

public abstract class Task<$Schema, Event, Activity, Resources>
    extends Module<$Schema, Event, Activity>
{
  public final void run(final Context<$Schema, Event, Activity> context, final Resources resources) {
    this.setContext(context);
    try {
      this.run(resources);
    } finally {
      this.setContext(null);
    }
  }

  protected abstract void run(Resources resources);
}
