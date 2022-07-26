package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import java.util.Set;

import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;

/**
 * A {@link TimelineSegment} represents a portion of a timeline of a simulation
 * that may or may not still be in progress. This representation incrementalizes
 * simulation results, so that clients can directly query/subscribe to the database
 * to gain insight into currently running simulations.
 *
 * Instead of a monolothic {@link gov.nasa.jpl.aerie.constraints.model.SimulationResults} object
 * that holds all simulation results, and is only computed and the end of an entire
 * {@link gov.nasa.jpl.aerie.merlin.driver.SimulationDriver} run, TimelineSegment represents just a
 * portion of the simulation results, and it is up to the client to reconstruct these portions
 * into a meaningful timeline.
 *
 * @param simulationDatasetId Each TimelineSegment belongs to exactly one {@link SimulationDatasetRecord}
 * but a SimulationDatasetRecord will likely have many TimelineSegments
 * @param lifeCycleEvents This is the set of all {@link LifeCycleEvent}'s this segment includes. This is
 * how clients receive updates on when activity instances begin or end, for example.
 * @param profileSegments A {@link ProfileSet} representing new profile segments to be streamed out.
 * @param simulationTime Represents the new simulation time that the client's timeline should be stepped up to
 */
public final record TimelineSegment(
    long simulationDatasetId,
    Set<LifeCycleEvent> lifeCycleEvents,
    ProfileSet profileSegments,
    String simulationTime) {}
