package gov.nasa.jpl.aerie.scheduler.server.http;

import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleAction;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleResults;

import javax.json.Json;
import javax.json.JsonValue;

/**
 * json serialization methods for data entities used in the scheduler response bodies
 */
public class ResponseSerializers {

  /**
   * serialize the scheduler run result, including if it is incomplete/failed
   *
   * @param response the result of the scheduling run to serialize
   * @return a json serialization of the scheduling run result
   */
  public static JsonValue serializeScheduleResultsResponse(final ScheduleAction.Response response) {
    if (response instanceof ScheduleAction.Response.Incomplete) {
      return Json
          .createObjectBuilder()
          .add("status", "incomplete")
          .build();
    } else if (response instanceof ScheduleAction.Response.Failed r) {
      return Json
          .createObjectBuilder()
          .add("status", "failed")
          .add("reason", r.reason())
          .build();
    } else if (response instanceof ScheduleAction.Response.Complete r) {
      return Json
          .createObjectBuilder()
          .add("status", "complete")
          .add("results", serializeScheduleResults(r.results()))
          .build();
    } else {
      throw new UnexpectedSubtypeError(ScheduleAction.Response.class, response);
    }
  }

  /**
   * serialize the provided scheduling result summary to json
   *
   * @param results the scheduling results to serialize
   * @return a json serialization of the given scheduling result
   */
  public static JsonValue serializeScheduleResults(final ScheduleResults results)
  {
    return Json
        .createObjectBuilder()
        .add("activityCount", results.activityCount())
        .add("goalScores", gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers.serializeMap(
            Json::createValue, results.goalScores()))
        .build();
  }

  /**
   * create report of given exception that can be passed as json payload
   *
   * @param e the exception to generate json report for
   * @return a json serialization of the exception details
   */
  public static JsonValue serializeException(final Exception e) {
    //TODO: stack trace or other details back to ui / client?
    return Json.createObjectBuilder()
               .add("message", e.toString())
               .build();
  }

}
