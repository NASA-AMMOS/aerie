package gov.nasa.jpl.ammos.mpsa.aerie.json;

import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Function;

public abstract class Uncurry {
  private Uncurry() {}

  public static <Result, T1, T2>
  Function<Pair<T1, T2>, Result>
  uncurry(Function<T1, Function<T2, Result>> f) {
    return p -> f.apply(p.getLeft()).apply(p.getRight());
  }


  public static <Result, T1, T2>
  Function<Pair<T1, T2>, Result>
  uncurry2(Function<T1, Function<T2, Result>> f) {
    return uncurry(f);
  }

  public static <Result, T1, T2, T3>
  Function<Pair<Pair<T1, T2>, T3>, Result>
  uncurry3(Function<T1, Function<T2, Function<T3, Result>>> f) {
    return uncurry(uncurry(f));
  }

  public static <Result, T1, T2, T3, T4>
  Function<Pair<Pair<Pair<T1, T2>, T3>, T4>, Result>
  uncurry4(Function<T1, Function<T2, Function<T3, Function<T4, Result>>>> f) {
    return uncurry(uncurry(uncurry(f)));
  }

  public static <Result, T1, T2, T3, T4, T5>
  Function<Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5>, Result>
  uncurry5(Function<T1, Function<T2, Function<T3, Function<T4, Function<T5, Result>>>>> f) {
    return uncurry(uncurry(uncurry(uncurry(f))));
  }

  public static <Result, T1, T2, T3, T4, T5, T6>
  Function<Pair<Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5>, T6>, Result>
  uncurry6(Function<T1, Function<T2, Function<T3, Function<T4, Function<T5, Function<T6, Result>>>>>> f) {
    return uncurry(uncurry(uncurry(uncurry(uncurry(f)))));
  }

  public static <Result, T1, T2, T3, T4, T5, T6, T7>
  Function<Pair<Pair<Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5>, T6>, T7>, Result>
  uncurry7(Function<T1, Function<T2, Function<T3, Function<T4, Function<T5, Function<T6, Function<T7, Result>>>>>>> f) {
    return uncurry(uncurry(uncurry(uncurry(uncurry(uncurry(f))))));
  }

  public static <Result, T1, T2, T3, T4, T5, T6, T7, T8>
  Function<Pair<Pair<Pair<Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5>, T6>, T7>, T8>, Result>
  uncurry8(Function<T1, Function<T2, Function<T3, Function<T4, Function<T5, Function<T6, Function<T7, Function<T8, Result>>>>>>>> f) {
    return uncurry(uncurry(uncurry(uncurry(uncurry(uncurry(uncurry(f)))))));
  }
}
