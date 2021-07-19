package gov.nasa.jpl.aerie.json;

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


  public static <T1, T2>
  Pair<T1, T2>
  tuple(T1 t1, T2 t2) {
    return Pair.of(t1, t2);
  }

  public static <T1, T2>
  Pair<T1, T2>
  tuple2(T1 t1, T2 t2) {
    return tuple(t1, t2);
  }

  public static <T1, T2, T3>
  Pair<Pair<T1, T2>, T3>
  tuple3(T1 t1, T2 t2, T3 t3) {
    return tuple(tuple(t1, t2), t3);
  }

  public static <T1, T2, T3, T4>
  Pair<Pair<Pair<T1, T2>, T3>, T4>
  tuple4(T1 t1, T2 t2, T3 t3, T4 t4) {
    return tuple(tuple(tuple(t1, t2), t3), t4);
  }

  public static <T1, T2, T3, T4, T5>
  Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5>
  tuple5(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
    return tuple(tuple(tuple(tuple(t1, t2), t3), t4), t5);
  }
}
