package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class ThreadedSimulationAgent {
  public /*sealed*/ interface SimulationRequest {
    record Simulate(String planId, long planRevision, ResultsProtocol.WriterRole writer) implements SimulationRequest {}

    record Terminate() implements SimulationRequest {}
  }


  private final BlockingQueue<SimulationRequest> requestQueue;

  private ThreadedSimulationAgent(final BlockingQueue<SimulationRequest> requestQueue) {
    this.requestQueue = Objects.requireNonNull(requestQueue);
  }

  public static ThreadedSimulationAgent spawn(final String threadName, final RunSimulationAction simulationAction) {
    final var requestQueue = new LinkedBlockingQueue<SimulationRequest>();

    final var thread = new Thread(new Worker(requestQueue, simulationAction));
    thread.setName(threadName);
    thread.start();

    return new ThreadedSimulationAgent(requestQueue);
  }

  public void simulate(final String planId, final long planRevision, final ResultsProtocol.WriterRole writer)
  throws InterruptedException
  {
    this.requestQueue.put(new SimulationRequest.Simulate(planId, planRevision, writer));
  }

  public void terminate() throws InterruptedException {
    this.requestQueue.put(new SimulationRequest.Terminate());
  }


  private static final class Worker implements Runnable {
    private final BlockingQueue<SimulationRequest> requestQueue;
    private final RunSimulationAction simulationAction;

    public Worker(
        final BlockingQueue<SimulationRequest> requestQueue,
        final RunSimulationAction simulationAction)
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
            final RunSimulationAction.Response response;
            try {
              response = this.simulationAction.run(req.planId(), req.planRevision());
            } catch (final Throwable ex) {
              ex.printStackTrace(System.err);
              req.writer().failWith(ex.getMessage());
              continue;
            }

            if (response instanceof RunSimulationAction.Response.Failed res) {
              req.writer().failWith(res.reason());
            } else if (response instanceof RunSimulationAction.Response.Success res) {
              req.writer().succeedWith(res.results());
            } else {
              final var ex = new UnexpectedSubtypeError(RunSimulationAction.Response.class, response);
              req.writer().failWith(ex.getMessage());
              throw ex;
            }
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
