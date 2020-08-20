package gov.nasa.jpl.ammos.mpsa.aerie.json;

import java.util.ArrayList;
import java.util.List;
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
    public final List<String> breadcrumbs;
    public final String reason;

    public FailureReason(String reason) {
      this.reason = reason;
      this.breadcrumbs = new ArrayList<>();
    }

    public FailureReason prependBreadcrumb(String breadcrumb) {
      breadcrumbs.add(0, breadcrumb);
      return this;
    }
  }
}
