package gov.nasa.jpl.aerie.e2e.utils;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class GatewayRequests implements AutoCloseable {
  private final APIRequestContext request;

  public GatewayRequests(Playwright playwright) {
    request = playwright.request().newContext(
            new APIRequest.NewContextOptions()
                    .setBaseURL(BaseURL.GATEWAY.url));
  }
  @Override
  public void close() {
    request.dispose();
  }

  /**
   * Uploads the Banananation JAR
   */
  public int uploadJarFile() throws IOException {
    return uploadJarFile("../examples/banananation/build/libs/banananation.jar");
  }

  /**
   * Uploads the JAR found at searchPath
   * @param jarPath is relative to the e2e-tests directory.
   */
  public int uploadJarFile(String jarPath) throws IOException {
    // Build File Payload
    final Path absolutePath = Path.of(jarPath).toAbsolutePath();
    final byte[] buffer = Files.readAllBytes(absolutePath);
    final FilePayload payload = new FilePayload(
        absolutePath.getFileName().toString(),
        "application/java-archive",
        buffer);

    final var response = request.post("/file", RequestOptions.create().setMultipart(FormData.create().set("file", payload)));

    // Process Response
    if(!response.ok()){
      throw new IOException(response.statusText());
    }
    try(final var reader = Json.createReader(new StringReader(response.text()))){
      final JsonObject bodyJson = reader.readObject();
      if(bodyJson.containsKey("errors")){
        System.err.println("Errors in response: \n" + bodyJson.get("errors"));
        throw new RuntimeException(bodyJson.toString());
      }
      return bodyJson.getInt("id");
    }
  }

}
