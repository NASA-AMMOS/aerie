package gov.nasa.jpl.aerie.command_model.sequencing;

import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;

import java.util.List;

@AutoValueMapper.Record
public record Sequence(String id, List<Command> commands) {
}
