package gov.nasa.jpl.ammos.mpsa.aerie.banananation2.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation2.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;

// TODO: Automatically generate at compile time.
public abstract class Task<$Schema> extends Module<$Schema> {
  private final ProxyContext<$Schema> context;

  protected Task(final ProxyContext<$Schema> context) {
    super(context);
    this.context = context;
  }

  protected Task() {
    this(new ProxyContext<>());
  }

  public void setContext(final Context<$Schema> context) {
    this.context.setTarget(context);
  }

  public abstract void run(BanananationResources<$Schema> resources);
}
