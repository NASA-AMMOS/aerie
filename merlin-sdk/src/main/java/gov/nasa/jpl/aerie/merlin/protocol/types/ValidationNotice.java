package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.util.List;

public record ValidationNotice(List<String> subjects, String message) { }
