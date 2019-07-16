package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ParameterType;

import java.util.List;

/**
 * A mission-specific representation of an activity parameter.
 *
 * Mission-specific activity parameters should implement this interface, as well as the
 * {@link ParameterType} protocol. Implementations of this interface provide methods used
 * by the Merlin system to interact with activity parameter instances.
 */
public interface ActivityParameter {
}
