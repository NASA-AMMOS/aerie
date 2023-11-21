package gov.nasa.jpl.aerie.merlin.server.http;

import java.util.Optional;

/**
 * A class representing a fallible operation result.
 *
 * @param <T> The type of the value.
 */
public class Failable<T> {

  private final T value;

  private final String message;
  private final boolean isFailure;

  /**
   * Constructs a Fallible with a value and a flag indicating success or failure.
   *
   * @param value The result value.
   * @param message
   * @param isFailure Indicates whether the operation was a failure.
   */
  public Failable(T value, final String message, boolean isFailure) {
    this.value = value;
    this.message = message;
    this.isFailure = isFailure;
  }

  /**
   * Checks if the operation was a failure.
   *
   * @return true if the operation was a failure, otherwise false.
   */
  public boolean isFailure() {
    return isFailure;
  }

  /**
   * Gets the value
   *
   * @return The value or null;

   */
  public T getOrNull() {
    return value;
  }

  /**
   * Gets the value wrapped in an {@link Optional}.
   *
   * @return An {@link Optional} containing the value, or an empty {@link Optional} if the operation is a failure.
   */
  public Optional<T> getOptional() {
    return Optional.ofNullable(value);
  }

  /**
   * Gets the message
   *
   * @return The message;

   */
  public String getMessage() {
    return message;
  }

  /**
   * Constructs a Fallible with a successful value.
   *
   * @param value The successful value.
   * @param <T>   The type of the value.
   * @return A successful Fallible.
   */
  public static <T> Failable<T> of(T value) {
    return new Failable<>(value, "", false);
  }

  /**
   * Constructs a Fallible representing a failure.
   *
   * @param <T> The type of the value.
   * @return A failed Fallible.
   */
  public static <T> Failable<T> failure() {
    return new Failable<>(null, "", true);
  }

  /**
   * Constructs a Fallible representing a failure with a value.
   *
   * @param value The value indicating failure.
   * @param <T>   The type of the value.
   * @return A failed Fallible with a value.
   */
  public static <T> Failable<T> failure(T value) {
    return new Failable<>(value, "", true);
  }

  /**
   * Constructs a Fallible representing a failure with a value and a message.
   *
   * @param value   The value indicating failure.
   * @param message The message associated with the failure.
   * @param <T>     The type of the value.
   * @return A failed Fallible with a value and a message.
   */
  public static <T> Failable<T> failure(T value, String message) {
    return new Failable<>(value, message, true);
  }
}
