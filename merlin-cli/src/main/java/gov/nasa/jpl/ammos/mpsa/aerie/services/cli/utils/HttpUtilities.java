package gov.nasa.jpl.ammos.mpsa.aerie.services.cli.utils;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;

import java.io.IOException;

public class HttpUtilities {

    private static ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);

    public static HttpResponse createBasicHttpResponse(int responseStatus, String reason) {
        return new BasicHttpResponse(protocolVersion, responseStatus, reason);
    }

    public static HttpResponse createBasicHttpResponse(int responseStatus) {
        return createBasicHttpResponse(responseStatus, "");
    }

    /**
     * Returns the error message contained in the body of response.
     * If no message is contained within the response, defaultMessage is returned
     *
     * @param response The HttpResponse from a request made to an Aerie service
     * @param defaultMessage An acceptable error message to use if no message is present in the response
     * @return The appropriate error message to provide to a user after a failed command
     */
    public static String getErrorMessage(HttpResponse response, String defaultMessage) {
        final var entity = response.getEntity();
        if (entity == null) return defaultMessage;

        try {
            return JsonUtilities.getErrorMessageFromFailureResponse(entity.getContent());
        } catch (IOException | JsonUtilities.ResponseWithoutErrorMessageException e) {
            return defaultMessage;
        }
    }
}
