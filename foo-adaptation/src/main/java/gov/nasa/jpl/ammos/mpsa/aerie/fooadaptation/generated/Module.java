package gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.mappers.FooActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.function.Supplier;

// TODO: Automatically generate at compile time.
public abstract class Module<$Schema>
    extends gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module<$Schema>
{
  protected Module(final Supplier<? extends Context<$Schema>> context) {
    super(context);
  }

  protected Module(final ResourcesBuilder.Cursor<$Schema> builder) {
    super(builder);
  }


  protected final String spawn(final FooActivity activity) {
    return spawn(new FooActivityMapper().getName(), new FooActivityMapper().getArguments(activity));
  }

  protected final String defer(final Duration duration, final FooActivity activity) {
    return defer(duration, new FooActivityMapper().getName(), new FooActivityMapper().getArguments(activity));
  }

  protected void call(final FooActivity activity) {
    waitFor(spawn(activity));
  }
}
