/**
 * Definitions for simulating and solving over resources which do not vary continuously.
 *
 * <p>
 *   In general, a dynamics gives an embedding of an interval of time into a space of values.
 *   We expect these embeddings to be <i>continuous</i>: a closed set of values
 *   should be mapped onto by a closed interval of time.
 * </p>
 *
 * <p>
 *   We also want a class of continuous maps from our space of values into the Sierpinski space of boolean valuations.
 *   We call these maps <i>conditions</i>, and each is uniquely given by a choice of closed set in the space of values.
 *   Due to computability and representability concerns, we restrict ourselves further to <i>compact</i> sets.
 * </p>
 *
 * <p>
 *   For an arbitrary type {@code T}, we want our conditions to be finite sets.
 *   The appropriate space for such a type is the <i>discrete</i> space,
 *     where all sets are closed (and all finite sets are compact).
 *   The only continuous embeddings of a time interval are the constant functions.
 *     In general, the only continuous maps from <i>any</i> space to a discrete space
 *     are the locally constant functions. Since time is a connected space,
 *     this forces global constancy.
 * </p>
 *
 * <p>
 *   Seen differently, a discrete space is a 0-dimensional Euclidean manifold,
 *   i.e. a space with no degrees of freedom around each point.
 * </p>
 */
package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete;
