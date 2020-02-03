package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;

public class HttpUtilities {

    private static ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);

    public static HttpResponse createBasicHttpResponse(int responseStatus, String reason) {
        return new BasicHttpResponse(protocolVersion, responseStatus, reason);
    }

    public static HttpResponse createBasicHttpResponse(int responseStatus) {
        return createBasicHttpResponse(responseStatus, "");
    }
}
