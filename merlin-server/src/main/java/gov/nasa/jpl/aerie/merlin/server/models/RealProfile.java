package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public final record RealProfile(List<Pair<Duration, RealDynamics>> segments) {}
