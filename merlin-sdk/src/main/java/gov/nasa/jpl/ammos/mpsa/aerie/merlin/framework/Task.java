package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

public abstract class Task<$Schema, Event, Activity, Resources extends Module<$Schema, Event, Activity>>
    extends Module<$Schema, Event, Activity>
{
  public final void run(final Context<$Schema, Event, Activity> context, final Resources resources) {
    final var oldTaskContext = this.setContext(context);
    final var oldResourcesContext = resources.setContext(context);

    try {
      this.run(resources);
    } finally {
      this.setContext(oldTaskContext);
      resources.setContext(oldResourcesContext);
    }
  }

  protected abstract void run(Resources resources);
}
