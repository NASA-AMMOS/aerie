package gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime.message;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessage;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessageTypeEnum;

// No need to implement an abstract class since there is only one ConcreteCreator in this Factory
public class MessageHandlerCreator {
  public MessageStrategy getMessageStrategy(AmqpMessage message)
      throws UnsupportedOperationException {
    if (message.getMessageType().equals(AmqpMessageTypeEnum.LOAD_ADAPTATION)) {
      return new LoadAdaptationMessageStrategy();
    }
    throw new UnsupportedOperationException("UnhandledAmqpMessageType");
  }
}
