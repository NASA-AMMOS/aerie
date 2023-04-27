package gov.nasa.jpl.aerie.scheduler.worker;

import gov.nasa.jpl.aerie.scheduler.server.config.PlanOutputMode;
import gov.nasa.jpl.aerie.scheduler.server.config.Store;
import java.net.URI;
import java.nio.file.Path;

public record WorkerAppConfiguration(
    Store store,
    URI merlinGraphqlURI,
    Path merlinFileStore,
    Path missionRuleJarPath,
    PlanOutputMode outputMode) {}
