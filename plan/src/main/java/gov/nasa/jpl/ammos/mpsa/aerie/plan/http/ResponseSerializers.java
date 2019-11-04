package gov.nasa.jpl.ammos.mpsa.aerie.plan.http;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.Breadcrumb;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.bind.JsonbException;
import javax.json.stream.JsonParsingException;
import java.util.List;

public final class ResponseSerializers {
  private static JsonValue serializeBreadcrumb(final Breadcrumb breadcrumb) {
    return breadcrumb.match(new Breadcrumb.Visitor<>() {
      @Override
      public JsonValue onListIndex(final int index) {
        return Json.createValue(index);
      }

      @Override
      public JsonValue onMapIndex(final String index) {
        return Json.createValue(index);
      }
    });
  }

  public static JsonValue serializeValidationMessage(final List<Breadcrumb> breadcrumbs, final String message) {
    final var breadcrumbsJson = Json.createArrayBuilder();
    for (final var breadcrumb : breadcrumbs) {
      breadcrumbsJson.add(serializeBreadcrumb(breadcrumb));
    }

    return Json.createObjectBuilder()
        .add("breadcrumbs", breadcrumbsJson)
        .add("message", message)
        .build();
  }

  public static JsonValue serializeValidationMessages(final List<Pair<List<Breadcrumb>, String>> messages) {
    final var messageJson = Json.createArrayBuilder();
    for (final var entry : messages) {
      messageJson.add(serializeValidationMessage(entry.getKey(), entry.getValue()));
    }

    return messageJson.build();
  }

  public static JsonValue serializeValidationException(final ValidationException ex) {
    return serializeValidationMessages(ex.getValidationErrors());
  }

  public static JsonValue serializeJsonParsingException(final JsonParsingException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "invalid json")
        .build();
  }

  public static JsonValue serializeInvalidEntityException(final InvalidEntityException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "invalid json")
        .build();
  }

  public static JsonValue serializeNoSuchPlanException(final NoSuchPlanException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "no such plan")
        .build();
  }

  public static JsonValue serializeNoSuchActivityInstanceException(final NoSuchActivityInstanceException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "no such activity instance")
        .build();
  }

  public static JsonValue serializeJsonbException(final JsonbException ex) {
    return Json.createObjectBuilder()
        .add("message", "invalid json")
        .build();
  }
}
