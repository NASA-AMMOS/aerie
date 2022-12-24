package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.SimulationFailure;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class ThreadedSimulationAgent implements SimulationAgent {
  private /*sealed*/ interface SimulationRequest {
    record Simulate(PlanId planId, RevisionData revisionData, ResultsProtocol.WriterRole writer) implements SimulationRequest {}

    record Terminate() implements SimulationRequest {}
  }


  private final BlockingQueue<SimulationRequest> requestQueue;

  public ThreadedSimulationAgent(final BlockingQueue<SimulationRequest> requestQueue) {
    this.requestQueue = Objects.requireNonNull(requestQueue);
  }

  public static ThreadedSimulationAgent spawn(final String threadName, final SimulationAgent simulationAgent) {
    final var requestQueue = new LinkedBlockingQueue<SimulationRequest>();

    final var thread = new Thread(new Worker(requestQueue, simulationAgent));
    thread.setName(threadName);
    thread.start();

    return new ThreadedSimulationAgent(requestQueue);
  }

  @Override
  public void simulate(final PlanId planId, final RevisionData revisionData, final ResultsProtocol.WriterRole writer)
  throws InterruptedException
  {
    this.requestQueue.put(new SimulationRequest.Simulate(planId, revisionData, writer));
  }

  public void terminate() throws InterruptedException {
    this.requestQueue.put(new SimulationRequest.Terminate());
  }


  private static final class Worker implements Runnable {
    private final BlockingQueue<SimulationRequest> requestQueue;
    private final SimulationAgent simulationAgent;

    public Worker(
        final BlockingQueue<SimulationRequest> requestQueue,
        final SimulationAgent simulationAgent)
    {
      this.requestQueue = Objects.requireNonNull(requestQueue);
      this.simulationAgent = Objects.requireNonNull(simulationAgent);
    }

    @Override
    public void run() {
      while (true) {
        try {
          final var request = this.requestQueue.take();

          if (request instanceof SimulationRequest.Simulate req) {
            try {
              this.simulationAgent.simulate(req.planId(), req.revisionData(), new ThreadedWriter(req.writer()));
            } catch (final Throwable ex) {
              ex.printStackTrace(System.err);
              req.writer().failWith(b -> b
                  .type("UNEXPECTED_SIMULATION_EXCEPTION")
                  .message("Something went wrong while simulating")
                  .trace(ex));
            }
            // continue
          } else if (request instanceof SimulationRequest.Terminate) {
            break;
          } else {
            throw new UnexpectedSubtypeError(SimulationRequest.class, request);
          }
        } catch (final Exception ex) {
          ex.printStackTrace(System.err);
          // continue
        }
      }
    }

    private static class ThreadedWriter implements ResultsProtocol.WriterRole {
      private final ResultsProtocol.WriterRole writer;
      private final AtomicBoolean isCanceled = new AtomicBoolean(false);
      private final AtomicLong keepAlive = new AtomicLong();
      private Optional<Thread> cancelPoller = Optional.empty();
      private final AtomicReference<Optional<Throwable>> propagatableThrowable = new AtomicReference<>(Optional.empty());

      ThreadedWriter(final ResultsProtocol.WriterRole writer) {
        this.writer = writer;
      }

      @Override
      public boolean isCanceled() {
        this.keepAlive.set(System.nanoTime());
        // if no thread, spin one up
        if (cancelPoller.isEmpty() || !cancelPoller.get().isAlive()) {
          cancelPoller = Optional.of(new Thread(() -> {
            try {
              while (System.nanoTime() - keepAlive.get() < Duration.of(15, ChronoUnit.SECONDS).toNanos()) {
                Thread.sleep(2000);
                if (writer.isCanceled()) {
                  isCanceled.set(true);
                  break;
                }
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              // Exit gracefully
            } catch (Throwable e) {
              propagatableThrowable.set(Optional.of(e)); // A failure of the cancelPoller should stop the simulation... right?
              throw e;
            }
          }));
          cancelPoller.get().start();
        }

        final var throwable = propagatableThrowable.get();
        if (throwable.isPresent()) {
          throw new Error(throwable.get());
        }
        return this.isCanceled.get();
      }

      @Override
      public void succeedWith(final SimulationResults results) {
        cancelPoller.ifPresent(Thread::interrupt);
        writer.succeedWith(results);
      }

      @Override
      public void failWith(final SimulationFailure reason) {
        cancelPoller.ifPresent(Thread::interrupt);
        writer.failWith(reason);
      }
    }
  }
}
