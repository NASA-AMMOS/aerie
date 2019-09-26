package gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.utils.HttpRequester;

import javax.json.bind.JsonbBuilder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public final class RemoteAdaptationService implements AdaptationService {
  private final HttpRequester client;

  public RemoteAdaptationService(final URI serviceBaseUri) {
    this.client = new HttpRequester(HttpClient.newHttpClient(), serviceBaseUri);
  }

  @Override
  public Map<String, ActivityType> getActivityTypes(final String adaptationId) throws NoSuchAdaptationException {
    final HttpResponse<String> response;
    try {
      response = this.client.sendRequest("GET", "/adaptations/" + adaptationId + "/activities");
    } catch (final IOException | InterruptedException ex) {
      throw new AdaptationAccessException(ex);
    }

    switch (response.statusCode()) {
      case 200:
        final Type ACTIVITY_MAP_TYPE = new HashMap<String, ActivityType>(){}.getClass().getGenericSuperclass();
        return JsonbBuilder.create().fromJson(response.body(), ACTIVITY_MAP_TYPE);

      case 404:
        throw new NoSuchAdaptationException(adaptationId);

      default:
        throw new AdaptationAccessException("unexpected status code `" + response.statusCode() + "`");
    }
  }

  private static class AdaptationAccessException extends RuntimeException {
    public AdaptationAccessException(final Throwable cause) {
      super(cause);
    }

    public AdaptationAccessException(final String message) {
      super(message);
    }
  }
}
