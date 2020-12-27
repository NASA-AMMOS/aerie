package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.BiteBananaActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.PeelBananaActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers.BiteBananaActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers.PeelBananaActivityMapper;
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


  protected final String spawn(final BiteBananaActivity activity) {
    return spawn(new BiteBananaActivityMapper().getName(), new BiteBananaActivityMapper().getArguments(activity));
  }

  protected final String spawn(final PeelBananaActivity activity) {
    return spawn(new PeelBananaActivityMapper().getName(), new PeelBananaActivityMapper().getArguments(activity));
  }

  protected final String defer(final Duration duration, final BiteBananaActivity activity) {
    return defer(
        duration,
        new BiteBananaActivityMapper().getName(),
        new BiteBananaActivityMapper().getArguments(activity));
  }

  protected final String defer(final Duration duration, final PeelBananaActivity activity) {
    return defer(
        duration,
        new PeelBananaActivityMapper().getName(),
        new PeelBananaActivityMapper().getArguments(activity));
  }

  protected void call(final BiteBananaActivity activity) {
    waitFor(spawn(activity));
  }

  protected void call(final PeelBananaActivity activity) {
    waitFor(spawn(activity));
  }
}
