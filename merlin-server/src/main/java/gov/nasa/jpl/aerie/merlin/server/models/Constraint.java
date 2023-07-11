package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.constraints.model.ConstraintType;

public record Constraint (Long id, String name, String description, String definition, ConstraintType type) {
}
