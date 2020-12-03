package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities.FooActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

import java.util.Map;

// TODO: Automatically generate at compile time.
public abstract class Module<$Schema>
    extends gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module<$Schema>
{
  protected final String spawn(final FooActivity activity) {
    // TODO: we should provide spec and TaskSpecType to this spawn.
    //  This hack allows for exercising spawn mechanics in the engine.
    return spawn("foo", Map.of(
        "x", SerializedValue.of(activity.x),
        "y", SerializedValue.of(activity.y)));
  }

  protected void call(final FooActivity activity) {
    waitFor(spawn(activity));
  }
}
