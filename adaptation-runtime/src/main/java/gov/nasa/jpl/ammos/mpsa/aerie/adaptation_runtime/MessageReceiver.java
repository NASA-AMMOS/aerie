package gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime.message.MessageContext;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime.message.MessageHandlerCreator;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation_runtime.message.MessageStrategy;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.AmqpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MessageReceiver {

    @Value("${adaptation-url}")
    private String adaptationUri;

    private ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

    @Autowired
    public MessageReceiver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void receiveMessage(String json) {
        logger.info("Message received: " + json);
        try {
            AmqpMessage message = this.objectMapper.readValue(json, AmqpMessage.class);
            logger.info("Message " + message.getMessageType());

            // Factory which decides what strategy will be used to handle the message
            MessageHandlerCreator messageHandlerCreator = new MessageHandlerCreator();
            MessageStrategy messageStrategy = messageHandlerCreator.getMessageStrategy(message);

            // Strategy to execute the specified handler
            MessageContext messageContext = new MessageContext();
            messageContext.addContextParam("adaptationUri", adaptationUri);
            messageContext.setMessageStrategy(messageStrategy);
            // An unhandled exception will cause RabbitMQ to rebroadcast the message infinitely
            messageContext.handleMessage(message);

        } catch (IOException e) {
            logger.error("Exception caught", e);
        } catch (UnsupportedOperationException e) {
            logger.error("Unsupported message type", e);
        }
    }

}
