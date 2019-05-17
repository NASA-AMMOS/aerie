package gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime.message;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessage;

import java.util.HashMap;

// TODO: use an abstract class and implement a method to publish messages
public interface MessageStrategy {
    void execute(AmqpMessage message, HashMap<String, String> contextParams);
}
