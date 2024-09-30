package gov.nasa.jpl.aerie.merlin.driver.retracing.engine.tracing;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executor;

public record TaskRestarter<T>(TaskResumptionInfo<T> resumptionInfo, Executor executor) {
  @SuppressWarnings("unchecked")
  public TaskStatus<T> restart(Scheduler scheduler) {
    final var reads = resumptionInfo.reads();
    final var numSteps = resumptionInfo.numSteps().getValue();
    Task<T> task = resumptionInfo.restarter().create(executor);
    final var readIterator = new ArrayList<>(reads).iterator();
    TaskStatus<T> taskStatus = null;
    for (int i = 0; i < numSteps + 1; i++) {
      taskStatus = task.step(new Scheduler() {
        @Override
        public <State> State get(final CellId<State> cellId) {
          if (readIterator.hasNext()) {
            return (State) readIterator.next();
          } else {
            return scheduler.get(cellId);
          }
        }

        @Override
        public <Event> void emit(final Event event, final Topic<Event> topic) {
          if (!readIterator.hasNext()) {
            scheduler.emit(event, topic);
          }
        }

        @Override
        public void spawn(InSpan childSpan, final TaskFactory<?> task) {
          if (!readIterator.hasNext()) {
            scheduler.spawn(childSpan, task);
          }
        }
      });
    }
    return Objects.requireNonNull(taskStatus);
  }
}
