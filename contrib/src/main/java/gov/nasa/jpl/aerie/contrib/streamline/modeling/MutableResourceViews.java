package gov.nasa.jpl.aerie.contrib.streamline.modeling;

import gov.nasa.jpl.aerie.contrib.streamline.core.*;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Dependencies;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.NEVER;
import static gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource.resource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.map;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.pure;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad.bind;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.name;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.discreteResource;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.not;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static java.util.stream.Collectors.toMap;

// TODO - find a better place to put this.
//   It's at the top level because it didn't really fit in any other place.
//   It doesn't belong in the core package, because it's not core functionality - end users could implement everything
//   in this class without disrupting the library at all.
//   I thought about putting it in Resources, but this is about MutableResources in particular, not any Resource.
/**
 * A utility class for building {@link MutableResource} "views"
 * - specialized versions of a {@link MutableResource} that don't own a cell themselves,
 * but rather transform their effects and pass them on to one or more other {@link MutableResource}s.
 */
public class MutableResourceViews {
    /**
     * Select one of several {@link MutableResource}s based on a selector resource.
     * Effects emitted on the resulting resoure will be directed to the selected underlying
     * {@link MutableResource} according to the selector.
     * <p>
     *     Note: "Effects" which extend over time, like
     *     {@link gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialEffects#using},
     *     and actually emit multiple effects over time to their target resource,
     *     may not behave correctly when given the result of this function.
     *     If the selector changes while such an extended effect is taking place,
     *     the atomic effects emitted by that extended effect may be routed to different backing MutableResources,
     *     which is likely not the desired behavior.
     *     In such cases, it may be safer to choose the backing resource directly in the task,
     *     insulating the extended effect from changes in the selector.
     * </p>
     */
    public static <D extends Dynamics<?, D>, K> MutableResource<D> select(
            Resource<Discrete<K>> selector,
            Function<K, MutableResource<D>> options) {
      var selection = map(selector, options);
      var readOnlyResource = bind(selector, $ -> options.apply($.extract()));
      var result = new MutableResource<D>() {
        @Override
        public void emit(DynamicsEffect<D> effect) {
          var selection$ = currentValue(selection, null);
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
     * Effects emitted on the resulting resource will be directed to the selected underlying
     * {@link MutableResource} according to the selector.
     * <p>
     *     Note: "Effects" which extend over time, like
     *     {@link gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialEffects#using},
     *     and actually emit multiple effects over time to their target resource,
     *     may not behave correctly when given the result of this function.
     *     If the selector changes while such an extended effect is taking place,
     *     the atomic effects emitted by that extended effect may be routed to different backing MutableResources,
     *     which is likely not the desired behavior.
     *     In such cases, it may be safer to choose the backing resource directly in the task,
     *     insulating the extended effect from changes in the selector.
     * </p>
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
     * <p>
     *     Note: "Effects" which extend over time, like
     *     {@link gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialEffects#using},
     *     and actually emit multiple effects over time to their target resource,
     *     may not behave correctly when given the result of this function.
     *     If the selector changes while such an extended effect is taking place,
     *     the atomic effects emitted by that extended effect may be routed to different backing MutableResources,
     *     which is likely not the desired behavior.
     *     In such cases, it may be safer to choose the backing resource directly in the task,
     *     insulating the extended effect from changes in the selector.
     * </p>
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
     * <p>
     *     Note: "Effects" which extend over time, like
     *     {@link gov.nasa.jpl.aerie.contrib.streamline.modeling.polynomial.PolynomialEffects#using},
     *     and actually emit multiple effects over time to their target resource,
     *     may not behave correctly when given the result of this function.
     *     If the selector changes while such an extended effect is taking place,
     *     the atomic effects emitted by that extended effect may be routed to different backing MutableResources,
     *     which is likely not the desired behavior.
     *     In such cases, it may be safer to choose the backing resource directly in the task,
     *     insulating the extended effect from changes in the selector.
     * </p>
     */
    public static <D extends Dynamics<?, D>> Map<Boolean, MutableResource<D>> multiplex(
            Resource<Discrete<Boolean>> selector,
            MutableResource<D> trueOption,
            MutableResource<D> falseOption) {
        return multiplex(
                Map.of(true, selector, false, not(selector)),
                Map.of(true, trueOption, false, falseOption));
    }

    /**
     * Create a {@link MutableResourceFactory}, which can in turn dynamically allocate {@link MutableResource}s
     * during simulation.
     * <p>
     *     IMPORTANT! This approach comes with some significant drawbacks, and should only be used if
     *     constructing the needed {@link MutableResource} during initialization is unworkable.
     *     Those drawbacks include:
     *     1) Safety - {@link MutableResourceFactory#create} cannot be safely replayed.
     *        That means special care must be taken to limit how this method is called.
     *     2) Performance - Dynamically-allocated {@link MutableResource}s sidestep important optimizations made by Aerie.
     *        Those optimizations limit the amount of work done by this resource and all resources downstream of this.
     *        Dynamic allocation also introduces additional overhead on reading values and emitting effects.
     *     3) Flexibility - Currently, only {@link CellRefV2#autoEffects} are supported with dynamic allocations.
     *        This is sufficient for most use cases, but some specialized use cases may need more advanced effect handling.
     *        This may change in future implementations of this method.
     * </p>
     * <p>
     *     To limit the performance downsides, we recommend using the pre-allocations feature.
     *     This will construct some resources during initialization as regular {@link MutableResource}s and assigned
     *     when {@link MutableResourceFactory#create} is called.
     *     When these pre-allocations are exhausted, the factory will switch to dynamic allocations.
     *     An excessive number of pre-allocations also introduces some performance cost, especially on memory.
     * </p>
     * <p>
     *     Finally, note that while resources can be allocated dynamically with this method, registration is always static.
     *     Thus, dynamically-allocated resources can only be used for internal simulation state, not (directly) as outputs.
     * </p>
     */
    public static <D extends Dynamics<?, D>> MutableResourceFactory<D>
    dynamicResourceFactory(int preallocations, D initialDynamics) {
        return new MutableResourceFactory<>(preallocations, DynamicsMonad.pure(initialDynamics));
    }

    public static class MutableResourceFactory<D extends Dynamics<?, D>> {
        private final List<MutableResource<D>> staticAllocations;
        // Note: Putting this variable in a local, rather than a resource, means we leak information
        // across Aerie task boundaries.
        // This means that logically concurrent calls to "create" will nevertheless return different resources,
        // avoiding race conditions.
        // It also means that replaying a call to "create" will return a new resource, which could cause serious
        // and hard-to-debug errors downstream.
        private int usedStaticAllocations = 0;
        private final MutableResource<CompositeDynamics<UUID, D>> dynamicAllocations
                = resource(new CompositeDynamics<>(Map.of()));

        public MutableResourceFactory(int staticAllocations, ErrorCatching<Expiring<D>> initialDynamics) {
            this.staticAllocations = IntStream.range(0, staticAllocations)
                    .mapToObj(i -> resource(initialDynamics))
                    .toList();
        }

        // TODO - build a version of this that allows arbitrary effect traits as well.
        // This will likely require using a CellRef instead of a MutableResource as the base,
        // so we can use a specialized effect type which explicitly reports which key it's affecting...

        /**
         * See {@link MutableResourceViews.MutableResourceFactory#create(ErrorCatching)} for description and warnings.
         */
        public MutableResource<D> create(D initialDynamics) {
            return create(pure(initialDynamics));
        }

        /**
         * Dynamically create a {@link MutableResource}.
         * Note that this method should *not* be replayed with {@link ModelActions#replaying}.
         * Doing so will allocate a new resource on each replay, potentially causing hard-to-debug downstream errors.
         * <p>
         *     Replaying tasks are used frequently in the streamline framework for efficiency.
         *     If you're unsure whether a task might replay, you can add logging to the call site using System.out.println.
         *     If those log messages appear multiple times, that task is likely being replayed.
         * </p>
         * <p>
         *     One common and safe use case is to create resources from a reaction, like this:
         *     <pre>
         *     class Model {
         *         public List&lt;MutableResource&lt;Polynomial&gt;&gt; polys = new ArrayList&lt;&gt;();
         *
         *         public Model() {
         *             var factory = {@link MutableResourceViews#dynamicResourceFactory}(5, polynomial(0));
         *             Reactions.whenever(someInterestingCondition, () -> {
         *                 polys.add(factory.create(polynomial(0)));
         *             });
         *         }
         *     }
         *     </pre>
         *     Since the reaction doesn't yield, it doesn't replay, making this use pattern safe.
         * </p>
         * <p>
         *     If necessary, this method can be insulated from being replayed by wrapping it in its own task, like so:
         *     <pre>
         *     // Somewhere else:
         *     MutableObject&lt;MutableResource&lt;Discrete&lt;String&gt;&gt;&gt; myResource = new MutableObject&lt;&gt;();
         *
         *     // When you want to create the resource, but need to insulate it from a replaying task:
         *     call(replaying(() -> myResource.setValue(resourceFactory.create(discrete("starting value")))));
         *     </pre>
         *     When doing this, take care that the <code>myResource</code> variable is declared and initialized
         *     such that it will not be re-initialized by a task replay!
         *     This example creates a new replaying task to create the resource,
         *     but that new task never replays because it never needs to yield to the engine.
         *     Additionally, collections like a list or map could be used instead of a <code>MutableObject</code>.
         * </p>
         */
        public MutableResource<D> create(ErrorCatching<Expiring<D>> initialDynamics) {
            if (usedStaticAllocations < staticAllocations.size()) {
                // Return the next available pre-allocated resource.
                var result = staticAllocations.get(usedStaticAllocations++);
                result.emit(name(
                        $ -> initialDynamics,
                        "Initialize pre-allocated dynamic MutableResource to %s",
                        initialDynamics));
                return result;
            }

            // Generate a random key to index this state:
            var uuid = UUID.randomUUID();

            // Use an effect to create the new inner dynamics.
            dynamicAllocations.emit("Create dynamic MutableResource",
                    CompositeDynamics.effect(uuid, $ -> initialDynamics));

            // Finally, build a view that will pass effects down to the correct part of the dynamic allocations map,
            // and will read its own dynamics from that part alone.
            return new MutableResource<>() {
                @Override
                public void emit(DynamicsEffect<D> effect) {
                    dynamicAllocations.emit(CompositeDynamics.effect(uuid, effect));
                }

                @Override
                public ErrorCatching<Expiring<D>> getDynamics() {
                    // Since the top level cannot be in error, and we don't care about the top-level expiry,
                    // it's safe to just get the top level dynamics, and access the dynamics we care about from there.
                    return currentValue(dynamicAllocations).get(uuid);
                }
            };
        }

        // This probably could have been done with just an "unstructured", but using a new type
        // gives us a place to group the effect utility method and makes the code above more legible, I think.
        private record CompositeDynamics<K, D extends Dynamics<?, D>>(Map<K, ErrorCatching<Expiring<D>>> extract)
                implements Dynamics<Map<K, ErrorCatching<Expiring<D>>>, CompositeDynamics<K, D>> {
            @Override
            public CompositeDynamics<K, D> step(Duration t) {
                return new CompositeDynamics<>(extract.entrySet().stream().collect(toMap(
                        Map.Entry::getKey,
                        entry -> map(entry.getValue(), $ -> $.step(t)))));
            }

            public static <K, D extends Dynamics<?, D>>
            DynamicsEffect<CompositeDynamics<K, D>> effect(K key, DynamicsEffect<D> effect) {
                // Carefully destructure and re-structure the dynamics, so that:
                // 1) The top level dynamics are never in error
                // 2) The top level expiry is the minimum of constituents' expiries.
                return name(alloc -> alloc.map(alloc$ -> {
                    var alloc$$ = new HashMap<>(alloc$.data().extract());
                    alloc$$.compute(key, (k, v) -> effect.apply(v));
                    var expiry = alloc$$.values().stream()
                            .map($ -> $.match(Expiring::expiry, failure -> NEVER))
                            .reduce(Expiry::or)
                            .orElse(NEVER);
                    return new Expiring<>(new CompositeDynamics<>(alloc$$), expiry);
                }), "%s", effect);
            }
        }
    }
}
