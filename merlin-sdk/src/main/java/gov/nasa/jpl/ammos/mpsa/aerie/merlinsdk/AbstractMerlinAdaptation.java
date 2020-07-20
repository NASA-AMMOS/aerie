package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapperLoader;

/**
 * An abstract base class for defining implementations of {@link MerlinAdaptation}.
 *
 * <p>
 * This class provides default implementations of some of the methods
 * required by the {@link MerlinAdaptation} interface.
 * </p>
 *
 * @param <Event> The simulation event type produced by this adaptation.
 */
public abstract class AbstractMerlinAdaptation<Event> implements MerlinAdaptation<Event> {
  /**
   * Invokes {@link ActivityMapperLoader#loadActivityMapper(Class)} with this class.
   *
   * <p>
   * This implementation uses reflection to load all activity mappers from a text-file registry in the adaptation.
   * See {@link ActivityMapperLoader#loadActivityMapper(Class)} for additional details.
   * </p>
   *
   * @return A composite activity mapper representing all registered mappers
   */
  @Override
  public final ActivityMapper getActivityMapper() {
    try {
      return ActivityMapperLoader.loadActivityMapper(this.getClass());
    } catch (final ActivityMapperLoader.ActivityMapperLoadException ex) {
      throw new Error(ex);
    }
  }
}
