package gov.nasa.jpl.ammos.mpsa.aerie.simulation.http;

import gov.nasa.jpl.ammos.mpsa.aerie.simulation.agents.Simulator;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.mocks.DevAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.mocks.DevPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.simulation.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import io.javalin.Javalin;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public final class SimulationBindings {
  public void registerRoutes(final Javalin javalin) {
    javalin.routes(() -> {
      path("simulations", () -> {
        post(this::createSimulation);
      });
    });

    javalin.exception(InvalidEntityException.class, (ex, ctx) -> ctx
        // If the request body entity is a legal JsonValue but not a legal object of the type we expect, then this exception
        // is expected.
        .status(400)
        .result(ResponseSerializers.serializeInvalidEntityException(ex).toString())
        .contentType("application/json")
    );
  }

  private void createSimulation(final Context ctx) throws InvalidEntityException {
    final CreateSimulationMessage request = RequestDeserializers
        .deserializeCreateSimulationMessage(ctx.body())
        .getSuccessOrThrow(InvalidEntityException::new);

    // TODO: Load the plan identified by the request.
    final Plan plan = new DevPlan();
    // TODO: Load the adaptation identified in the plan.
    final MerlinAdaptation adaptation = new DevAdaptation();

    // SAFETY: The plan and the adaptation must use the same StateContainer type.
    @SuppressWarnings("unchecked")
    final var simulator = new Simulator(plan, adaptation);
    final var results = simulator.run();

    ctx.result(ResponseSerializers.serializeSimulationResults(results).toString());
  }
}
