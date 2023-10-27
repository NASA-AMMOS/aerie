package gov.nasa.jpl.aerie.merlin.worker.postgres;

import gov.nasa.jpl.aerie.json.JsonParser;

import static gov.nasa.jpl.aerie.json.BasicParsers.anyP;
import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.longP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;

public final class PostgresNotificationJsonParsers {

  public static final JsonParser<PostgresSimulationNotificationPayload> postgresSimulationNotificationP
      = productP
      . field("model_revision", longP)
      . field("plan_revision", longP)
      . field("simulation_revision", longP)
      . optionalField("simulation_template_revision", longP)
      . field("plan_id", longP)
      . field("dataset_id", longP)
      . field("simulation_id", longP)
      . map(
          untuple(PostgresSimulationNotificationPayload::new),
          $ -> tuple($.modelRevision(),
                     $.planRevision(),
                     $.simulationRevision(),
                     $.simulationTemplateRevision(),
                     $.planId(),
                     $.datasetId(),
                     $.simulationId()));

  public static final JsonParser<PostgresValidationNotificationPayload> postgresValidationNotificationP
      = productP
      . field("activity_directive_id", intP)
      . field("revision", intP)
      . field("plan_id", intP)
      . field("model_id", intP)
      . field("type", stringP)
      . field("arguments", mapP(serializedValueP))
      . map(
          untuple(PostgresValidationNotificationPayload::new),
          $ -> tuple($.activityDirectiveId(), $.revision(), $.planId(), $.modelId(), $.typeName(), $.arguments()));
}
