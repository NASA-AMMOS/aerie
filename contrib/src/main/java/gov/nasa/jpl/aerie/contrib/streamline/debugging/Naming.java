package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import org.apache.commons.lang3.mutable.MutableObject;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Allows anything that uses reference equality to be given a name.
 *
 * <p>
 *   By handling naming in a static auxiliary data structure, we achieve several goals:
 *   1) Naming doesn't bloat interfaces like Resource and DynamicsEffect.
 *   2) Names can be applied to classes and interfaces after-the-fact,
 *      including applying names to classes and interfaces that can't be modified, like library code.
 *   3) Naming is nevertheless globally available.
 *      (Unlike passing the name in a parallel parameter, for example.)
 * </p>
 */
public final class Naming {
  private Naming() {}

  // Use a WeakHashMap so that naming a thing doesn't prevent it from being garbage-collected.
  private static final WeakHashMap<Object, Supplier<Optional<String>>> NAMES = new WeakHashMap<>();
  // Way to inject a temporary "anonymous" name, so derived names still work even when not all args are named.
  private static final MutableObject<Optional<String>> anonymousName = new MutableObject<>(Optional.empty());

  /**
   * Register a name for thing, as a function of args' names.
   * If any of the args are anonymous, so is this thing.
   */
  public static void name(Object thing, String nameFormat, Object... args) {
    // Only capture weak references to arguments, so we don't leak memory
    var args$ = Arrays.stream(args).map(WeakReference::new).toArray(WeakReference[]::new);
    NAMES.put(thing, () -> {
      Object[] argNames = new Object[args$.length];
      for (int i = 0; i < args$.length; ++i) {
        // Try to resolve the argument name by first looking up and using its registered name,
        // or by falling back to the anonymous name.
        var argName$ = Optional.ofNullable(args$[i].get())
                .flatMap(Naming::getName)
                .or(anonymousName::getValue);
        if (argName$.isEmpty()) return Optional.empty();
        argNames[i] = argName$.get();
      }
      return Optional.of(nameFormat.formatted(argNames));
    });
  }

  /**
   * Get the name for thing.
   * If thing has no registered name and no synonyms,
   * returns empty.
   */
  public static Optional<String> getName(Object thing) {
    return Optional.ofNullable(NAMES.get(thing)).flatMap(Supplier::get).or(anonymousName::getValue);
  }

  /**
   * Get the name for thing.
   * Use anonymousName for anything without a name instead of returning empty.
   */
  public static String getName(Object thing, String anonymousName) {
    Naming.anonymousName.setValue(Optional.of(anonymousName));
    var result = getName(thing);
    Naming.anonymousName.setValue(Optional.empty());
    // This will never throw, because anonymous name will guarantee that some name is found.
    return result.orElseThrow();
  }

  public static String argsFormat(Collection<?> collection) {
    return "(" + IntStream.range(0, collection.size()).mapToObj($ -> "%s").collect(Collectors.joining(", ")) + ")";
  }
}
