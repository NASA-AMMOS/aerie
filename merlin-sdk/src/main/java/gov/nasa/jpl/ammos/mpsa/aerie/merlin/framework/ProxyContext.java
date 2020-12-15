package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Map;

public final class ProxyContext<$Schema>
    implements Context<$Schema>
{
  private Context<$Schema> context = null;

  public void setTarget(final Context<$Schema> context) {
    this.context = context;
  }

  public Context<$Schema> getTarget() {
    return this.context;
  }


  @Override
  public History<? extends $Schema> now() {
    return this.context.now();
  }

  @Override
  public final <Event> void emit(final Event event, final Query<? super $Schema, Event, ?> query) {
    this.context.emit(event, query);
  }

  @Override
  public final <Spec> String spawn(final Spec spec, final TaskSpecType<? super $Schema, Spec> type) {
    return this.context.spawn(spec, type);
  }

  @Override
  public final <Spec> String defer(final Duration duration, final Spec spec, final TaskSpecType<? super $Schema, Spec> type) {
    return this.context.defer(duration, spec, type);
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    return this.context.spawn(type, arguments);
  }

  @Override
  public final void delay(final Duration duration) {
    this.context.delay(duration);
  }

  @Override
  public final void waitFor(final String id) {
    this.context.waitFor(id);
  }

  @Override
  public void waitUntil(final Condition<$Schema> condition) {
    this.context.waitUntil(condition);
  }
}
