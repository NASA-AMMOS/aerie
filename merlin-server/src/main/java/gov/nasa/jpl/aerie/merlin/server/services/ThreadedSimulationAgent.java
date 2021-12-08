package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class ThreadedSimulationAgent implements SimulationAgent {
  private /*sealed*/ interface SimulationRequest {
    record Simulate(String planId, RevisionData revisionData, ResultsProtocol.WriterRole writer) implements SimulationRequest {}

    record Terminate() implements SimulationRequest {}
  }


  private final BlockingQueue<SimulationRequest> requestQueue;

  private ThreadedSimulationAgent(final BlockingQueue<SimulationRequest> requestQueue) {
    this.requestQueue = Objects.requireNonNull(requestQueue);
  }

  public static ThreadedSimulationAgent spawn(final String threadName, final SimulationAgent simulationAction) {
    final var requestQueue = new LinkedBlockingQueue<SimulationRequest>();

    final var thread = new Thread(new Worker(requestQueue, simulationAction));
    thread.setName(threadName);
    thread.start();

    return new ThreadedSimulationAgent(requestQueue);
  }

  @Override
  public void simulate(final String planId, final RevisionData revisionData, final ResultsProtocol.WriterRole writer)
  throws InterruptedException
  {
    this.requestQueue.put(new SimulationRequest.Simulate(planId, revisionData, writer));
  }

  public void terminate() throws InterruptedException {
    this.requestQueue.put(new SimulationRequest.Terminate());
  }


  private static final class Worker implements Runnable {
    private final BlockingQueue<SimulationRequest> requestQueue;
    private final SimulationAgent simulationAction;

    public Worker(
        final BlockingQueue<SimulationRequest> requestQueue,
        final SimulationAgent simulationAction)
    {
      this.requestQueue = Objects.requireNonNull(requestQueue);
      this.simulationAction = Objects.requireNonNull(simulationAction);
    }

    @Override
    public void run() {
      while (true) {
        try {
          final var request = this.requestQueue.take();

          if (request instanceof SimulationRequest.Simulate req) {
            try {
              this.simulationAction.simulate(req.planId(), req.revisionData(), req.writer());
            } catch (final Throwable ex) {
              ex.printStackTrace(System.err);
              req.writer().failWith(ex.getMessage());
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
  }
}
