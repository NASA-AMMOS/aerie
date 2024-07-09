package gov.nasa.jpl.aerie.command_model.sequencing;

import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;

import java.util.List;
import java.util.Map;

@AutoValueMapper.Record
public record Command(String stem, List<String> arguments) {}
