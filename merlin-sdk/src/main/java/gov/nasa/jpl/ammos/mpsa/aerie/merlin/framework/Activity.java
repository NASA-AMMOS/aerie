package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import java.util.List;

public interface Activity<Context, Resources> {
  void modelEffects(Context context, Resources resources);

  // TODO: Improve quality/fidelity of validation failures.
  default List<String> getValidationFailures() {
    return List.of();
  }
}
