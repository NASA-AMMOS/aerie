package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.streamline.core.*;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Dependencies;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.not;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static java.util.stream.Collectors.toMap;

/**
 * A utility class for building {@link MutableResource} "views"
 * - specialized versions of a {@link MutableResource} that don't own a cell themselves,
 * but rather transform their effects and pass them on to one or more other {@link MutableResource}s.
 */
public class Multiplexing {
    /**
     * Select one of several {@link MutableResource}s based on a selector resource.
     * Effects emitted on the resulting resoure will be directed to the selected underlying
     * {@link MutableResource} according to the selector.
     */
    public static <D extends Dynamics<?, D>, K> MutableResource<D> select(
            Resource<Discrete<K>> selector,
            Function<K, MutableResource<D>> options) {
      var selection = map(selector, options);
      var readOnlyResource = bind(selector, $ -> options.apply($.extract()));
      var result = new MutableResource<D>() {
        @Override
        public void emit(DynamicsEffect<D> effect) {
          var selection$ = Resources.currentValue(selection, null);
          if (selection$ != null) {
            selection$.emit(effect);
          } else {
            // TODO - log that the effect was not emitted because the selector was in error
          }
        }

        @Override
        public ErrorCatching<Expiring<D>> getDynamics() {
          return readOnlyResource.getDynamics();
        }
      };
      name(result, "Select %s against %s", selector, options);
      return result;
    }

    /**
     * Select one of several {@link MutableResource}s based on a selector resource.
     * Effects emitted on the resulting resoure will be directed to the selected underlying
     * {@link MutableResource} according to the selector.
     */
    public static <D extends Dynamics<?, D>, K> MutableResource<D> select(
            Resource<Discrete<K>> selector,
            Map<K, MutableResource<D>> options) {
        var result = select(selector, options::get);
        for (var possibleResult : options.values()) {
            Dependencies.addDependency(result, possibleResult);
        }
        name(result, "Select %s against %s", selector, options);
        return result;
    }

    /**
     * Multiplex between a set of source {@link MutableResource}s, based on a set of selector resources.
     * Generally, this is used when the selectors are guaranteed (by the caller) to take on distinct values,
     * though this is not required.
     */
    public static <D extends Dynamics<?, D>, K, J> Map<K, MutableResource<D>> multiplex(
            Map<K, Resource<Discrete<J>>> selectors,
            Map<J, MutableResource<D>> options) {
        return selectors.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), select(entry.getValue(), options)))
                .collect(toMap(Pair::getKey, Pair::getValue));
    }

    /**
     * Multiplex between a two source {@link MutableResource}s, based on a boolean selector resources.
     */
    public static <D extends Dynamics<?, D>> Map<Boolean, MutableResource<D>> multiplex(
            Resource<Discrete<Boolean>> selector,
            MutableResource<D> trueOption,
            MutableResource<D> falseOption) {
        return multiplex(
                Map.of(true, selector, false, not(selector)),
                Map.of(true, trueOption, false, falseOption));
    }
}
