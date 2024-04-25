package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduling.EditablePlan;
import gov.nasa.jpl.aerie.scheduling.Procedure;
import gov.nasa.jpl.aerie.scheduling.plan.Edit;
import gov.nasa.jpl.aerie.scheduling.plan.NewDirective;
import gov.nasa.jpl.aerie.scheduling.simulation.SimulateOptions;
import gov.nasa.jpl.aerie.timeline.CollectOptions;
import gov.nasa.jpl.aerie.timeline.Duration;
import gov.nasa.jpl.aerie.timeline.Interval;
import gov.nasa.jpl.aerie.timeline.collections.Directives;
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyDirective;
import gov.nasa.jpl.aerie.timeline.plan.SimulationResults;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record SampleProcedure() implements Procedure {
  @Override
  public void run(@NotNull final EditablePlan plan, @NotNull final CollectOptions options) {

  }

  public static void main(String[] args) throws ProcedureLoader.ProcedureLoadException {
    var jarPath = Path.of("/Users/dailis/projects/aerie/worktrees/feature/procedural-scheduling/procedural/examples/foo-procedures/build/libs/foo-procedures-SampleProcedure-procedure.jar");
    var procedure = ProcedureLoader.loadProcedure(jarPath);
    procedure.run(new EditablePlan() {
      @Nullable
      @Override
      public SimulationResults latestResults() {
        return null;
      }

      @Override
      public long create(@NotNull final NewDirective directive) {
        System.out.println("Create!");
        return 0;
      }

      @Override
      public void commit() {

      }

      @NotNull
      @Override
      public List<Edit> rollback() {
        return null;
      }

      @NotNull
      @Override
      public SimulationResults simulate(@NotNull final SimulateOptions options) {
        return null;
      }

      @Override
      public int getId() {
        return 0;
      }

      @NotNull
      @Override
      public Interval totalBounds() {
        return null;
      }

      @NotNull
      @Override
      public Duration toRelative(@NotNull final Instant abs) {
        return null;
      }

      @NotNull
      @Override
      public Instant toAbsolute(@NotNull final Duration rel) {
        return null;
      }

      @NotNull
      @Override
      public <A> Directives<A> directives(
          @Nullable final String type,
          @NotNull final Function1<? super SerializedValue, ? extends A> deserializer)
      {
        return null;
      }

      @NotNull
      @Override
      public Directives<AnyDirective> directives(@NotNull final String type) {
        return null;
      }

      @NotNull
      @Override
      public Directives<AnyDirective> directives() {
        return null;
      }
    }, null);
  }
}
