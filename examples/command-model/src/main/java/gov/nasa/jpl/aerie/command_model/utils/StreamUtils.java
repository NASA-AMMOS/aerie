package gov.nasa.jpl.aerie.command_model.utils;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class StreamUtils {
    private StreamUtils() {}

    public static <T> Stream<Pair<Integer, T>> enumerate(Stream<T> stream) {
        // Horrible hack. If you're using this model for real, just use Guava's Stream zip method.
        MutableInt i = new MutableInt(0);
        List<Pair<Integer, T>> results = new ArrayList<>();
        stream.forEachOrdered(t -> results.add(Pair.of(i.getAndIncrement(), t)));
        return results.stream();
    }
}
