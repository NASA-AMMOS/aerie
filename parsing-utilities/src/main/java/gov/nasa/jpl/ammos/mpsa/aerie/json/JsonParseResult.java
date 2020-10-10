package gov.nasa.jpl.ammos.mpsa.aerie.json;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface JsonParseResult<T> {
  <Value> Value match(Visitor<? super T, Value> visitor);

  interface Visitor<T, Result> {
    Result onSuccess(T result);
    Result onFailure(FailureReason failure);
  }

  static <T> JsonParseResult<T> success(final T value) {
    return new JsonParseResult<>() {
      @Override
      public <Result> Result match(final Visitor<? super T, Result> visitor) {
        return visitor.onSuccess(value);
      }
    };
  }

  static <T> JsonParseResult<T> failure(final FailureReason reason) {
    return new JsonParseResult<>() {
      @Override
      public <Result> Result match(final Visitor<? super T, Result> visitor) {
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

    return this.match(new Visitor<>() {
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

    return this.match(new Visitor<>() {
      @Override
      public JsonParseResult<Result> onFailure(final FailureReason failure) {
        return JsonParseResult.failure(failure);
      }

      @Override
      public JsonParseResult<Result> onSuccess(final T result1) {
        return other.match(new Visitor<>() {
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

    return this.match(new Visitor<>() {
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

    return this.match(new Visitor<>() {
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
    return this.match(new Visitor<>() {
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

  default Optional<T> asSuccess() {
    return this.match(new Visitor<>() {
      @Override
      public Optional<T> onSuccess(final T result) {
        return Optional.of(result);
      }

      @Override
      public Optional<T> onFailure(final FailureReason failure) {
        return Optional.empty();
      }
    });
  }

  default Optional<FailureReason> asFailure() {
    return this.match(new Visitor<>() {
      @Override
      public Optional<FailureReason> onSuccess(final T result) {
        return Optional.empty();
      }

      @Override
      public Optional<FailureReason> onFailure(final FailureReason failure) {
        return Optional.of(failure);
      }
    });
  }

  default <Throws extends Throwable>
  T getSuccessOrThrow(final Function<FailureReason, ? extends Throws> throwsSupplier) throws Throws {
    return this
        .asSuccess()
        .orElseThrow(() -> throwsSupplier.apply(this.asFailure().orElseThrow()));
  }

  default T getSuccessOrThrow() {
    return this.getSuccessOrThrow(reason -> new RuntimeException("Called getSuccessOrThrow on a Failure case"));
  }

  default FailureReason getFailureOrThrow() {
    return this.match(new Visitor<>() {
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
