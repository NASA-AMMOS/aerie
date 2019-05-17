package gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime.message;

import gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk.AdaptationUtils;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinSDKAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.AdaptationBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;

public class LoadAdaptationMessageStrategy implements MessageStrategy {

  private RestTemplate restTemplate;
  private static final Logger logger = LoggerFactory.getLogger(LoadAdaptationMessageStrategy.class);

  public LoadAdaptationMessageStrategy() {
    restTemplate = new RestTemplate();
  }

  public void execute(AmqpMessage message, HashMap<String, String> contextParams) {
    AmqpMessageData messageData = message.getData();
    String adaptationId = (String) messageData.getAdditionalProperties().get("adaptationId");
    if (!adaptationId.isEmpty() && contextParams.containsKey("adaptationUri")) {
      String uri = String.format("%s/%s", contextParams.get("adaptationUri"), adaptationId);

      ResponseEntity<Adaptation> response =
          restTemplate.exchange(
              uri, HttpMethod.GET, null, new ParameterizedTypeReference<Adaptation>() {});

      Adaptation adaptation = response.getBody();
      if (adaptation == null) {
        logger.error("Adaptation could not be parsed from request body " + uri);
        return;
      }

      try {
        MerlinSDKAdaptation userAdaptation =
            AdaptationUtils.loadAdaptation(adaptation.getLocation());
        if (userAdaptation == null) {
          logger.error("loadAdaptation returned a null value for adaptation " + adaptationId);
          return;
        }

        AdaptationBuilder builder = userAdaptation.init();
        if (builder == null) {
          logger.error("Adaptation could not be initialized (adaptation id " + adaptationId + ")");
          return;
        }

        // TODO: Wire up signal listener
        // TODO: Create dedicated topic for this adaptation
        // TODO: Prevent this runtime from managing other adaptations until this one is unloaded
        logger.info("Successfully loaded adaptation " + builder.getAdaptation().getName());

      } catch (Exception e) {
        logger.error("Error loading adaptation " + adaptationId, e);
      }
    }
  }
}
