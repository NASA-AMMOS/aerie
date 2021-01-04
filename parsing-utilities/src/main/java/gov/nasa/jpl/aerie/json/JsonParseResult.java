package gov.nasa.jpl.aerie.json;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface JsonParseResult<T> {
  <Value, Throws extends Throwable> Value match(Visitor<? super T, Value, Throws> visitor) throws Throws;

  interface Visitor<T, Result, Throws extends Throwable> {
    Result onSuccess(T result) throws Throws;
    Result onFailure() throws Throws;
  }

  static <T> JsonParseResult<T> success(final T value) {
    return new JsonParseResult<>() {
      @Override
      public <Result, Throws extends Throwable> Result match(final Visitor<? super T, Result, Throws> visitor) throws Throws {
        return visitor.onSuccess(value);
      }
    };
  }

  static <T> JsonParseResult<T> failure(String reason) {
    return new JsonParseResult<>() {
      @Override
      public <Result, Throws extends Throwable> Result match(final Visitor<? super T, Result, Throws> visitor) throws Throws {
        return visitor.onFailure();
      }

      @Override
      public FailureReason failureReason() {
        return new FailureReason(reason);
      }
    };
  }

  static <T> JsonParseResult<T> failure(FailureReason reason) {
    return new JsonParseResult<>() {
      @Override
      public <Result, Throws extends Throwable> Result match(final Visitor<? super T, Result, Throws> visitor) throws Throws {
        return visitor.onFailure();
      }

      @Override
      public FailureReason failureReason() {
        return reason;
      }
    };
  }

  static <T> JsonParseResult<T> failure() {
    return new JsonParseResult<>() {
      @Override
      public <Result, Throws extends Throwable> Result match(final Visitor<? super T, Result, Throws> visitor) throws Throws {
        return visitor.onFailure();
      }

      @Override
      public FailureReason failureReason() {
        return new FailureReason("Unknown reason");
      }
    };
  }

  /** Combine two results together. If either is a failure, returns a failure. */
  default <S, Result> JsonParseResult<Result> parWith(final JsonParseResult<S> other, final BiFunction<T, S, Result> step) {
    final var self = this;

    return this.match(new Visitor<T, JsonParseResult<Result>, RuntimeException>() {
      @Override
      public JsonParseResult<Result> onFailure() {
        return JsonParseResult.failure(self.failureReason());
      }

      @Override
      public JsonParseResult<Result> onSuccess(final T result1) {
        return other.match(new Visitor<S, JsonParseResult<Result>, RuntimeException>() {
          @Override
          public JsonParseResult<Result> onFailure() {
            return JsonParseResult.failure(other.failureReason());
          }

          @Override
          public JsonParseResult<Result> onSuccess(final S result2) {
            return JsonParseResult.success(step.apply(result1, result2));
          }
        });
      }
    });
  }

  default <S> JsonParseResult<Pair<T, S>> parWith(final JsonParseResult<S> other) {
    return this.parWith(other, Pair::of);
  }

  /** Prepends the given breadcrumb if the result is a failure. */
  default JsonParseResult<T> prependBreadcrumb(final Breadcrumb breadcrumb) {
    final var self = this;

    return this.match(new Visitor<T, JsonParseResult<T>, RuntimeException>() {
      @Override
      public JsonParseResult<T> onSuccess(final T _result) {
        return self;
      }

      @Override
      public JsonParseResult<T> onFailure() {
        return JsonParseResult.failure(self.failureReason().prependBreadcrumb(breadcrumb));
      }
    });
  }

  default <S> JsonParseResult<S> mapSuccess(final Function<T, S> transform) {
    return this.match(new Visitor<T, JsonParseResult<S>, RuntimeException>() {
      @Override
      public JsonParseResult<S> onSuccess(final T result) {
        return JsonParseResult.success(transform.apply(result));
      }

      @Override
      public JsonParseResult<S> onFailure() {
        return JsonParseResult.failure(failureReason());
      }
    });
  }

  default boolean isFailure() {
    return this.match(new Visitor<T, Boolean, RuntimeException>() {
      @Override
      public Boolean onFailure() {
        return true;
      }

      @Override
      public Boolean onSuccess(final T result) {
        return false;
      }
    });
  }

  default <Throws extends Throwable> T getSuccessOrThrow(final Supplier<? extends Throws> throwsSupplier) throws Throws {
    return this.match(new Visitor<T, T, Throws>() {
      @Override
      public T onFailure() throws Throws {
        throw throwsSupplier.get();
      }

      @Override
      public T onSuccess(final T result) {
        return result;
      }
    });
  }

  default T getSuccessOrThrow() {
    return this.getSuccessOrThrow(() -> new RuntimeException("Called getSuccessOrThrow on a Failure case"));
  }

  default FailureReason failureReason() {
    return new FailureReason("invalid json");
  }

  class FailureReason {
    public final List<Breadcrumb> breadcrumbs;
    public final String reason;

    public FailureReason(String reason) {
      this.reason = reason;
      this.breadcrumbs = new ArrayList<>();
    }

    public FailureReason prependBreadcrumb(Breadcrumb breadcrumb) {
      breadcrumbs.add(0, breadcrumb);
      return this;
    }
  }
}
