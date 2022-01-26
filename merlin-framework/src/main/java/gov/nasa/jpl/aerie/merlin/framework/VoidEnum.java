package gov.nasa.jpl.aerie.merlin.framework;

/**
 * The VoidEnum represents a "Unit" type, which is a type that allows only one value (and thus can hold no information)
 * It is useful as a return type for void functions. It is in some cases preferable to Java's
 * Void type (capital V) because its single value is not null, but rather VoidEnum.VOID.
 *
 * https://en.wikipedia.org/wiki/Unit_type
 */
public enum VoidEnum {
  VOID
}
