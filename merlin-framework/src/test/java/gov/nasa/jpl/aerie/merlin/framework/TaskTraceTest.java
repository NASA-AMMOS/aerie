package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import org.junit.jupiter.api.Test;

class TaskTraceTest {

  /*
  Use cases:
  1. Initial run: merely reporting
  2. Imitation: traverse the rbt, comparing live read values with historical read values
  3. Rerun a task from start to present, feeding it read values and ignoring its actions
     - once it reaches present, it will continue running until it yields. Need to forward its actions to real sim engine
     - after this point, forward all of that task's actions to the simulation engine
   */

  @Test
  void foo() {

    final Topic<Integer> topic1 = new Topic<>();
    final CellId<Integer> cellId = new CellId<Integer>() {};

    final var realScheduler = new Scheduler() {
      @Override
      public <State> State get(final CellId<State> cellId) {
        return (State) new Object();
      }

      @Override
      public <Event> void emit(final Event event, final Topic<Event> topic) {

      }

      @Override
      public void spawn(final TaskFactory<?> task) {

      }
    };

    final TaskTrace rbt;
    // Initial run
    {
      final var writer = TaskTrace.writer();
      writer.emit(1, topic1);
      writer.emit(2, topic1);
      writer.emit(3, topic1);
      writer.read(cellId, 0);
      writer.emit(4, topic1);
      writer.emit(5, topic1);
      writer.emit(6, topic1);
      rbt = writer.get();
    }

    // Second run
    {
      final var cursor = TaskTrace.cursor(rbt);
      while (true) {
        final var nextAction = cursor.nextAction();
        if (nextAction instanceof Action.Emit<?> emit) {
          // emit the event
        } else if (nextAction instanceof Action.Yield yield) {
          // yield the taskstatus
        } else if (nextAction instanceof Action.Read read) {
          final var currentReadValue = new Object(); // use read.topic() to get the current read value
          final var taskResumeInfo$ = cursor.read(currentReadValue);
          if (taskResumeInfo$.isPresent()) {
            final var taskResumeInfo = taskResumeInfo$.get();
            final var readIterator = taskResumeInfo.reads().iterator();
            new Scheduler() {
              @Override
              public <State> State get(final CellId<State> cellId) {
                if (readIterator.hasNext()) {
                  return (State) readIterator.next();
                } else {
                  final State state = realScheduler.get(cellId);
                  taskResumeInfo.writer().read(cellId, state);
                  return state;
                }
              }

              @Override
              public <Event> void emit(final Event event, final Topic<Event> topic) {
                if (readIterator.hasNext()) {
                  // Ignore
                } else {
                  taskResumeInfo.writer().emit(event, topic);
                  realScheduler.emit(event, topic);
                }
              }

              @Override
              public void spawn(final TaskFactory<?> task) {
                if (readIterator.hasNext()) {
                  // Ignore
                } else {
                  taskResumeInfo.writer().spawn();
                  realScheduler.spawn(task);
                }
              }
            };
            for (int i = 0; i < taskResumeInfo.numSteps(); i++) {
              // step
            }
            // restart the task, run it up to the present time
          }
        } else {
          throw new Error("Unhandled variant of Action: " + nextAction);
        }
      }
      // action is read, or we're out of actions
      // TODO if we've been asked to simulate past the known extent of a previously unfinished task, maybe we start it
      //  up again just in case? Especially if that extent is the result of delays or calls, rather than conditions



//      writer.read(topic2, 1);
//      writer.emit(topic1, 7);
//      writer.emit(topic1, 8);
//      writer.emit(topic1, 9);

//      assertTrue(true);
    }
  }
}
