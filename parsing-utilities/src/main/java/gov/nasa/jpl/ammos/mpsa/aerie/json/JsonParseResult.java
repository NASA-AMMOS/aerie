package gov.nasa.jpl.ammos.mpsa.aerie.json;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface JsonParseResult<T> {
  <Value, Throws extends Throwable> Value match(Visitor<? super T, Value, Throws> visitor) throws Throws;

  interface Visitor<T, Result, Throws extends Throwable> {
    Result onSuccess(T result) throws Throws;
    Result onFailure(FailureReason failure) throws Throws;
  }

  static <T> JsonParseResult<T> success(final T value) {
    return new JsonParseResult<>() {
      @Override
      public <Result, Throws extends Throwable> Result match(final Visitor<? super T, Result, Throws> visitor) throws Throws {
        return visitor.onSuccess(value);
      }
    };
  }

  static <T> JsonParseResult<T> failure(final FailureReason reason) {
    return new JsonParseResult<>() {
      @Override
      public <Result, Throws extends Throwable> Result match(final Visitor<? super T, Result, Throws> visitor) throws Throws {
        return visitor.onFailure(reason);
      }

      @Override
      public FailureReason getFailureOrThrow() {
        return reason;
      }
    };
  }

  static <T> JsonParseResult<T> failure(final String reason) {
    return failure(new FailureReason(reason));
  }

  static <T> JsonParseResult<T> failure() {
    return failure(new FailureReason("Unknown reason"));
  }


  default <S> JsonParseResult<S> mapSuccess(final Function<T, S> transform) {
    Objects.requireNonNull(transform);

    return this.match(new Visitor<T, JsonParseResult<S>, RuntimeException>() {
      @Override
      public JsonParseResult<S> onSuccess(final T result) {
        return JsonParseResult.success(transform.apply(result));
      }

      @Override
      public JsonParseResult<S> onFailure(final FailureReason reason) {
        return JsonParseResult.failure(reason);
      }
    });
  }

  // TODO: Run both parsers and collect any failures together.
  default <S, Result> JsonParseResult<Result> parWith(final JsonParseResult<S> other, final BiFunction<T, S, Result> combine) {
    Objects.requireNonNull(other);
    Objects.requireNonNull(combine);

    return this.match(new Visitor<T, JsonParseResult<Result>, RuntimeException>() {
      @Override
      public JsonParseResult<Result> onFailure(final FailureReason failure) {
        return JsonParseResult.failure(failure);
      }

      @Override
      public JsonParseResult<Result> onSuccess(final T result1) {
        return other.match(new Visitor<S, JsonParseResult<Result>, RuntimeException>() {
          @Override
          public JsonParseResult<Result> onFailure(final FailureReason failure) {
            return JsonParseResult.failure(failure);
          }

          @Override
          public JsonParseResult<Result> onSuccess(final S result2) {
            return JsonParseResult.success(combine.apply(result1, result2));
          }
        });
      }
    });
  }

  default <S> JsonParseResult<Pair<T, S>> parWith(final JsonParseResult<S> other) {
    return parWith(other, Pair::of);
  }

  default <S> JsonParseResult<S> andThen(final Function<T, JsonParseResult<S>> step) {
    Objects.requireNonNull(step);

    return this.match(new Visitor<T, JsonParseResult<S>, RuntimeException>() {
      @Override
      public JsonParseResult<S> onSuccess(final T result) {
        return step.apply(result);
      }

      @Override
      public JsonParseResult<S> onFailure(final FailureReason failure) {
        return JsonParseResult.failure(failure);
      }
    });
  }

  default JsonParseResult<T> prependBreadcrumb(final Breadcrumb breadcrumb) {
    Objects.requireNonNull(breadcrumb);

    return this.match(new Visitor<T, JsonParseResult<T>, RuntimeException>() {
      @Override
      public JsonParseResult<T> onSuccess(final T result) {
        return JsonParseResult.success(result);
      }

      @Override
      public JsonParseResult<T> onFailure(final FailureReason failure) {
        return JsonParseResult.failure(failure.prependBreadcrumb(breadcrumb));
      }
    });
  }

  default boolean isFailure() {
    return this.match(new Visitor<T, Boolean, RuntimeException>() {
      @Override
      public Boolean onFailure(final FailureReason reason) {
        return true;
      }

      @Override
      public Boolean onSuccess(final T result) {
        return false;
      }
    });
  }

  default <Throws extends Throwable> T getSuccessOrThrow(final Function<FailureReason, ? extends Throws> throwsSupplier) throws Throws {
    return this.match(new Visitor<T, T, Throws>() {
      @Override
      public T onFailure(final FailureReason reason) throws Throws {
        throw throwsSupplier.apply(reason);
      }

      @Override
      public T onSuccess(final T result) {
        return result;
      }
    });
  }

  default T getSuccessOrThrow() {
    return this.getSuccessOrThrow(reason -> new RuntimeException("Called getSuccessOrThrow on a Failure case"));
  }

  default FailureReason getFailureOrThrow() {
    return this.match(new Visitor<T, FailureReason, RuntimeException>() {
      @Override
      public FailureReason onSuccess(final T result) {
        throw new RuntimeException("Called getFailureOrThrow on a Success case");
      }

      @Override
      public FailureReason onFailure(final FailureReason failure) {
        return failure;
      }
    });
  }

  class FailureReason {
    public final List<Breadcrumb> breadcrumbs;
    public final String reason;

    public FailureReason(final String reason) {
      this.reason = Objects.requireNonNull(reason);
      this.breadcrumbs = new ArrayList<>();
    }

    public FailureReason prependBreadcrumb(final Breadcrumb breadcrumb) {
      this.breadcrumbs.add(0, Objects.requireNonNull(breadcrumb));
      return this;
    }
  }
}
