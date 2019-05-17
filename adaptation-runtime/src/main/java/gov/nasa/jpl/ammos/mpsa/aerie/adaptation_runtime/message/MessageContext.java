package gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime.message;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessage;

import java.util.HashMap;

public class MessageContext {
  private MessageStrategy messageStrategy;
  private HashMap<String, String> contextParams = new HashMap<>();

  public void setMessageStrategy(MessageStrategy messageStrategy) {
    this.messageStrategy = messageStrategy;
  }

  public void handleMessage(AmqpMessage message) {
    this.messageStrategy.execute(message, contextParams);
  }

  public void addContextParam(String key, String value) {
    this.contextParams.put(key, value);
  }
}
